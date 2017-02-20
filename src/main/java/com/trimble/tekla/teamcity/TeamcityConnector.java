/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.teamcity;

import java.io.IOException;

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
    
    public String GetQueueDataForConfiguration(TeamcityConfiguration conf, String buildConfiguration) throws IOException {
      String restpoint = "/app/rest/buildQueue?locator=buildType:" + buildConfiguration;
      return this.connector.Get(conf, restpoint);
    }
    
    public String GetBuildsForBranch(TeamcityConfiguration conf, String branch, String buildConfiguration) throws IOException {
      String restpoint = "/app/rest/builds/" + "?locator=buildType:" + buildConfiguration + 
              ",branch:" + branch + ",running:any,canceled:any,count:2";
      return this.connector.Get(conf, restpoint);
    }
    
    public String TestTeamcityConnection(TeamcityConfiguration conf) {
      String restpoint = "/app/rest/builds";
      
      try
      {
        String data = this.connector.Get(conf, restpoint);
        logger.debug("teamcity returned: "  + data);
        return "Ok";
      } catch (Exception e) {
        logger.debug("Hook Exception: "  + e.getMessage());
        e.printStackTrace();
        return "Not able to Connect to Teamcity Server : " + e.getMessage();
      }        
    }
    
    public void TriggerCheckForChanges(TeamcityConfiguration conf, String vcsRoot) {
        String url = "/app/rest/debug/vcsCheckingForChangesQueue?locator=vcsRoot:"  + vcsRoot;
        this.connector.Post(conf, url, null);        
    }
    
    public void QueueBuild(
            TeamcityConfiguration conf,
            String branch,
            String buildid,
            String comment,
            Boolean isDefault) {
        String url = "/app/rest/buildQueue";
        this.connector.PostPayload(conf, url, GetPayload(branch, buildid, comment, isDefault));        
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

  public String GetBuild(TeamcityConfiguration conf, String id) throws IOException {
    String url = "/app/rest/builds/id:" + id;
    return this.connector.Get(conf, url);            
  }
}
