/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.teamcity;

import com.atlassian.bitbucket.setting.Settings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author jocs
 */
public class HttpConnector {
   
    public void Post(TeamcityConfiguration conf, String url, Map<String, String> parameters, Settings settings) {
        
        try {                  
            
            String urlstr = conf.getUrl() + url;

            URL urldata = new URL(urlstr);
            TeamcityLogger.logMessage(settings, "[HttpConnector][Post] Hook Request: "  + urlstr);
            
            String authStr = conf.getUserName() + ":" + conf.getPassWord();
            String authEncoded = Base64.encodeBase64String(authStr.getBytes());
            
            HttpURLConnection connection = (HttpURLConnection) urldata.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setRequestProperty("Authorization", "Basic " + authEncoded);
            connection.setRequestProperty("Content-Length", "0"); //JDK-6997628
            connection.getOutputStream().close();
            
            InputStream content = (InputStream)connection.getInputStream();
            BufferedReader in   = 
                new BufferedReader (new InputStreamReader (content));
            
            StringBuilder dataout = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                dataout.append(line);
            }
            
            TeamcityLogger.logMessage(settings, "[HttpConnector][Post] Hook Reply: "  + line);
            
        } catch (Exception e) {
            TeamcityLogger.logMessage(settings, "[HttpConnector][Post] Hook Exception: "  + e.getMessage());
            e.printStackTrace();
        }         
    }
    public boolean isReachable(TeamcityConfiguration conf, Settings settings) {
        try {       
            InetAddress address = InetAddress.getByName(conf.getUrl().replace("http://", "").replace("https://", ""));
            address.isReachable(500);
            return true;
        } catch (IOException e) {
            TeamcityLogger.logMessage(settings, "[HttpConnector][isReachable] Failed to reach server, skip queue checker thread to avoid deadlocks: " + e.getMessage());
            return false;
        }
    }

    public String Get(TeamcityConfiguration conf, String url, Settings settings) throws MalformedURLException, IOException {
        
      String urlstr = conf.getUrl() + url;

      URL urldata = new URL(urlstr);
      TeamcityLogger.logMessage(settings, "[HttpConnector][Get] Hook Request with timeout 5 seconds for connect: "  + urlstr + "-");

      String authStr = conf.getUserName() + ":" + conf.getPassWord();
      String authEncoded = Base64.encodeBase64String(authStr.getBytes());

      HttpURLConnection connection = (HttpURLConnection) urldata.openConnection();

      connection.setRequestMethod("GET");
      connection.setDoOutput(true);
      connection.setConnectTimeout(5000);
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Authorization", "Basic " + authEncoded);

      InputStream content = (InputStream)connection.getInputStream();
      BufferedReader in = new BufferedReader (new InputStreamReader (content));

      StringBuilder dataout = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
          dataout.append(line);
      }

      TeamcityLogger.logMessage(settings, "[HttpConnector][Get] Hook Reply: "  + line);

      return dataout.toString();       
    }
    
    public String Get(String url, Settings settings) throws MalformedURLException, IOException {
      String urlstr = url;
      URL urldata = new URL(urlstr);
      TeamcityLogger.logMessage(settings, "[HttpConnector][Get]  Hook Request with timeout 5 seconds for connect: "  + urlstr);
      HttpURLConnection connection = (HttpURLConnection) urldata.openConnection();
      connection.setConnectTimeout(5000);
      connection.setRequestMethod("GET");
      connection.setDoOutput(true);
      connection.setRequestProperty("Accept", "application/json");
      InputStream content = (InputStream)connection.getInputStream();
      BufferedReader in   = 
          new BufferedReader (new InputStreamReader (content));

      StringBuilder dataout = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
          dataout.append(line);
      }

      TeamcityLogger.logMessage(settings, "[HttpConnector][Get] Hook Reply: "  + line);
      return dataout.toString();
    }
    
    public String PostPayload(TeamcityConfiguration conf, String url, String payload, Settings settings) {
      try {                  
        String urlstr = conf.getUrl() + url;
        URL urldata = new URL(urlstr);
        TeamcityLogger.logMessage(settings, "[HttpConnector][PostPayload] Hook Request: "  + urlstr);
        String authStr = conf.getUserName() + ":" + conf.getPassWord();
        String authEncoded = Base64.encodeBase64String(authStr.getBytes());
        HttpURLConnection connection = (HttpURLConnection) urldata.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(5000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Basic " + authEncoded);
        if (payload != null) {
          connection.setRequestProperty("Content-Type", "application/xml; charset=utf-8");
          connection.setRequestProperty("Content-Length", Integer.toString(payload.length())); //JDK-6997628
          connection.getOutputStream().write(payload.getBytes("UTF8"));
          connection.getOutputStream().close();
        }

        InputStream content = (InputStream)connection.getInputStream();
        BufferedReader in   =  new BufferedReader (new InputStreamReader (content));

        StringBuilder dataout = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
          dataout.append(line);
        }

        TeamcityLogger.logMessage(settings, "[HttpConnector][PostPayload] Hook Reply: "  + line);
        return line;
      } catch (Exception e) {
        TeamcityLogger.logMessage(settings, "[HttpConnector][PostPayload] Hook Exception: "  + e.getMessage());
        e.printStackTrace();
        return e.getMessage();
      }         
    }    
}
