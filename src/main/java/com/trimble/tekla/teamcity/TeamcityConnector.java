/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.teamcity;

import com.atlassian.bitbucket.setting.Settings;
import com.trimble.tekla.pojo.TeamcityQueuedElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jocs
 */
public class TeamcityConnector  {
    public final HttpConnector connector;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("BitbucketTeamcityHook");
        
    public TeamcityConnector(HttpConnector connector) {
     this.connector = connector;     
    }
    
    public String GetQueueData(TeamcityConfiguration conf, Settings settings) throws IOException {
      String restpoint = "/app/rest/buildQueue";
      return this.connector.Get(conf, restpoint, settings);
    }  
    
    public String GetQueueDataForConfiguration(TeamcityConfiguration conf, String buildConfiguration, Settings settings) throws IOException {
      String restpoint = "/app/rest/buildQueue?locator=buildType:" + buildConfiguration;
      return this.connector.Get(conf, restpoint, settings);
    }
    
    public String GetBuildsForBranch(TeamcityConfiguration conf, String branch, String buildConfiguration, Settings settings) throws IOException {
      String restpoint = "/app/rest/builds/" + "?locator=buildType:" + buildConfiguration + 
              ",branch:" + branch + ",running:any,canceled:any,count:2";
      return this.connector.Get(conf, restpoint, settings);
    }
    
    public List<TeamcityQueuedElement> GetQueuedBuilds(TeamcityConfiguration conf, Settings settings) throws IOException, JSONException {
        String queueData = this.GetQueueData(conf, settings);
        JSONObject jsonObj = new JSONObject(queueData);        
        Integer numberOfQueuedBuilds = jsonObj.getInt("count");        
        if(numberOfQueuedBuilds == 0) {
          return new ArrayList<>();
        }
        
        List<TeamcityQueuedElement> queuedElements = new ArrayList<>();
        JSONArray builds = jsonObj.getJSONArray("build");
        for (int i = 0; i < builds.length(); i++) {
            try
            {                           
                JSONObject queued = builds.getJSONObject(i);
                String branchName = "";
                if(queued.has("branchName")){
                  branchName = queued.getString("branchName");
                }

                String buildType = queued.getString("buildTypeId");
                String id = queued.getString("id");
                String webUrl = queued.getString("webUrl");
                TeamcityQueuedElement queuedElement = new TeamcityQueuedElement();
                queuedElement.setBranch(branchName);
                queuedElement.setBuildType(buildType);
                queuedElement.setId(id);
                queuedElement.setWebUrl(webUrl);
                
                queuedElements.add(queuedElement);
                
            } catch (JSONException e) {
            }                                                   
        }
        
        return queuedElements;
    } 
    
    public Boolean IsInQueue(TeamcityConfiguration conf, String buildConfig, String branch, Settings settings) throws IOException, JSONException {
        String queueData = this.GetQueueDataForConfiguration(conf, buildConfig, settings);
        JSONObject jsonObj = new JSONObject(queueData);
        
        Integer numberOfQueuedBuilds = jsonObj.getInt("count");
        
        if(numberOfQueuedBuilds == 0) {
          return false;
        }
        
        JSONArray builds = jsonObj.getJSONArray("build");
        for (int i = 0; i < builds.length(); i++) {
            try
            {                           
                JSONObject queued = builds.getJSONObject(i);
                String branchName = queued.getString("branchName");

                if (branchName.equals(branch)) {
                    return true;
                }                            
            } catch (JSONException e) {
            }                                                   
        }
        
        return false;
    } 
    
    public String TestTeamcityConnection(TeamcityConfiguration conf, Settings settings) {
      String restpoint = "/app/rest/builds";
      
      try
      {
        String data = this.connector.Get(conf, restpoint, settings);
        logger.debug("teamcity returned: "  + data);
        return "Ok";
      } catch (Exception e) {
        logger.debug("Hook Exception: "  + e.getMessage());
        return "Not able to Connect to Teamcity Server : " + e.getMessage();
      }        
    }
    
    public void TriggerCheckForChanges(TeamcityConfiguration conf, String vcsRoot, Settings settings) {
        String url = "/app/rest/debug/vcsCheckingForChangesQueue?locator=vcsRoot:"  + vcsRoot;
        this.connector.Post(conf, url, null, settings);        
    }
    
    public String QueueBuild(
            TeamcityConfiguration conf,
            String branch,
            String buildid,
            String comment,
            Boolean isDefault, Settings settings) {
        String url = "/app/rest/buildQueue";
        return this.connector.PostPayload(conf, url, GetPayload(branch, buildid, comment, isDefault), settings);        
    }    

    private String GetCancelAndRequeuePayload(String readIntoQueue) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("<buildCancelRequest comment=\"requeue build\" readdIntoQueue=\"%s\" />", readIntoQueue));        
        return builder.toString();
    }
    
    private String GetPayload(String branch, String buildid, String comment, Boolean isDefault) {
        StringBuilder builder = new StringBuilder();
        if (!"".equals(branch)) {            
            if (isDefault) {
              builder.append("<build>");
            } else {
              builder.append(String.format("<build branchName=\"%s\">", branch));
            }
          
            builder.append(String.format("<buildType id=\"%s\"/>", buildid));
            builder.append(String.format("<comment><text>%s</text></comment>", comment));
            builder.append("</build>");
        } else {
            builder.append("<build>");
            builder.append(String.format("<buildType id=\"%s\"/>", buildid));
            builder.append(String.format("<comment><text>%s</text></comment>", comment));
            builder.append("</build>");        
        }
        
        return builder.toString();
    }

  public String GetBuild(TeamcityConfiguration conf, String id, Settings settings) throws IOException {
    String url = "/app/rest/builds/id:" + id;
    return this.connector.Get(conf, url, settings);            
  }

  public void ReQueueBuild(TeamcityConfiguration conf, String id, Settings settings, Boolean readIntoQueue) {
    String url = "/app/rest/builds/id:" + id;
    if (readIntoQueue) {
      this.connector.PostPayload(conf, url, this.GetCancelAndRequeuePayload("true"), settings);
    } else {
      this.connector.PostPayload(conf, url, this.GetCancelAndRequeuePayload("false"), settings);
    }
    
  }
}
