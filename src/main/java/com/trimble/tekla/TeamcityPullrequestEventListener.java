/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla;

import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReopenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestUpdatedEvent;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.event.api.EventListener;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;

/**
 *
 * @author jocs
 */
public class TeamcityPullrequestEventListener {

  private final TeamcityConnectionSettings connectionSettings;
  private final SettingsService settingsService;
  private final TeamcityConnector connector;
  public TeamcityPullrequestEventListener(TeamcityConnectionSettings connectionSettings, SettingsService settingsService) {
    this.connectionSettings = connectionSettings;
    this.settingsService = settingsService;
    this.connector = new TeamcityConnector(new HttpConnector());
  }

  
  @EventListener
  public void onPullRequestOpenedEvent(PullRequestOpenedEvent event) {
    PullRequestRef ref = event.getPullRequest().getFromRef();
    
    if (ref.getDisplayId().toLowerCase().contains("bugfix/")) {
      TriggerBuildFromPullRequest(ref, "usePrFrombugFix", "bugFixRule");
    }
    
    if (ref.getDisplayId().toLowerCase().contains("feature/")) {
      TriggerBuildFromPullRequest(ref, "usePrFromFeature", "featureRule");
    }

    if (ref.getDisplayId().toLowerCase().contains("hotfix/")) {
      TriggerBuildFromPullRequest(ref, "usePrFromhotFix", "hotFixRule");
    }        
  }

  @EventListener
  public void onPullRequestRescoped(PullRequestRescopedEvent event) {
    PullRequestRef ref = event.getPullRequest().getFromRef();
    String previousFromHash = event.getPreviousFromHash();
    String currentFromHash = ref.getLatestCommit();
    
    if (currentFromHash.equals(previousFromHash)) {
      return;
    }
    
    if (ref.getDisplayId().toLowerCase().contains("bugfix/")) {
      TriggerBuildFromPullRequest(ref, "usePrFrombugFix", "bugFixRule");
    }
    
    if (ref.getDisplayId().toLowerCase().contains("feature/")) {
      TriggerBuildFromPullRequest(ref, "usePrFromFeature", "featureRule");
    }

    if (ref.getDisplayId().toLowerCase().contains("hotfix/")) {
      TriggerBuildFromPullRequest(ref, "usePrFromhotFix", "hotFixRule");
    }
  }

  private void TriggerBuildFromPullRequest(PullRequestRef ref, String key, String buildKey) {
    Repository repo = ref.getRepository();
    Settings settings = settingsService.getSettings(repo);
    Boolean isTriggerOnPr = settings.getBoolean(key, false);
    if (!isTriggerOnPr) {
      return;
    }
    
    String password = this.connectionSettings.getPassword(ref.getRepository());    
    TeamcityConfiguration conf = 
      new TeamcityConfiguration(
        settings.getString("TeamCityUrl"),
        settings.getString("TeamCityUserName"),
        password);
    
    String buildIdIn = settings.getString(buildKey, "");    
    String branch = ref.getDisplayId().toLowerCase();
    String branchtoLower = branch.toLowerCase();
    for(String buildId : buildIdIn.split("\\s+")) {
      if (branchtoLower.startsWith("feature/") || branchtoLower.startsWith("bugfix/") || branchtoLower.startsWith("hotfix/")) {
        this.connector.QueueBuild(conf, branch.split("/")[1], buildId, "Pull request Trigger from Bitbucket", false);
      } else {
        this.connector.QueueBuild(conf, branch, buildId, "Pull request Trigger from Bitbucket", false);
      }    
    }
  }
}
