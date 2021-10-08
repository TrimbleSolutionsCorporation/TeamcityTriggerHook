/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.teamcity;

import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.setting.Settings;
import com.trimble.tekla.pojo.TeamcityQueuedElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jocs
 */
public class TeamcityConnector  {
    public final HttpConnector connector;
        
    public TeamcityConnector(HttpConnector connector) {
     this.connector = connector;     
    }
    
    public String GetQueueAndRunningData(TeamcityConfiguration conf, Settings settings, String branch, String repoName) throws IOException {
      String restpoint = "/app/rest/builds?locator=state:(queued:true,running:true),branch:" + branch;     
      String returnData = this.connector.Get(conf, restpoint, settings, repoName);
      TeamcityLogger.logMessage(settings, repoName, "[HttpConnector][GetQueueData] return data: " + returnData);      
      return returnData;
    }  
    
    public String GetQueueDataForConfiguration(TeamcityConfiguration conf,
                                               String buildConfiguration,
                                               Settings settings,
                                               String repoName) throws IOException {
      String restpoint = "/app/rest/buildQueue?locator=buildType:" + buildConfiguration;
      return this.connector.Get(conf, restpoint, settings, repoName);
    }
    
    public String GetBuildsForBranch(TeamcityConfiguration conf,
                                     String branch,
                                     String buildConfiguration,
                                     Settings settings,
                                     String repoName,
                                     Boolean includeQueued) throws IOException {
      String restpoint = "/app/rest/builds/" + "?locator=buildType:" + buildConfiguration + 
              ",branch:" + branch + ",canceled:any,state:(queued:" + includeQueued + ",running:true),count:2";
      return this.connector.Get(conf, restpoint, settings, repoName);
    }

    public String GetDependentBuilds(TeamcityConfiguration conf,
                                       String id,
                                       Settings settings,
                                       String repoName,
                                       Boolean includeInitial) throws IOException {
      String restpoint = "/app/rest/builds/" + "?locator=snapshotDependency:(from:(id:" + id + 
              "),recursive:true,includeInitial:" + includeInitial + "),state:(queued:true,running:true)";
      return this.connector.Get(conf, restpoint, settings, repoName);
    }

    public String GetBuildDependencies(TeamcityConfiguration conf,
                                       String id,
                                       Settings settings,
                                       String repoName,
                                       Boolean includeInitial) throws IOException {
      String restpoint = "/app/rest/builds/" + "?locator=snapshotDependency:(to:(id:" + id + 
              "),recursive:true,includeInitial:" + includeInitial + "),state:(queued:true,running:true)";
      return this.connector.Get(conf, restpoint, settings, repoName);
    }
    
    public List<TeamcityQueuedElement> GetQueuedAndRunningBuilds(TeamcityConfiguration conf,
                                                                 Settings settings,
                                                                 String branch,
                                                                 String repoName) throws IOException, JSONException {
        String queueData = this.GetQueueAndRunningData(conf, settings, branch, repoName);
        List<TeamcityQueuedElement> queuedElements = new ArrayList<>();
        if(queueData == ""){
          return queuedElements;
        }
        JSONObject jsonObj = new JSONObject(queueData);        
        Integer numberOfQueuedBuilds = jsonObj.getInt("count");        
        if(numberOfQueuedBuilds == 0) {
          return new ArrayList<>();
        }
        
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

    public Boolean IsInQueue(TeamcityConfiguration conf, String buildConfig, String branch, String revision, Settings settings, String repoName) throws IOException, JSONException {
        String restpoint = String.format("/app/rest/builds/?locator=buildType:%s,branch:%s,revision(version:%s),state:(queued:true,running:false)", buildConfig , branch, revision);
        String queueData = this.connector.Get(conf, restpoint, settings, repoName);
      
        JSONObject jsonObj = new JSONObject(queueData);
        return jsonObj.getInt("count") > 0;
    } 
    
    public String TestTeamcityConnection(TeamcityConfiguration conf, Settings settings, String repoName) {
      String restpoint = "/app/rest/builds";
      
      try
      {
        String data = this.connector.Get(conf, restpoint, settings, repoName);
        TeamcityLogger.logMessage(settings, repoName, "teamcity returned: "  + data);
        return "Ok";
      } catch (Exception e) {
        TeamcityLogger.logMessage(settings, repoName, "Hook Exception: "  + e.getMessage());
        return "Not able to Connect to Teamcity Server : " + e.getMessage();
      }        
    }
    
    public void TriggerCheckForChanges(TeamcityConfiguration conf, String vcsRoot, Settings settings, String repoName) {
        String url = "/app/rest/debug/vcsCheckingForChangesQueue?locator=vcsRoot:"  + vcsRoot;
        this.connector.Post(conf, url, null, settings, repoName);        
    }
    
    public String QueueBuild(
            TeamcityConfiguration conf,
            String branch,
            String buildid,
            String comment,
            Boolean isDefault,
            Settings settings,
            String repoName,
            PullRequest prInfo) {
        String url = "/app/rest/buildQueue";
        return this.connector.PostPayload(conf, url, GetPayload(branch, buildid, comment, isDefault, prInfo), settings, repoName);
    }

    private String GetCancelAndRequeuePayload(String readIntoQueue) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("<buildCancelRequest comment=\"requeue build\" readdIntoQueue=\"%s\" />", readIntoQueue));        
        return builder.toString();
    }
    
    private String GetPayload(String branch, String buildid, String comment, Boolean isDefault, PullRequest prInfo) {
        StringBuilder builder = new StringBuilder();
        if (!"".equals(branch)) {            
            if (isDefault) {
              builder.append("<build>");
            } else {
              builder.append(String.format("<build branchName=\"%s\">", branch));
            }
          
            builder.append(String.format("<buildType id=\"%s\"/>", buildid));
            builder.append(String.format("<comment><text>%s</text></comment>", comment));
            if (prInfo != null) {
                builder.append("<properties>");
                builder.append(String.format("<property name=\"bitbucket.pr.source.branch\" value=\"%s\"/>", branch));
                builder.append(String.format("<property name=\"bitbucket.pr.target.branch\" value=\"%s\"/>", prInfo.getToRef().getId()));
                builder.append(String.format("<property name=\"bitbucket.pr.number\" value=\"%s\"/>", prInfo.getId()));
                builder.append(String.format("<property name=\"bitbucket.pr.title\" value=\"%s\"/>", prInfo.getTitle()));
                builder.append("</properties>");
            }
            builder.append("</build>");
        } else {
            builder.append("<build>");
            builder.append(String.format("<buildType id=\"%s\"/>", buildid));
            builder.append(String.format("<comment><text>%s</text></comment>", comment));
            builder.append("</build>");        
        }
        
        return builder.toString();
    }

  public String GetBuild(TeamcityConfiguration conf, String id, Settings settings, String repoName) throws IOException {
    String url = "/app/rest/builds/id:" + id;
    return this.connector.Get(conf, url, settings, repoName);
  }

  public void ReQueueBuild(TeamcityConfiguration conf, String id, Settings settings, Boolean readIntoQueue, String repoName) {
    String url = "/app/rest/builds/id:" + id;
    if (readIntoQueue) {
      this.connector.PostPayload(conf, url, this.GetCancelAndRequeuePayload("true"), settings, repoName);
    } else {
      this.connector.PostPayload(conf, url, this.GetCancelAndRequeuePayload("false"), settings, repoName);
    }
    
  }

  public void CancelDependenciesOfBuild(TeamcityConfiguration conf, String id, Settings settings, String repoName) throws IOException, JSONException{
    final String dependenciesData = this.GetBuildDependencies(conf, id, settings, repoName, true);
    Set<Integer> targetDependenciesIds = BuildIdsSetFromResponse(dependenciesData);

    Set<Integer> cancelableDependencies = new HashSet<Integer>();
    for (Integer buildId : targetDependenciesIds) {
      // Get a set of all builds that depend on this
      final String dependentBuildsData = this.GetDependentBuilds(conf, Integer.toString(buildId), settings, repoName, true);
      Set<Integer> dependentBuildIds = BuildIdsSetFromResponse(dependentBuildsData);

      // Remove all the dependencies of the target build
      // If any build remain in the set, this mean another build chain is using this as a dependency
      dependentBuildIds.removeAll(targetDependenciesIds);
      if (dependentBuildIds.isEmpty()) {
        cancelableDependencies.add(buildId);
      }
    }

    for (Integer buildId : cancelableDependencies) {
        this.ReQueueBuild(conf, Integer.toString(buildId), settings, false, repoName);
    }
  }

  private Set<Integer> BuildIdsSetFromResponse(String buildsReponse) throws JSONException {
    Set<Integer> buildIdsSet = new HashSet<Integer>();
    final JSONObject buildsObj = new JSONObject(buildsReponse);
    final JSONArray builds = buildsObj.getJSONArray("build");

    for (int i = 0; i < builds.length(); i++) {
      buildIdsSet.add(builds.getJSONObject(i).getInt("id"));
    }
    return buildIdsSet;
  }
}
