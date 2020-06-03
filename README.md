# wse-example-pushpublishing-hls

## Create a custom Wowza Streaming Engine Apple HLS stream target (push publishing)
[Wowza Streaming Engine™ media server software](https://www.wowza.com/products/streaming-engine) version 4.5.0 and later enables you to develop your own Apple HLS (cupertino) Stream Target to support custom destinations that may not be supported by the built-in push publishing HLS profiles.  

The abstract base class, which you must extend in your custom implementation, enables you to create and maintain playlists, and to gather audio/video content from Wowza Streaming Engine live stream packetizers and package it into media segments. For your convenience, we've also modeled the playlists and media segments in Java classes and provided utility classes and code examples that demonstrate how to convert the models into content that can be transmitted to the destination and consumed by any player. 

>	**Important:**
>	* In the code, the Stream Targets feature in Wowza Streaming Engine is referred to as *push publishing*. Apple HLS is referred to as *Cupertino* in the code and Streaming Engine configuration files.
>
>	* This document is aimed for developers who know how to program in Java, how players access playlists and media segments from Stream Target destinations, and how to develop and code the mechanism to transmit the playlist and media segment content from Wowza Streaming Engine to the destination using Java.

## Contents
- [Prerequisites](#Prerequisites)
- [Add a custom Apple HLS push-publishing profile to Wowza Streaming Engine](#PushPublishingProfilesCustom)
- [Sample Implementation](#SampleImplementation)
- [About the Apple HLS push-publishing workflow in Wowza Streaming Engine](#AppleHLSWorkflow)
- [Configure for redundancy](#Redundancy)
- [Configure an Adaptive Group](#AdaptiveGroup)
- [More resources](#Resources)
- [Contact](#Contact)
- [License](#License)

<a name="Prerequisites"></a>
## Prerequisites
Wowza Streaming Engine 4.5.0 or later.

<a name="PushPublishingProfilesCustom"></a>
## Add a custom Apple HLS push-publishing profile to Wowza Streaming Engine
To add a custom push publishing profile, you must:
1. Add a profile entry and any profile-specific properties to the **[install-dir]/conf/PushPublishProfilesCustom.xml** file. (If the file doesn't exist, create it.) See [Push publishing profiles](#PushPublishingProfiles) below.

2. Copy the jar file containing your custom implementation to **[install-dir]/lib**.

3. Restart Wowza Streaming Engine.

4. Enable the Stream Targets feature for an application in Wowza Streaming Engine Manager (click your application's name in the contents panel, click **Stream Targets** in the contents panel, and then click **Enable Stream Targets**). When Stream Targets is enabled for an application, a **PushPublishMap.txt** file is added to the **[install-dir]/conf/[application name]** folder.

5. Add entries for your new profile to the application's **PushPublishMap.txt** map file. See [Map file entries](#PushPublishMap) below.

<a name="PushPublishingProfiles"></a>
### Push publishing profiles
The following is an example push publishing profile entry:
```
<?xml version="1.0" encoding="UTF-8"?>
<Root>
	<PushPublishProfiles>
		<PushPublishProfile>
			<Name>profilename</Name>
			<Protocol>protocol</Protocol>
			<BaseClass>classpath</BaseClass>
			<UtilClass>utility classpath</UtilClass>
			<Implementation>
				<Name>destinationname</Name>
			</Implementation>
			<HTTPConfiguration>
			</HTTPConfiguration>
		</PushPublishProfile>
	</PushPublishProfiles>
</Root>
```
Where:

* **profilename** - The name for your new profile. The name must be unique and can't have spaces. (Example: **destinationx-cupertino**)

* **protocol** - The protocol used to transmit the playlists and media segments to the destination. (Example: **HTTP**)

* **classpath** - The full package and classname for the class that extends **com.wowza.wms.pushpublish.protocol.cupertino.PushPublishHTTPCupertino** and pushes the content to the new destination. (Example:  **com.mycompany.wms.plugin.pushpublishing.protocol.cupertino.DestinationXHandler**)

* **utility classpath** - (Optional) The full package and classname for the class that implements the **IPushPublishUtil** interface. This class is used to modify and validate map entries specific to the profile without needing a running push-publish session for the map entry. If no custom utility methods are required, leave this field empty.

* **destinationname** - A short string that identifies the supported destination. (Example: **DestinationX**)

<a name="PushPublishMap"></a>
### Map file entries
A **PushPublishMap.txt** entry must include the following information:

```
<streamname>={"entryName":"<entryname>", "profile":"<profilename>", "streamName":"<outputstreamname>", "destinationName":"<destinationname>"}
```

Where:
* **&lt;streamname&gt;** - The name of the incoming stream. This is published to the destination. (Example: **myStream**)

* **&lt;entryname&gt;** - The unique name for the entry in the **PushPublishMap.txt** file. (Example: **myStreamToDestinationX**)

* **&lt;profilename&gt;** - The **PushPublishProfile.Name** element value that you specified when you added your custom profile to the **PushPublishProfilesCustom.xml** file. (Example: **destinationx-cupertino**)

* **&lt;destinationname&gt;** - The identifier for the destination. (Example: **destination-x**)

The following optional parameters are also supported in map entries:
* **"enabled":"false"** - Enables/disables map file entries without having to remove them. The default value (**true**) enables map file entries. To disable map file entries, set to **false**.

* **"debugLog":"true"** - Enables debug logging for a specific entry if **true**.  The default value is **false**.

* **"cupertino.renditions":"audiovideo|audioonly"** - Controls the rendition types to be generated by the base cupertino push-publishing profile. The default value (**"audiovideo"**) creates a rendition with both audio and video in the media segments. Set to **"audioonly"** to create an audio-only rendition. To generate both types, use a pipe-separated list (|). Note that you may need to configure the application to generate an Apple HLS audio-only rendition for the **"audioonly"** option to be honored. For more information, see [How to create Apple App Store compliant streams (audio only rendition)](https://www.wowza.com/docs/how-to-create-apple-app-store-compliant-streams).

* **"destinationServer":"backup"** - Controls whether the destination is treated as a **primary**, **secondary**, or **redundant** destination. The default value is **primary**. For more information, see [Configure for redundancy](#Redundancy) below.

* **"host":"myHost.com"** - The host domain you want to push to (if required by your profile).

* **"port":"1234"** - The host port you want to push to (if required by your profile).

* **"username":"myUsername"** - The user name required to connect to the destination (if required by your profile).

* **"password":"myPassword"** - The password required to connect to the destination (if required by your profile).

* **"http.manifestDebug":"false"** - Enables lower-level Apple HLS playlist debug logging within the base classes if **true**. The default value is **false**.

* **"http.playlistCount":"false"** - The number of chunks maintained in the playlist. The default value is **0**. When set to **0**, the **playlistCount** is defined by the live stream packetizer **cupertinoPlaylistChunkCount** property, which has a default value of **3**.

* **"http.playlistAcrossSessions":"false"** - Set to **true** to maintain playlists across push-publishing sessions. The default value is **false**.

* **"http.playlistTimeout":"false"** - The time, in milliseconds, that a cross-session playlist is maintained. The default value is **120000** (2 minutes).

* **"adaptiveGroup":"Group1"** - Enables multiple map entries to be grouped into a multi-rendition playlist. The value is a name for the group. For more information, see [Configure an Adaptive Group](#AdaptiveGroup) below.

* **"setSendToBackupServer":"false"** - Instructs the profile entry instance to push to its **backup** destination endpoint, if applicable. The default value is **false**. Note that use of this parameter can vary based on your implementation.

For more information about these properties, see [About map profiles](https://www.wowza.com/docs/how-to-push-streams-to-cdns-and-other-services-push-publishing#profiles).

Additionally, if your implementation supports them, you can define custom parameters for your profile and specify them in your map file using simple JSON **"key":"value"** notation.

<a name="SampleImplementation"></a>
## Sample Implementation
The sample implementations we've provided are fully functional and can be used as a starting point for your own custom implementations. The **File** sample implementation writes playlists and media segments to local disk, which can then be played back using a player such as [VLC media player](http://www.videolan.org/vlc/index.html). The **Http** sample implementation writes playlists and media segments to a HTTP server that can handle **HTTP PUT** requests.

To configure the sample implementations:

1. Copy the **wse-example-pushpublishing-hls.jar** file from the package into your Wowza Streaming Engine **[install-dir]/lib** folder.

2. Add the following PushPublishProfile to the **PushPublishProfilesCustom.xml** file (or use the **PushPublishProfilesCustom.xml** file supplied in the **conf** folder):

	 ```
	 <PushPublishProfile>
		 <Name>cupertino-file</Name>
		 <Protocol>HTTP</Protocol>
		 <BaseClass>com.mycompany.wms.example.pushpublish.protocol.cupertino.PushPublishHTTPCupertinoFileHandler</BaseClass>
		 <UtilClass></UtilClass>
		 <Implementation>
			 <Name>Cupertino File</Name>
		 </Implementation>
		 <HTTPConfiguration>
		 </HTTPConfiguration>
	 </PushPublishProfile>
	 <PushPublishProfile>
		 <Name>cupertino-http</Name>
		 <Protocol>HTTP</Protocol>
		 <BaseClass>com.mycompany.wms.example.pushpublish.protocol.cupertino.PushPublishHTTPCupertinoHTTPHandler</BaseClass>
		 <UtilClass></UtilClass>
		 <Implementation>
			 <Name>Cupertino HTTP</Name>
		 </Implementation>
		 <HTTPConfiguration>
		 </HTTPConfiguration>
	 </PushPublishProfile>
	 ```
3. Enable the Stream Targets feature on an application in Wowza Streaming Engine Manager.

4. Restart Wowza Streaming Engine.

5. Add one or more of the sample entries below to the application's **PushPublishMap.txt** file:

>	**Important:** You must change the **"file.root"** parameter value to a location on your local hard drive that Wowza Streaming Engine can write to. When you start streaming to **[wowza-ip-address]:[port]/[application]/myStream**, one or more folders will appear in the location referenced by the **"file.root"** parameter in the map entries.

#### Sample Entries
The following samples use a stream named **myStream**. Replace **myStream** with the name of your stream.

```
#cupertino-file entries
myStream={"entryName":"myStream", "profile":"cupertino-file", "streamName":"myOutputStream", "destinationName":"filesystem", "file.root":"c:\temp\hlsfile"}

myStream={"entryName":"myStreamBackup", "profile":"cupertino-file", "streamName":"myOutputStream", "destinationName":"filesystem", "file.root":"c:\temp\hlsfile", "destinationServer":"backup" }

myStream={"entryName":"myStreamRedundant", "profile":"cupertino-file", "streamName":"myOutputStream", "destinationName":"filesystem", "file.root":"c:\temp\hlsfile", "destinationServer":"redundant" }

#cupertino-http entries
myStream={"entryName":"myStream-http", "profile":"cupertino-http", "streamName":"myOutputStream", "destinationName":"webserver", "host":"example.com", "http.path":"hls"}

myStream={"entryName":"myStreamBackup-http", "profile":"cupertino-http", "streamName":"myOutputStream", "destinationName":"webserver", "host":"example.com", "http.path":"hls", "destinationServer":"backup" }

myStream={"entryName":"myStreamRedundant-http", "profile":"cupertino-http", "streamName":"myOutputStream", "destinationName":"webserver", "host":"example.com", "http.path":"hls", "destinationServer":"redundant" }
```

For the following sample entries, the transcoder must be enabled and configured to use the default template (**Transrate**) for myStream. For more information, see [How to set up and run Wowza Transcoder for live streaming](https://www.wowza.com/docs/how-to-set-up-and-run-wowza-transcoder-for-live-streaming).

```
#cupertino-file entries
myStream_360p={"entryName":"myStream360p", "profile":"cupertino-file", "streamName":"myOutputStream360p", "destinationName":"filesystem", "file.root":"c:\temp\hlsfile", "adaptiveGroup":"Group1"}

myStream_160p={"entryName":"myStream160p", "profile":"cupertino-file", "streamName":"myOutputStream160p", "destinationName":"filesystem", "file.root":"c:\temp\hlsfile", "adaptiveGroup":"Group1"}

#cupertino-http entries
myStream_360p={"entryName":"myStream360p-http", "profile":"cupertino-http", "streamName":"myOutputStream360p", "destinationName":"webserver", "host":"example.com", "http.path":"hls", "adaptiveGroup":"Group1"}

myStream_160p={"entryName":"myStream160p-http", "profile":"cupertino-http", "streamName":"myOutputStream160p", "destinationName":"webserver", "host":"example.com", "http.path":"hls", "adaptiveGroup":"Group1"}
```

<a name="AppleHLSWorkflow"></a>
## About the Apple HLS push-publishing workflow in Wowza Streaming Engine
Before you can send an Apple HLS (cupertino) stream to a custom destination, the **ModulePushPublishing** module must monitor the application, identify the correct profile, and then create **MediaSegmentModels** and **PlaylistModels** for the stream. After all **MediaSegmentModels** and **PlaylistModels** are created and initialized, the stream is transmitted to the destination, using communication sessions as necessary.

### Preparing the stream for transmission
When the Stream Targets feature is enabled on an application, the **ModulePushPublishing** module monitors the application for the ingestion of new streams. When a new stream is detected, the module searches the application's **PushPublishMap.txt** file for matching stream name entries.

When a stream name match is found, the module then searches the list of push-publishing profiles for the **"profile"** specified in the map entry. When a profile name match is found, the module creates an instance of the **&lt;PushPublishProfile&gt;&lt;BaseClass&gt;** and calls the instance's **init()** method.

The **init()** method should call **super.init()** so that the parent classes are properly initialized. The base class of the profile **init()** method then calls the **load()** method of the profile twice: once with a map of the profile's **&lt;PushPublishProfile&gt;&lt;BaseClass&gt;**, **&lt;PushPublishProfile&gt;&lt;UtilClass&gt;** and again with the **&lt;PushPublishProfile&gt;&lt;Implementation&gt;** values of the map entry.

After the instance is initialized, the **ModulePushPublish** module starts a thread that's responsible for monitoring the Apple HLS live stream packetizer for the stream, looking for completed media segments that haven't been sent to the push-publish destination. When a new media segment is assembled by the live stream packetizer, the base class creates a **MediaSegmentModel** for the media segment and calls the implementation specific **updateMediaSegmentPlaybackURI(MediaSegmentModel)** method with the new **MediaSegmentModel**. This allows the playback URI for the media segment to be customized for playback from the destination.

If a **media PlaylistModel** hasn't been created yet for the stream, the base class creates and initializes a media PlaylistModel in accordance with the HLS/Pantos specification. Any new **MediaSegmentModels** are added to the media PlaylistModel, and the implementation-specific **updateMediaPlaylistPlaybackURI(PlaylistModel)** method is called by the new media PlaylistModel so that the playback URI for the media playlist can be customized for playback from the destination.

If a **master PlaylistModel** hasn't been created for the stream, the base class creates and initializes a master PlaylistModel in accordance with the HLS/Pantos specification. Any new **media PlaylistModels** are added to the master PlaylistModel and the implementation-specific **updateMasterPlaylistPlaybackURI(PlaylistModel)** method is called by the new master PlaylistModel so that the playback URI for the master playlist can be customized for playback from the destination.

### Transmitting the stream to the destination
After all new **MediaSegmentModels** and **PlaylistModels** for the stream are created and initialized, the **ModulePushPublish** module spawns another thread to transmit new content to the destination. Within the context of this thread, the base class builds a list of items (media segments and playlists) to be sent to the destination and then creates clones of those items to prevent modification of the items while they're being sent.  

If your custom destination requires or supports an open communication session, prior to requesting the transmission of any data, the base class calls the implementation-specific **outputOpen()** method. This indicates that it's about to send one or more items (media segments or playlists).

The implementation-specific version of the **sendMediaSegment()** method is responsible for transmitting the **PacketFragmentList**, which is the binary representation of the media segment contained in a **MediaSegmentModel**, to the destination. The base class calls the implementation-specific **sendMediaSegment(MediaSegmentModel)** for each media segment that must be sent. The implementation-specific **updateMediaSegmentPlaybackURI()** method is called with the **MediaSegmentModel** to set the URI that the player uses to access the **PacketFragmentList** data that was transmitted to the destination.

If the **media PlaylistModel** for the stream hasn't been transmitted or has changed since the last transmission, call the implementation-specific **sendMediaPlaylist(PlaylistModel)** method to convert the **media PlaylistModel** to a string and transmit it to the destination. The implementation-specific **updateMediaPlaylistPlaybackURI()** method is called to set the URI that the player uses to access the media playlist data that was transmitted to the destination.

Similarly, if the **master PlaylistModel** for the stream hasn't been transmitted or has changed since the last transmission, call the implementation-specific **sendMasterPlaylist(PlaylistModel)** method to convert the **master PlaylistModel** to a string and transmit it to the destination. The implementation-specific **updateMasterPlaylistPlaybackURI()** method is called to set the URI that the player uses to access the master playlist data that was transmitted to the destination.

> **Note:** The **com.wowza.wms.manifest.writer.m3u8.PlaylistWriter** class is available to verify, convert, and write a PlaylistModel. For more information, see the [Wowza Streaming Engine Server Side API](https://www.wowza.com/resources/serverapi/).

If any media segments are dropped from the **media PlaylistModel**, the implementation-specific **deleteMediaSegment(MediaSegmentModel)** method is called to request the removal of the media segment from the destination.

After all updated items have been sent to the destination, the base class calls the implementation-specific **outputClose()** method, allowing the destinations to close the communication session.

<a name="Redundancy"></a>
## Configure for redundancy
You can manually configure push publishing for redundancy by creating two map entries: one with the **"destinationServer"** parameter set to **"primary"**, and the other with the **"destinationServer"** parameter set to **"backup"**. The other parameters in the entries should be identical. For **backup** map entries, the base class calls the implementation-specific **setSendToBackupServer(true)** method.

Push publishing can also be configured to "automatically" implement redundancy by setting the **"destinationServer"** parameter to **"redundant"** in a single map entry. When an incoming stream matches a **redundant** map entry, two sessions are created in memory. Each session has identical settings, as specified in the map entry, except that one is automatically designated as the backup stream (**setSendToBackupServer()** is set with **true**) and the other is designated as the primary stream (**setSendToBackupServer()** is set with **false**). In this scenario, it's expected that the **PushPublishProfile** knows any destination-specific changes that are required for a **backup** session.

<a name="AdaptiveGroup"></a>
## Configure an Adaptive Group
> **Note:** In Wowza Streaming Engine 4.7.5.02 and earlier, the **Adaptive Group** parameter was called **Adaptive Groups**, and it supported a comma-separated list of group names. This enabled you to create a single stream target for each rendition and specify as many groups as you wanted to include the enredition in. However, due to changes in Akamai Media Service Live, one of our most common HLS destinations, the single rendition must now be pushed through multiple map entries, one for each group the rendition is part of.

To combine multiple outputs into an adaptive-bitrate (ABR) group, add the **"adaptiveGroup"** parameter to your map entries. Note that you must create a separate map entry for each rendition and each adaptive group that rendition is part of. When two or more map entries have the same **"adaptiveGroup"** value, the **ModulePushPublish** module creates a **group master PlaylistModel** and adds the **media PlaylistModel** for each session to the **group master PlaylistModel**. The module then calls one of the implementation-specific **updateGroupMasterPlaylistPlaybackURI(PlaylistModel) methods** to set the playback URI for the group.

After all members of the group successfully send their media playlists, the **ModulePushPublish** module calls one of the adaptive group member's implementation-specific **sendGroupMasterPlaylist(String, PlaylistModel)** to send the adaptive group playlist to the destination.

<a name="Resources"></a>
## More resources
[How to extend Wowza Streaming Engine using the Wowza IDE](https://www.wowza.com/docs/how-to-extend-wowza-streaming-engine-using-the-wowza-ide)

Wowza Media Systems™ provides developers with a platform to create streaming applications and solutions. See [Wowza Developer Tools](https://www.wowza.com/developer) to learn more about our APIs and SDK.

[Wowza Streaming Engine Server-Side API](https://www.wowza.com/resources/serverapi/)

[HTTP Live Streaming Pantos Specification](https://tools.ietf.org/html/draft-pantos-http-live-streaming-19)

[How to use CDNs and services to distribute live streams (push publishing)](https://www.wowza.com/docs/how-to-push-streams-to-cdns-and-other-services-push-publishing)

[How to send Apple HLS streams to a generic destination](https://www.wowza.com/docs/how-to-send-apple-hls-streams-to-a-generic-destination)

<a name="Contact"></a>
## Contact
[Wowza Media Systems, LLC](https://www.wowza.com/contact)

<a name="License"></a>
## License
This code is distributed under the [BSD 3-Clause License](https://github.com/WowzaMediaSystems/wse-example-pushpublish-hls/blob/master/LICENSE.txt).
