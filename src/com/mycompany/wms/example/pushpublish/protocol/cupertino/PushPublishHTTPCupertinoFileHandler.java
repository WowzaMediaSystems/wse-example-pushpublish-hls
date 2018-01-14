/*
 * This code and all components (c) Copyright 2018, Wowza Media Systems, LLC. All rights reserved.
 * This code is licensed pursuant to the BSD 3-Clause License.
 */
package com.mycompany.wms.example.pushpublish.protocol.cupertino;

import java.io.*;
import java.net.*;
import java.util.*;

import com.wowza.util.*;
import com.wowza.wms.manifest.model.m3u8.*;
import com.wowza.wms.manifest.writer.m3u8.*;
import com.wowza.wms.pushpublish.protocol.cupertino.PushPublishHTTPCupertino;
import com.wowza.wms.server.*;
import com.wowza.wms.util.*;

public class PushPublishHTTPCupertinoFileHandler extends PushPublishHTTPCupertino
{

	/*
	 * Directory layout is as follow:
	 * <root-dir> is the directory in the map file
	 * <dstStreamName> is gotten from the base implementation
	 * <sessionId> is gotten from the base implementation, this is here in case the incoming stream comes and goes, we don't overwrite media segments
	 *
	 *
	 * <root-dir>/<dstStreamName>/playlist.m3u8 (master playlist)
	 * <root-dir>/<dstStreamName>/chunklist.m3u8 (media playlist)
	 * <root-dir>/<groupName>/playlist.m3u8 (group master playlist)
	 * <root-dir>/<dstStreamName>/<sessionId>/media_x.ts (media segments)
	 *
	 * We reference the media playlists and the media segements using a preceeding "../" such that when the group master playlist references the different
	 * media playlists that represent renditions, it can pick them up from different implementations without modification.
	 *
	 * If this is sending to a backup server, the <dstStreamName> has "-b" appended to the end
	 *
	 */

	File rootDir = null;
	boolean backup = false;
	String groupName = null;

	public PushPublishHTTPCupertinoFileHandler() throws LicensingException
	{
		super();
	}

	@Override
	public void load(HashMap<String, String> dataMap)
	{
		super.load(dataMap);

		String destStr = PushPublishUtils.removeMapString(dataMap, "file.root");
		if (destStr != null)
		{
			this.rootDir = new File(destStr );
			logInfo("load", "Using: " + this.rootDir);
			if (!this.rootDir.exists())
			{
				this.rootDir.mkdir();
				logInfo("load", "Created destination folder: " + this.rootDir);
			}
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

		String path = "../" + getDstStreamName() + (this.backup ? "-b/":"/") + playlist.getUri().toString();
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

		String path = "../" + getDstStreamName() + (this.backup ? "-b/":"/") + playlist.getUri().toString();
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
		FileOutputStream output = null;
		try
		{
			File destinationDir = getDestionationGroupDir();
			if (!destinationDir.exists())
				destinationDir.mkdirs();

			File playlistFile = new File(destinationDir + "/" + playlist.getUri());
			if (!playlistFile.exists())
				playlistFile.createNewFile();

			output = new FileOutputStream(playlistFile, false);  // don't append
			retVal = writePlaylist(playlist, output);
		}
		catch (Exception e)
		{
			logError("sendGroupMasterPlaylist", "Failed to send master playlist to: " + playlist.getUri(), e);
		}
		finally
		{
			if (output != null)
				try {
					output.flush();
					output.close();
				} catch (Exception e2)
				{

				};
		}
		return retVal;
	}

	@Override
	public int sendMasterPlaylist(PlaylistModel playlist)
	{
		int retVal = 0;
		FileOutputStream output = null;
		try
		{
			File destinationDir = getDestionationDir();
			if (!destinationDir.exists())
				destinationDir.mkdirs();

			File playlistFile = new File(destinationDir + "/" + playlist.getUri());
			if (!playlistFile.exists())
				playlistFile.createNewFile();

			output = new FileOutputStream(playlistFile, false);  // don't append
			retVal = writePlaylist(playlist, output);
		}
		catch (Exception e)
		{
			logError("sendMasterPlaylist", "Failed to send master playlist to: " + playlist.getUri(), e);
		}
		finally
		{
			if (output != null)
				try {
					output.flush();
					output.close();
				} catch (Exception e2)
				{

				};
		}
		return retVal;
	}

	@Override
	public int sendMediaPlaylist(PlaylistModel playlist)
	{
		int retVal = 0;
		FileOutputStream output = null;
		try
		{
			File destinationDir = getDestionationDir();
			if (!destinationDir.exists())
				destinationDir.mkdirs();

			File playlistFile = new File(destinationDir + "/" + playlist.getUri());
			if (!playlistFile.exists())
				playlistFile.createNewFile();

			output = new FileOutputStream(playlistFile, false);  // don't append
			retVal = writePlaylist(playlist, output);
		}
		catch (Exception e)
		{
			logError("sendMediaPlaylist", "Failed to send media playlist to: " + playlist.getUri(), e);
		}
		finally
		{
			if (output != null)
				try {
					output.flush();
					output.close();
				} catch (Exception e2)
				{

				};
		}
		return retVal;
	}

	@Override
	public int sendMediaSegment(MediaSegmentModel mediaSegment)
	{
		int retVal = 0;
		FileOutputStream output = null;
		try
		{
			File destinationDir = getDestionationDir();
			String path = destinationDir + "/" + mediaSegment.getUri();
			int idx = path.lastIndexOf("/media_");
			path = path.substring(0,idx);

			File file = new File(path);

			if (!file.exists())
				file.mkdirs();

			file = new File(destinationDir + "/" + mediaSegment.getUri());
			if (!file.exists())
				file.createNewFile();

			PacketFragmentList list = mediaSegment.getFragmentList();
			if (list != null)
			{
				output = new FileOutputStream(file, false);

				Iterator<IPacketFragment> itr = list.getFragments().iterator();
				while (itr.hasNext())
				{
					IPacketFragment fragment = itr.next();
					if (fragment.getLen() <= 0)
						continue;
					byte[] data = fragment.getBuffer();

					output.write(data);
					retVal += data.length;
				}
			}
			else
				retVal = 1;  // empty fragment list.
		}
		catch (Exception e)
		{
			logError("sendMediaSegment", "Failed to send media segment data to " + mediaSegment.getUri(), e);
		}
		finally
		{
			if (output != null)
			{
				try
				{
					output.flush();
					output.close();
				}
				catch (Exception e)
				{
				}
			}
		}

		return retVal;
	}

	@Override
	public int deleteMediaSegment(MediaSegmentModel mediaSegment)
	{
		int retVal = 0;

		File segment = new File(getDestionationDir() + "/" + mediaSegment.getUri());
		if (segment.exists())
			if (segment.delete())
				retVal = 1;

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
		return this.backup;
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
		File destinationDir = getDestionationDir();
		String retVal = "Invalid Destination " + destinationDir.toString();
		try
		{
			retVal = destinationDir.toURI().toURL().toString();
		}
		catch (MalformedURLException e)
		{
			logError("getDestionationLogData", "Unable to convert " + destinationDir + " to valid path" ,e);
		}

		return retVal;
	}

	private int writePlaylist(PlaylistModel playlist, FileOutputStream output) throws IOException
	{
		int retVal = 0;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		PlaylistWriter writer = new PlaylistWriter(out, getContextStr());
		if (writer.write(playlist))
		{
			String outStr = out.toString();
			byte[] bytes = outStr.getBytes();
			output.write(bytes);
			retVal = bytes.length;
		}

		return retVal;
	}

	private File getDestionationDir()
	{
		if (!this.backup)
			return new File(this.rootDir + "/" + getDstStreamName());
		return new File(this.rootDir + "/" + "/" + getDstStreamName()+"-b");
	}

	private File getDestionationGroupDir()
	{
		if (!this.backup)
			return new File(this.rootDir + "/" + this.groupName);
		return new File(this.rootDir + "/" + getDstStreamName()+"-b");
	}
}
