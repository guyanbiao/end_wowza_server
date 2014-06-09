package com.rayclear.odd;

import java.io.*;
import java.util.*;
import com.wowza.wms.vhost.*;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.server.*;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.MediaStreamMap;
import com.wowza.wms.application.*;
import com.wowza.wms.http.*;
import com.wowza.wms.client.*;
import com.wowza.wms.http.*;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
public class StreamCount extends HTTProvider2Base
{       
  private void outputConnectionInfo(StringBuffer ret, ConnectionCounter counter)
  {
    ret.append("<ConnectionsCurrent>"+counter.getCurrent()+"</ConnectionsCurrent>");
    ret.append("<ConnectionsTotal>"+counter.getTotal()+"</ConnectionsTotal>");
    ret.append("<ConnectionsTotalAccepted>"+counter.getTotalAccepted()+"</ConnectionsTotalAccepted>");
    ret.append("<ConnectionsTotalRejected>"+counter.getTotalRejected()+"</ConnectionsTotalRejected>");
  }

  public void onHTTPRequest(IVHost inVhost, IHTTPRequest req, IHTTPResponse resp)
  {
    if (!doHTTPAuthentication(inVhost, req, resp))
      return;
    StringBuffer ret = new StringBuffer();

    try
    {
      List vhostNames = VHostSingleton.getVHostNames();
      ret.append("<?xml version=\"1.0\"?>\n<WowzaMediaServerPro>");

      Iterator<String> iter = vhostNames.iterator();
      while (iter.hasNext())
      {
        String vhostName = iter.next();
        IVHost vhost = (IVHost)VHostSingleton.getInstance(vhostName);
        if (vhost != null)
        {
          ret.append("<VHost>");
          ret.append("<Name>"+vhostName+"</Name>");
          ret.append("<TimeRunning>"+vhost.getTimeRunningSeconds()+"</TimeRunning>");
          ret.append("<ConnectionsLimit>"+vhost.getConnectionLimit()+"</ConnectionsLimit>");

          outputConnectionInfo(ret, vhost.getConnectionCounter());

          List appNames = vhost.getApplicationNames();
          List<String> appFolders = vhost.getApplicationFolderNames();
          Iterator<String> appNameIterator = appFolders.iterator();
          while (appNameIterator.hasNext())
          {
            String applicationName = appNameIterator.next();
            ret.append("<Application>");
            ret.append("<Name>"+applicationName+"</Name>");
            boolean appExists = appNames.contains(applicationName);
            ret.append("<Status>"+(appExists?"loaded":"unloaded")+"</Status>");
            if (appExists)
            {
              IApplication application = vhost.getApplication(applicationName);
              if (application == null)
                continue;

              ret.append("<TimeRunning>"+application.getTimeRunningSeconds()+"</TimeRunning>");
              outputConnectionInfo(ret, application.getConnectionCounter());

              List appInstances = application.getAppInstanceNames();
              Iterator<String> iterAppInstances = appInstances.iterator();
              while (iterAppInstances.hasNext())
              {
                String appInstanceName = iterAppInstances.next();
                IApplicationInstance appInstance = application.getAppInstance(appInstanceName);
                if (appInstance == null)
                  continue;

                ret.append("<ApplicationInstance>");
                ret.append("<Name>"+appInstance.getName()+"</Name>");
                ret.append("<TimeRunning>"+appInstance.getTimeRunningSeconds()+"</TimeRunning>");

                outputConnectionInfo(ret, appInstance.getConnectionCounter());
                MediaStreamMap  msm=appInstance.getStreams();


                List<String> publishStreams =msm.getPublishStreamNames();
                Iterator<String> iterPublish = publishStreams.iterator();

                while(iterPublish.hasNext())
                {
                  IMediaStream stream = msm.getStream(iterPublish.next()); //appInstance.getClient(c);
                  if (stream == null)
                    continue;

                  ret.append("<PublishStream>");
                  ret.append("<Name>"+stream.getName()+"</Name>");
                  ret.append("<Type>"+stream.getStreamType()+"</Type>");
                  ret.append("<PublishStreamReady>"+stream.isPublishStreamReady(false, true)+"</PublishStreamReady>");
                  ret.append("<PublishAudioCodecId>"+stream.getPublishAudioCodecId()+"</PublishAudioCodecId>");
                  ret.append("<PublishVideoCodecId>"+stream.getPublishVideoCodecId()+"</PublishVideoCodecId>");
                  List<IMediaStream> playStreams=appInstance.getPlayStreamsByName(stream.getName());
                  if(playStreams!=null)
                  {
                    ret.append("<SessionsFlash count=\""+playStreams.size()+"\">");
                    Iterator<IMediaStream> iterPlay=playStreams.iterator();
                    while(iterPlay.hasNext())
                    {
                      IMediaStream playSession= iterPlay.next();
                      IClient playClient=playSession.getClient();
                      ret.append("<Session>");
                      ret.append("<ClientIP>"+playClient.getIp()+"</ClientIP>");
                      ret.append("<ClientFlashVer>"+playClient.getFlashVer()+"</ClientFlashVer>");
                      ret.append("<ClientId>"+playClient.getClientId()+"</ClientId>");
                      ret.append("<ClientConnectTime>"+playClient.getConnectTime()+"</ClientConnectTime>");
                      ret.append("</Session>");
                    }
                    ret.append("</SessionsFlash>");
                  }
                  else
                  {
                    ret.append("<SessionsFlash count=\"0\"/>");
                  }
                  String http_stream_name = stream.getName();
                  if(http_stream_name.contains("rtmp")) {
                	  String[] name_arr = http_stream_name.split("/");

                	  http_stream_name = name_arr[name_arr.length - 1];
                  }

                  List<IHTTPStreamerSession> cupertinoSession=appInstance.getHTTPStreamerSessions(http_stream_name);
                  if(cupertinoSession!=null)
                  {
                    ret.append("<SessionsCupertino count=\""+cupertinoSession.size()+"\">");
                    Iterator<IHTTPStreamerSession> iterPlay=cupertinoSession.iterator();
                    while(iterPlay.hasNext())
                    {
                      IHTTPStreamerSession playSession= iterPlay.next();
                      ret.append("<Session>");
                      ret.append("<ClientIP>"+playSession.getIpAddress()+"</ClientIP>");
                      ret.append("<ClientId>"+playSession.getSessionId()+"</ClientId>");

                      ret.append("<ClientUserAgent>"+playSession.getUserAgent()+"</ClientUserAgent>");
                      ret.append("<ClientConnectTime>"+playSession.getTimeRunning()+"</ClientConnectTime>");
                      ret.append("</Session>");
                    }                                                                               

                    ret.append("</SessionsCupertino>");
                  }
                  else
                  {
                    ret.append("<SessionsCupertino count=\"0\"/>");
                  }

                  List<IHTTPStreamerSession> smoothSession=appInstance.getHTTPStreamerSessions(IHTTPStreamerSession.SESSIONPROTOCOL_SMOOTHSTREAMING, stream.getName());
                  if(smoothSession!=null)
                  {                                                                       
                    ret.append("<SessionsSmooth count=\""+smoothSession.size()+"\">");
                    Iterator<IHTTPStreamerSession> iterPlay=smoothSession.iterator();
                    while(iterPlay.hasNext())
                    {
                      IHTTPStreamerSession playSession= iterPlay.next();
                      ret.append("<Session>");
                      ret.append("<ClientIP>"+playSession.getIpAddress()+"</ClientIP>");
                      ret.append("<ClientUserAgent>"+playSession.getUserAgent()+"</ClientUserAgent>");
                      ret.append("<ClientConnectTime>"+playSession.getTimeRunning()+"</ClientConnectTime>");
                      ret.append("</Session>");
                    }                                                                               
                    ret.append("</SessionsSmooth>");
                  }
                  else
                  {
                    ret.append("<SessionsSmooth count=\"0\"/>");
                  }

                  List<RTPSession> rtpStreams=appInstance.getRTPSessions(stream.getName());
                  if(rtpStreams!=null)
                  {
                    ret.append("<SessionsRTSP count=\""+rtpStreams.size()+"\">");
                    Iterator<RTPSession> iterPlay=rtpStreams.iterator();
                    while(iterPlay.hasNext())
                    {
                      RTPSession playSession= iterPlay.next();
                      ret.append("<Session>");
                      ret.append("<ClientIP>"+playSession.getIp()+"</ClientIP>");
                      ret.append("<ClientId>"+playSession.getSessionId()+"</ClientId>");
                      ret.append("<ClientUserAgent>"+playSession.getUserAgent()+"</ClientUserAgent>");
                      ret.append("</Session>");
                      //ret.append("<ClientConnectTime>"+playSession.get+"</ClientConnectTime>");
                    }
                    ret.append("</SessionsRTSP>");
                  }
                  else
                  {
                    ret.append("<SessionsRTSP count=\"0\"/>");
                  }

                  ret.append("</PublishStream>");

                }

                /*                                                              
                                                                                List<IClient> clients = appInstance.getClients();
                                                                                Iterator<IClient> iterClient = clients.iterator();
                                                                                while(iterClient.hasNext())
                                                                                {
                                                                                IClient client = iterClient.next(); //appInstance.getClient(c);
                                                                                if (client == null)
                                                                                continue;
                                                                                ret.append("<Client>");
                                                                                ret.append("<ClientId>"+client.getClientId()+"</ClientId>");
                                                                                ret.append("<FlashVersion>"+client.getFlashVer()+"</FlashVersion>");
                                                                                ret.append("<IpAddress>"+client.getIp()+"</IpAddress>");
                                                                                ret.append("<Referrer>"+client.getReferrer()+"</Referrer>");
                                                                                ret.append("<TimeRunning>"+client.getTimeRunningSeconds()+"</TimeRunning>");
                //ret.append("<Duration>"+((double)(System.currentTimeMillis()-client.getConnectTime())/1000.0)+"</Duration>");
                //ret.append("<DateStarted>"+client.getDateStarted()+"</DateStarted>");
                ret.append("<URI>"+client.getUri()+"</URI>");

                String protocolStr = "unknown";
                switch(client.getProtocol())
                {
                case RtmpSessionInfo.PROTOCOL_RTMP:
                protocolStr = client.isEncrypted()?"RTMPE":"RTMP";
                break;
                case RtmpSessionInfo.PROTOCOL_RTMPT:
                protocolStr = client.isSSL()?"RTMPS":(client.isEncrypted()?"RTMPTE":"RTMPT");
                break;
                }

                ret.append("<Protocol type=/""+client.getProtocol()+"/">"+protocolStr+"</Protocol>");
                ret.append("<IsSSL>"+client.isSSL()+"</IsSSL>");
                ret.append("<IsEncrypted>"+client.isEncrypted()+"</IsEncrypted>");
                ret.append("<Port>"+client.getServerHostPort().getPort()+"</Port>");
                ret.append("</Client>");
                                                                                }
                                                                                */                                                              
                ret.append("</ApplicationInstance>");
              }
            }

            ret.append("</Application>");
          }

          ret.append("</VHost>");
        }
      }

      ret.append("</WowzaMediaServerPro>");
    }
    catch (Exception e)
    {
      WMSLoggerFactory.getLogger(HTTPServerVersion.class).error("HTTPServerInfoXML.onHTTPRequest: "+e.toString());
      e.printStackTrace();
    }

    try
    {
      resp.setHeader("Content-Type", "text/xml");

      OutputStream out = resp.getOutputStream();
      byte[] outBytes = ret.toString().getBytes();
      out.write(outBytes);
    }
    catch (Exception e)
    {
      WMSLoggerFactory.getLogger(HTTPServerVersion.class).error("HTTPServerInfoXML.onHTTPRequest: "+e.toString());
      e.printStackTrace();
    }

  }
}
