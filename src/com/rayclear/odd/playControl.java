package com.rayclear.odd;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class playControl extends ModuleBase {
	private String serverUrl;
	private String postStreamStart(String clientId, String streamName, String streamType, String ip, String agent) {
		String posturl = serverUrl + "/v1/publish/stream_start";
		getLogger().info(posturl);
		String content = "";
		
		try {
			HttpPost httpPost = new HttpPost(posturl);
			
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("stream_id",streamName));
			params.add(new BasicNameValuePair("client_id",clientId));
			params.add(new BasicNameValuePair("stream_type",streamType));
			params.add(new BasicNameValuePair("ip",ip));
			params.add(new BasicNameValuePair("agent",agent));


			httpPost.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));

			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			content = EntityUtils.toString(entity, "UTF-8");
			if (statusCode >= 300 || statusCode < 200) {
				getLogger().info("------------------Post Fail--------------------");
			}
			if (statusCode < 300 && statusCode >= 200) {
				getLogger().info("------------------Post Success--------------------");
			}
		} catch (ClientProtocolException e) {
			// e.printStackTrace();
			getLogger().info(e.getMessage());
		} catch (IOException e) {
			// e.printStackTrace();
			getLogger().info(e.getMessage());
		}
		return content;		
	}
	private void postStreamStop(String clientId, String streamName, long bytes) {
		String posturl = serverUrl + "/v1/publish/stream_terminate";
		getLogger().info(posturl);
		try {
			
			HttpPost httpPost = new HttpPost(posturl);
			
			List<NameValuePair> params=new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("stream_id",streamName));
			params.add(new BasicNameValuePair("client_id",clientId));
			httpPost.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpResponse response = httpClient.execute(httpPost);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 300 || statusCode < 200) {
				getLogger().info("------------------Post Fail--------------------");
			}
			if (statusCode < 300 && statusCode >= 200) {
				getLogger().info("------------------Post Success--------------------");
			}
		} catch (ClientProtocolException e) {
			// e.printStackTrace();
			getLogger().info(e.getMessage());
		} catch (IOException e) {
			// e.printStackTrace();
			getLogger().info(e.getMessage());
		}
	}

	public void onAppStart(IApplicationInstance appInstance) {
		String fullname = appInstance.getApplication().getName() + "/"
				+ appInstance.getName();
		getLogger().info("onAppStart: " + fullname);
		serverUrl = appInstance.getProperties().getPropertyStr("serverUrl");
		getLogger().info("serverUrl");

	}
	
	public void onHTTPSessionCreate(IHTTPStreamerSession httpSession) {
		getLogger().info(httpSession.getStreamName() + "************");
		getLogger().info(httpSession.getStream());

		getLogger().info("onHTTPSessionCreate: " + httpSession.getSessionId());
	    String result = postStreamStart("http_" + httpSession.getSessionId(), httpSession.getStreamName(), "HTTP", httpSession.getIpAddress(), httpSession.getUserAgent());
		if (result.equals("false")) {
			httpSession.rejectSession();
			getLogger().info("reject http session" + httpSession.getSessionId());
		}
	}
	public void onHTTPSessionDestroy(IHTTPStreamerSession httpSession) {
		getLogger().info(httpSession.getStreamName() + "************");
		getLogger().info(httpSession.getStream());
		getLogger().info("onHTTPSessionDestroy: " + httpSession.getSessionId());
	    postStreamStop("http_" + httpSession.getSessionId(), httpSession.getStreamName(), httpSession.getIOPerformanceCounter().getMessagesOutBytes());
	}
	
	public void onRTPSessionCreate(RTPSession rtpSession) {
		getLogger().info("onRTPSessionCreate: " + rtpSession.getSessionId());
		String uri = rtpSession.getUri();
		RTPUrl url = new RTPUrl(uri);
		String streamName = url.getStreamName();
	    String result = postStreamStart("rtsp_" + rtpSession.getSessionId(), streamName, "RTSP", rtpSession.getIp(), rtpSession.getUserAgent());
		if (result.equals("false")) {
			rtpSession.rejectSession();
			getLogger().info("reject rtsp session" + streamName);
		}
	}
	public void onRTPSessionDestroy(RTPSession rtpSession) {
		getLogger().info("onRTPSessionDestroy: " + rtpSession.getSessionId());
		String uri = rtpSession.getUri();
		RTPUrl url = new RTPUrl(uri);
		String streamName = url.getStreamName();
	    postStreamStop("rtsp_" + rtpSession.getSessionId(), streamName, rtpSession.getIOPerformanceCounter().getMessagesOutBytes());
	}
	

	public void onStreamCreate(IMediaStream stream) {
		getLogger().info("onStreamCreate: " + stream.getSrc());
		stream.addClientListener(new IMediaStreamActionNotify2() {
			
			@Override
			public void onPlay(IMediaStream stream, String streamName,
					double playStart, double playLen, int playReset) {
				Integer clientId = stream.getClientId();

				IClient client = stream.getClient();
				if (client != null) { // rtsp 的流也会走这里，但他的 client 是空的
					getLogger().info("request client id is: " + clientId);
					String result = postStreamStart("rtmp_" + clientId,
							streamName, "RTMP", client.getIp(), client.getFlashVer());
					if (result.equals("false")) {
						client.rejectConnection("Secure connection required.");
						client.shutdownClient();
					}
				}
			}
			
			@Override
			public void onStop(IMediaStream stream) {
				getLogger().info("data traffic: " + stream.getMediaIOPerformance().getMessagesOutBytes());
				if (stream.getClient() != null) {
					postStreamStop("rtmp_" + stream.getClientId(), stream
							.getName(), stream.getMediaIOPerformance()
							.getMessagesOutBytes());
				}
			}

			@Override
			public void onMetaData(IMediaStream stream, AMFPacket metaDataPacket) {
			}

			@Override
			public void onPauseRaw(IMediaStream stream, boolean isPause,
					double location) {
			}

			@Override
			public void onPause(IMediaStream stream, boolean isPause,
					double location) {
			}

			@Override
			public void onPublish(IMediaStream stream, String streamName,
					boolean isRecord, boolean isAppend) {
			}

			@Override
			public void onSeek(IMediaStream stream, double location) {
			}

			@Override
			public void onUnPublish(IMediaStream stream, String streamName,
					boolean isRecord, boolean isAppend) {
			}
		});
	}

}
