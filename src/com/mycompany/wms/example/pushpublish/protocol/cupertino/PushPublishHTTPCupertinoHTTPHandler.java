/*
 * This code and all components (c) Copyright 2018, Wowza Media Systems, LLC. All rights reserved.
 * This code is licensed pursuant to the BSD 3-Clause License.
 */
package com.mycompany.wms.example.pushpublish.protocol.cupertino;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import com.wowza.util.IPacketFragment;
import com.wowza.util.PacketFragmentList;
import com.wowza.util.StringUtils;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.manifest.model.m3u8.MediaSegmentModel;
import com.wowza.wms.manifest.model.m3u8.PlaylistModel;
import com.wowza.wms.manifest.writer.m3u8.PlaylistWriter;
import com.wowza.wms.pushpublish.manager.IPushPublisher;
import com.wowza.wms.pushpublish.protocol.cupertino.PushPublishHTTPCupertino;
import com.wowza.wms.server.LicensingException;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.util.PushPublishUtils;

public class PushPublishHTTPCupertinoHTTPHandler extends PushPublishHTTPCupertino
{

	/*
	 * Directory layout is as follow:
	 * <basePath> is the http.path directory in the map file
	 * <dstStreamName> is gotten from the base implementation
	 * <sessionId> is gotten from the base implementation, this is here in case the incoming stream comes and goes, we don't overwrite media segments
	 *
	 *
	 * /<basePath>/<dstStreamName>/playlist.m3u8 (master playlist)
	 * /<basePath>/<dstStreamName>/chunklist.m3u8 (media playlist)
	 * /<basePath>/<groupName>/playlist.m3u8 (group master playlist)
	 * /<basePath>/<dstStreamName>/<sessionId>/media_x.ts (media segments)
	 *
	 * We reference the media playlists and the media segements using a preceeding "../" such that when the group master playlist references the different
	 * media playlists that represent renditions, it can pick them up from different implementations without modification.
	 *
	 * If this is sending to a backup server, the <dstStreamName> has "-b" appended to the end
	 *
	 */

	private static final int DEFAULT_HTTP_PORT = 80;
	private static final int DEFAULT_HTTPS_PORT = 443;

	String basePath = "/";
	String httpHost = "example.com";

	boolean isSendSSL = false;

	boolean backup = false;
	String groupName = null;
	private int connectionTimeout = 5000;
	private int readTimeout = 5000;

	public PushPublishHTTPCupertinoHTTPHandler() throws LicensingException
	{
		super();
	}

	@Override
	public void init(IApplicationInstance appInstance, String streamName, IMediaStream stream, Map<String, String> profileData, Map<String, String> maps, IPushPublisher pushPublisher, boolean streamDebug)
	{
		String localEntryName = PushPublishUtils.getMapString(maps, "entryName");

		// playlistCrossName must be unique to the application Instance.
		this.playlistCrossName = "pushpublish-cupertino-http-playlists-" + appInstance.getContextStr() + "-" + streamName + "-" + localEntryName;

		// Call super.init() to initialize this profile and trigger call to our load() method
		super.init(appInstance, streamName, stream, profileData, maps, pushPublisher, streamDebug);
	}

	@Override
	public void load(HashMap<String, String> dataMap)
	{
		System.out.println("load: " + dataMap);
		super.load(dataMap);

		httpHost = hostname;
		String httpHostStr = PushPublishUtils.removeMapString(dataMap, "http.host");
		if (!StringUtils.isEmpty(httpHostStr))
			httpHost = httpHostStr;

		String basePathStr = PushPublishUtils.removeMapString(dataMap, "http.path");
		if (!StringUtils.isEmpty(basePathStr))
			basePath = basePathStr;
		if (!basePath.endsWith("/"))
			basePath += "/";

		String sendSSLStr = PushPublishUtils.removeMapString(dataMap, "sendSSL");
		if (sendSSLStr != null)
		{
			sendSSLStr = sendSSLStr.toLowerCase(Locale.ENGLISH);
			isSendSSL = sendSSLStr.startsWith("t") || sendSSLStr.startsWith("y");
		}

		// set default http(s) port if it hasn't been changed from the default rtmp port.
		if (port == 1935)
		{
			port = isSendSSL ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT;
		}

	}

	@Override
	public boolean updateGroupMasterPlaylistPlaybackURI(String groupName, PlaylistModel masterPlaylist)
	{
		boolean retVal = true;
		String newPath = "../" + groupName + "/" + masterPlaylist.getUri().getPath();
		try
		{
			masterPlaylist.setUri(new URI(newPath));
			this.groupName = groupName;
		}
		catch (Exception e)
		{
			logError("updateGroupMasterPlaylistPlaybackURI", "Invalid path " + newPath, e);
			retVal = false;
		}
		return retVal;
	}

	@Override
	public boolean updateMasterPlaylistPlaybackURI(PlaylistModel playlist)
	{
		boolean retVal = true;

		String path = "../" + getDstStreamName() + (backup ? "-b/" : "/") + playlist.getUri().toString();
		try
		{
			playlist.setUri(new URI(path));
		}
		catch (URISyntaxException e)
		{
			logError("updateMasterPlaylistPlaybackURI", "Failed to update master playlist to " + path);
			retVal = false;
		}
		return retVal;
	}

	@Override
	public boolean updateMediaPlaylistPlaybackURI(PlaylistModel playlist)
	{
		boolean retVal = true;

		String path = "../" + getDstStreamName() + (backup ? "-b/" : "/") + playlist.getUri().toString();
		try
		{
			playlist.setUri(new URI(path));
		}
		catch (URISyntaxException e)
		{
			logError("updateMediaPlaylistPlaybackURI", "Failed to update media playlist to " + path);
			retVal = false;
		}
		return retVal;
	}

	@Override
	public boolean updateMediaSegmentPlaybackURI(MediaSegmentModel mediaSegment)
	{
		boolean retVal = true;
		String newPath = mediaSegment.getUri().getPath();

		// to prevent overriding prior segments if the stream were to reset,
		// we'll use the sessionStr to create a sub directory to keep the
		// media segments in.

		try
		{
			String temp = getRandomSessionStr() + "/" + newPath;
			mediaSegment.setUri(new URI(temp));
		}
		catch (Exception e)
		{
			retVal = false;
			logError("updateMediaSegmentPlaybackURI", "Invalid path " + newPath, e);
		}
		return retVal;
	}

	@Override
	public int sendGroupMasterPlaylist(String groupName, PlaylistModel playlist)
	{
		int retVal = 0;
		String playlistPath = playlist.getUri().getPath().replaceFirst("../", basePath);

		retVal = writePlaylist(playlist, playlistPath);
		return retVal;
	}

	@Override
	public int sendMasterPlaylist(PlaylistModel playlist)
	{
		int retVal = 0;
		String playlistPath = playlist.getUri().getPath().replaceFirst("../", basePath);

		retVal = writePlaylist(playlist, playlistPath);
		return retVal;
	}

	@Override
	public int sendMediaPlaylist(PlaylistModel playlist)
	{
		int retVal = 0;
		String playlistPath = playlist.getUri().getPath().replaceFirst("../", basePath);

		retVal = writePlaylist(playlist, playlistPath);
		return retVal;
	}

	@Override
	public int sendMediaSegment(MediaSegmentModel mediaSegment)
	{
		int size = 0;
		URL url = null;
		HttpURLConnection conn = null;
		try
		{
			PacketFragmentList list = mediaSegment.getFragmentList();
			if (list != null && list.size() != 0)
			{
				url = new URL((isSendSSL ? "https://" : "http://") + httpHost + getPortStr() + "/" + getDestinationPath() + "/" + mediaSegment.getUri());
				conn = (HttpURLConnection)url.openConnection();
				conn.setConnectTimeout(connectionTimeout);
				conn.setReadTimeout(readTimeout);
				conn.setRequestMethod("PUT");
				conn.setDoOutput(true);

				Iterator<IPacketFragment> itr = list.getFragments().iterator();
				while (itr.hasNext())
				{
					IPacketFragment fragment = itr.next();
					if (fragment.getLen() <= 0)
						continue;
					byte[] data = fragment.getBuffer();

					conn.getOutputStream().write(data);
					size += data.length;
				}
				int status = conn.getResponseCode();
				if (status < 200 || status >= 300)
					size = 0;
			}
			else
				size = 1;  // empty fragment list.
		}
		catch (Exception e)
		{
			logError("sendMediaSegment", "Failed to send media segment data to " + url.toString(), e);
			size = 0;
		}
		finally
		{
			if (conn != null)
			{
				conn.disconnect();
			}
		}
		return size;
	}

	@Override
	public int deleteMediaSegment(MediaSegmentModel mediaSegment)
	{
		int retVal = 0;
		URL url = null;
		HttpURLConnection conn = null;
		OutputStream out = null;
		try
		{
			url = new URL((isSendSSL ? "https://" : "http://") + httpHost + getPortStr() + "/" + getDestinationPath() + "/" + mediaSegment.getUri());
			conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(connectionTimeout);
			conn.setReadTimeout(readTimeout);
			conn.setRequestMethod("DELETE");
			int status = conn.getResponseCode();
			if (status >= 200 || status < 300)
				retVal = 1;
			else
				logWarn("deleteMediaSegment", "Failed to delete media segment " + mediaSegment.getUri() + ", http status: " + status);
		}
		catch (Exception e)
		{
			logError("deleteMediaSegment", "Failed to delete media segment " + url.toString(), e);
			retVal = 0;
		}
		finally
		{
			if (out != null)
			{
				try
				{
					out.close();
				}
				catch (IOException e)
				{
					// ignore
				}
			}
			if (conn != null)
			{
				conn.disconnect();
			}
		}
		return retVal;
	}

	@Override
	public void setSendToBackupServer(boolean backup)
	{
		this.backup = backup;
	}

	@Override
	public boolean isSendToBackupServer()
	{
		return backup;
	}

	@Override
	public boolean outputOpen()
	{
		return true;
	}

	@Override
	public boolean outputClose()
	{
		return true;
	}

	@Override
	public String getDestionationLogData()
	{
		return "{\"" + (isSendSSL ? "https://" : "http://") + httpHost + getPortStr() + "/" + getDestinationPath() + "\"}";
	}

	private int writePlaylist(PlaylistModel playlist, String playlistPath)
	{
		System.out.println("***********************************" + playlistPath);
		int retVal = 0;
		URL url = null;
		HttpURLConnection conn = null;
		try
		{
			url = new URL((isSendSSL ? "https://" : "http://") + httpHost + getPortStr() + "/" + playlistPath);
			conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(connectionTimeout);
			conn.setReadTimeout(readTimeout);
			conn.setDoOutput(true);
			conn.setRequestMethod("PUT");

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			PlaylistWriter writer = new PlaylistWriter(out, getContextStr());
			String outStr = "";
			if (writer.write(playlist))
			{
				outStr = out.toString();
				byte[] bytes = outStr.getBytes();
				conn.getOutputStream().write(bytes);
				retVal = bytes.length;
			}
			int status = conn.getResponseCode();
			if (status < 200 || status >= 300)
			{
				retVal = 0;
				logWarn("writePlaylist", "Failed to send playlist data to " + playlist.getUri() + ", http status: " + status + ", playlist: " + outStr);
			}

		}
		catch (Exception e)
		{
			logError("sendMediaSegment", "Failed to send playlist data to " + url.toString(), e);
			retVal = 0;
		}
		finally
		{
			if (conn != null)
			{
				conn.disconnect();
			}
		}
		return retVal;
	}

	private String getDestinationPath()
	{
		if (!backup)
			return basePath + getDstStreamName();
		return basePath + getDstStreamName() + "-b";
	}

	private String getPortStr()
	{
		String portStr = "";
		if (port != DEFAULT_HTTP_PORT && port != DEFAULT_HTTPS_PORT)
			portStr = ":" + port;
		return portStr;
	}
}
