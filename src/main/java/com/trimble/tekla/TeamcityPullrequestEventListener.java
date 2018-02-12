/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla;

import com.trimble.tekla.teamcity.TeamcityLogger;
import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.event.api.EventListener;
import com.trimble.tekla.pojo.Listener;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jocs
 */
public class TeamcityPullrequestEventListener {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("TeamcityTriggerHook");
  private final TeamcityConnectionSettings connectionSettings;
  private final SettingsService settingsService;
  private final TeamcityConnector connector;

  public TeamcityPullrequestEventListener(TeamcityConnectionSettings connectionSettings, SettingsService settingsService) {
    this.connectionSettings = connectionSettings;
    this.settingsService = settingsService;
    this.connector = new TeamcityConnector(new HttpConnector());
  }

  @EventListener
  public void onPullRequestOpenedEvent(PullRequestOpenedEvent event) throws IOException, JSONException {
    PullRequestRef ref = event.getPullRequest().getFromRef();
    TriggerBuildFromPullRequest(ref);
  }

  @EventListener
  public void onPullRequestRescoped(PullRequestRescopedEvent event) throws IOException, JSONException {
    PullRequestRef ref = event.getPullRequest().getFromRef();
    String previousFromHash = event.getPreviousFromHash();
    String currentFromHash = ref.getLatestCommit();

    if (currentFromHash.equals(previousFromHash)) {
      return;
    }

    TriggerBuildFromPullRequest(ref);
  }

  private void TriggerBuildFromPullRequest(PullRequestRef ref) throws IOException, JSONException {
    Repository repo = ref.getRepository();
    Settings settings = settingsService.getSettings(repo);
    String password = this.connectionSettings.getPassword(ref.getRepository());
    TeamcityConfiguration conf
            = new TeamcityConfiguration(
                    settings.getString("teamCityUrl"),
                    settings.getString("teamCityUserName"),
                    password);

    String branch = ref.getDisplayId();
    final String repositoryListenersJson = settings.getString(Field.REPOSITORY_LISTENERS_JSON, StringUtils.EMPTY);
    if (repositoryListenersJson.isEmpty()) {
      return;
    }

    final Listener[] configurations = Listener.GetBuildConfigurationsFromBranch(repositoryListenersJson, branch);
    for (final Listener buildConfig : configurations) {
      if (buildConfig.getTriggerOnPullRequest()) {
        TeamcityLogger.logMessage(settings, "Trigger BuildId: " + buildConfig.getTargetId());
        try {
          if (this.connector.IsInQueue(conf, buildConfig.getTargetId(), buildConfig.getBranchConfig(), settings)) {
            TeamcityLogger.logMessage(settings, "Skip already in queue: " + buildConfig.getTargetId());
            continue;
          }
        } catch (IOException | JSONException ex) {
          TeamcityLogger.logMessage(settings, "Exception: " + ex.getMessage());
        }

        // check if build is running
        String buildData = this.connector.GetBuildsForBranch(
                conf,
                buildConfig.getBranchConfig(),
                buildConfig.getTargetId(),
                settings);

        JSONObject obj = new JSONObject(buildData);
        String count = obj.getString("count");

        if (count.equals("0") || !buildConfig.getCancelRunningBuilds()) {
          this.connector.QueueBuild(
                  conf,
                  buildConfig.getBranchConfig(),
                  buildConfig.getTargetId(),
                  "Pull request Trigger from Bitbucket",
                  false,
                  settings);
        } else {
          JSONArray builds = obj.getJSONArray("build");
          Boolean flipRequeue = true;
          for (int i = 0; i < builds.length(); i++) {
            Boolean isRunning = builds.getJSONObject(i).getString("state").equals("running");
            if (isRunning) {
              String id = builds.getJSONObject(i).getString("id");
              this.connector.ReQueueBuild(conf, id, settings, flipRequeue);
              flipRequeue = false;
            }
          }
          
          if(flipRequeue) {
            // at this point all builds were finished, so we need to trigger
            this.connector.QueueBuild(
                    conf,
                    buildConfig.getBranchConfig(),
                    buildConfig.getTargetId(),
                    "Pull request Trigger from Bitbucket",
                    false,
                    settings);            
          }
        }
      }
    }
  }
}
