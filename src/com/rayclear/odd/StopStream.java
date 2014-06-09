package com.rayclear.odd;

import com.wowza.wms.application.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.rtp.model.*;
import com.wowza.wms.httpstreamer.model.*;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.*;
import com.wowza.wms.httpstreamer.smoothstreaming.httpstreamer.*;


import java.io.*;
import java.util.*;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.client.IClient;
import com.wowza.wms.http.*;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.logging.*;
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.stream.*;
import com.wowza.wms.stream.mediacaster.MediaStreamMediaCasterUtils;
import com.wowza.wms.vhost.IVHost;
import com.wowza.wms.vhost.StreamItem;
import com.wowza.wms.vhost.StreamList;


public class StopStream extends HTTProvider2Base {

	public void onHTTPRequest(IVHost vhost, IHTTPRequest req, IHTTPResponse resp) {
		if (!doHTTPAuthentication(vhost, req, resp))
			return;
		WMSLoggerFactory.getLogger(null).info("gehuaHttp HTTPRequest");

		Map<String, List<String>> params = req.getParameterMap();

		String action = "";
		String app = "";
		String streamName = "";
		String report = "";
		if (req.getMethod().equalsIgnoreCase("get")
				|| req.getMethod().equalsIgnoreCase("post")) {
			req.parseBodyForParams(true);

			try {
				if (params.containsKey("action")) {
					action = params.get("action").get(0);
				} else {
					report += "<BR>" + "action is required";
				}

				WMSLoggerFactory.getLogger(null).info(
						"gehuaHttp action: " + action);

				if (params.containsKey("app")) {
					app = params.get("app").get(0);
				} else {
					report += "<BR>" + "app is required";
				}

				WMSLoggerFactory.getLogger(null).info("gehuaHttp app: " + app);

				if (params.containsKey("streamname")) {
					streamName = params.get("streamname").get(0);
				} else {
					report += "<BR>" + "streamname is required";
				}

				WMSLoggerFactory.getLogger(null).info(
						"gehuaHttp streamName: " + streamName);

			} catch (Exception ex) {
				report = "Error: " + ex.getMessage();
			}
		} else {
			report = "Nothing to do.";
		}

		
			IApplicationInstance appInstance = vhost.getApplication(app)
					.getAppInstance("_definst_");
			String limitCountStr = appInstance.getProperties().getPropertyStr("limitCount");
			WMSLoggerFactory.getLogger(null).info(limitCountStr);
			//Integer limitCount = Integer.parseInt(limitCountStr);



			if (action.equalsIgnoreCase("stopStream")
					&& report.equalsIgnoreCase("")) {
				WMSLoggerFactory.getLogger(null).info("stopStream");

				String streamTypeStr = appInstance.getStreamType();
				

				
				
				List<IHTTPStreamerSession> cupertinoSession=appInstance.getHTTPStreamerSessions(streamName);
				if(cupertinoSession!=null)
				{
					Iterator<IHTTPStreamerSession> iterPlay=cupertinoSession.iterator();
					while(iterPlay.hasNext())
					{
						IHTTPStreamerSession playSession= iterPlay.next();
						playSession.rejectSession();
						WMSLoggerFactory.getLogger(null).warn("kill http");
					}										
				} else {
					WMSLoggerFactory.getLogger(null).warn(
							"httt stream not found: " + streamName);
				}

				boolean isLiveRepeaterEdge = false;
				while (true) {
					StreamList streamDefs = appInstance.getVHost()
							.getStreamTypes();
					StreamItem streamDef = streamDefs
							.getStreamDef(streamTypeStr);
					if (streamDef == null)
						break;
					isLiveRepeaterEdge = streamDef.getProperties()
							.getPropertyBoolean("isLiveRepeaterEdge",
									isLiveRepeaterEdge);
					break;
				}

				if (isLiveRepeaterEdge)
					streamName = MediaStreamMediaCasterUtils
							.mapMediaCasterName(appInstance, null, streamName);

				IMediaStream stream = appInstance.getStreams().getStream(
						streamName);
				if (stream != null) {
					// do stop stream
					List<IMediaStream> playStreams=appInstance.getPlayStreamsByName(stream.getName());
					if(playStreams!=null)
					{
						Iterator<IMediaStream> iterPlay=playStreams.iterator();
						Integer counter = 0;
						while(iterPlay.hasNext())
						{
							IMediaStream playSession = iterPlay.next();
							IClient client = playSession.getClient();
							appInstance.getVHost().removeClient(client.getClientId());
						}
					}		

				} else {
					WMSLoggerFactory.getLogger(null).warn(
							"rtmp stream not found: " + streamName);
					report = "Stream Not Found: " + streamName;
				}
				
				List<RTPSession> rtpStreams=appInstance.getRTPSessions(streamName);
				if(rtpStreams!=null)
				{
					Iterator<RTPSession> iterPlay=rtpStreams.iterator();
					while(iterPlay.hasNext())
					{
						RTPSession playSession= iterPlay.next();
						playSession.rejectSession();
					}
				}
				
				

				


			} else {
				WMSLoggerFactory.getLogger(null).warn(
						"gehuaHttp: action not found: " + action);
			}



		String retStr = "<html><head><title>HTTProvider LiveRecord</title></head><body><h1>"
				+ report + "</h1></body></html>";

		try {
			OutputStream out = resp.getOutputStream();
			byte[] outBytes = retStr.getBytes();
			out.write(outBytes);
		} catch (Exception e) {
			WMSLoggerFactory.getLogger(null).error(
					"HTTPLiveStreamRecord: " + e.toString());
		}

	}
}
