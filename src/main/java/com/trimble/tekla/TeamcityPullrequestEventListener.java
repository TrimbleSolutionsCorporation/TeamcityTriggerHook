/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla;

import com.trimble.tekla.helpers.ExclusionTriggers;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestParticipantsUpdatedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.event.api.EventListener;
import com.trimble.tekla.helpers.ChangesetService;
import com.trimble.tekla.pojo.Trigger;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import com.trimble.tekla.teamcity.TeamcityLogger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Named
public class TeamcityPullrequestEventListener {

  private final TeamcityConnectionSettings connectionSettings;
  private final SettingsService settingsService;
  private final TeamcityConnector connector;
  private final PullRequestService pullRequestService;

  @Inject
  public TeamcityPullrequestEventListener(
          final TeamcityConnectionSettings connectionSettings,
          final SettingsService settingsService,
          final PullRequestService pullRequestService) {
    this.connectionSettings = connectionSettings;
    this.settingsService = settingsService;
    this.pullRequestService = pullRequestService;
    this.connector = new TeamcityConnector(new HttpConnector());
  }

  @EventListener
  public void onPullRequestOpenedEvent(final PullRequestOpenedEvent event) throws IOException, JSONException {
    final PullRequest pr = event.getPullRequest();
    final Repository repo = pr.getFromRef().getRepository();
    final Settings settings = this.settingsService.getSettings(repo).get();
    
    try {
      TriggerBuildFromPullRequest(pr, false);
    } catch (final IOException | JSONException ex) {
      TeamcityLogger.logMessage(settings, "PullRequest Opened Event Failed: " + ex.getMessage());
    }
  }

  @EventListener
  public void onPullRequestParticipantsUpdatedEvent(final PullRequestParticipantsUpdatedEvent event) throws IOException, JSONException {
    final PullRequest pr = event.getPullRequest();
    final Set<PullRequestParticipant> reviewers = pr.getReviewers();
    final Repository repo = pr.getFromRef().getRepository();
    final Settings settings = this.settingsService.getSettings(repo).get();
    if (event.getAddedParticipants().size() > 0 && reviewers.size() > 0) {
      // trigger only when number of participations is 2 or higher (author + reviewer)
      try {
        TriggerBuildFromPullRequest(event.getPullRequest(), true);
      } catch (final IOException | JSONException ex) {
        TeamcityLogger.logMessage(settings, "PullRequest Reviwer update event failed: " + ex.getMessage());
      }
    }
  }

  @EventListener
  public void onPullRequestRescoped(final PullRequestRescopedEvent event) throws IOException, JSONException {
    final String previousFromHash = event.getPreviousFromHash();
    final String currentFromHash = event.getPullRequest().getFromRef().getLatestCommit();

    if (currentFromHash.equals(previousFromHash)) {
      return;
    }

    final PullRequest pr = event.getPullRequest();
    final Repository repo = pr.getFromRef().getRepository();
    final Settings settings = this.settingsService.getSettings(repo).get();


    try {
      TeamcityLogger.logMessage(settings, "Run PullRequest Rescoped Event : " + pr.getFromRef().getDisplayId());
      TriggerBuildFromPullRequest(pr, false);
    } catch (final IOException | JSONException ex) {
      TeamcityLogger.logMessage(settings, "PullRequest Rescoped Event Failed: " + ex.getMessage() + " " + pr.getFromRef().getDisplayId());
    }
  }

  private void TriggerBuildFromPullRequest(final PullRequest pr, Boolean UpdatedReviewers) throws IOException, JSONException {
    final Repository repo = pr.getFromRef().getRepository();
    final Settings settings = this.settingsService.getSettings(repo).get();
    final String password = this.connectionSettings.getPassword(pr.getFromRef().getRepository());
    // has one reviewer
    final Boolean areParticipants = !pr.getReviewers().isEmpty();
    final TeamcityConfiguration conf
            = new TeamcityConfiguration(
                    settings.getString("teamCityUrl"),
                    settings.getString("teamCityUserName"),
                    password);

    final String branch = pr.getFromRef().getId();
    final String repositoryTriggersJson = settings.getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
    if (repositoryTriggersJson.isEmpty()) {
      return;
    }

    Set triggeredBuilds = new HashSet();

    final ArrayList<String> changes = ChangesetService.GetChangedFiles(pr, pullRequestService);
    final Trigger[] configurations = Trigger.GetBuildConfigurationsFromBranch(repositoryTriggersJson, branch);
    for (final Trigger buildConfig : configurations) {
      TeamcityLogger.logMessage(settings, "Try Trigger: " + buildConfig.getBranchConfig() + " " + branch + " : " + buildConfig.getTarget());
      if (buildConfig.isTriggerOnPullRequest()) {
        if (UpdatedReviewers && buildConfig.isTriggerWhenNoReviewers()) {
          TeamcityLogger.logMessage(settings, "Skip For Update Reviewers: " + buildConfig.getBranchConfig() + " " + branch);
          // we dont want to build when reviewers were updated and we allow to trigger when no reviwers are defined
          continue;
        }

        if (!areParticipants && !buildConfig.isTriggerWhenNoReviewers()) {
          // we dont want to build if there are no participants
          TeamcityLogger.logMessage(settings, "Skip: " + buildConfig.getBranchConfig() + " " + branch);
          continue;
        }

        if (triggeredBuilds.contains(buildConfig.getTarget())) {
          continue;
        }

        if (!ExclusionTriggers.ShouldTriggerOnListOfFiles(buildConfig.gettriggerInclusion(), buildConfig.gettriggerExclusion(), changes)) {
          continue;
        }

        TeamcityLogger.logMessage(settings, "Trigger BuildId: " + buildConfig.getTarget() + " " + branch);
        try {
          if (this.connector.IsInQueue(conf, buildConfig.getTarget(), buildConfig.getBranchConfig(), settings)) {
            TeamcityLogger.logMessage(settings, "Skip already in queue: " + buildConfig.getTarget()+ " " + branch);
            continue;
          }
        } catch (IOException | JSONException ex) {
          TeamcityLogger.logMessage(settings, "Exception: " + ex.getMessage() + " " + branch);
        }

        // check if build is running
        final String buildData = this.connector.GetBuildsForBranch(
                conf,
                buildConfig.getBranchConfig(),
                buildConfig.getTarget(),
                settings);

        final JSONObject obj = new JSONObject(buildData);
        final String count = obj.getString("count");

        if (count.equals("0") || !buildConfig.isCancelRunningBuilds()) {
          this.connector.QueueBuild(
                  conf,
                  buildConfig.getBranchConfig(),
                  buildConfig.getTarget(),
                  "Trigger from Bitbucket: Pull Request: " + pr.getId(),
                  false,
                  settings);
          triggeredBuilds.add(buildConfig.getTarget());
        } else {
          final JSONArray builds = obj.getJSONArray("build");
          for (int i = 0; i < builds.length(); i++) {
            final Boolean isRunning = builds.getJSONObject(i).getString("state").equals("running");
            if (isRunning) {
              final String id = builds.getJSONObject(i).getString("id");
              this.connector.ReQueueBuild(conf, id, settings, false);
            }
          }

          // at this point all builds were finished, so we need to trigger
          this.connector.QueueBuild(
                  conf,
                  buildConfig.getBranchConfig(),
                  buildConfig.getTarget(),
                  "Trigger from Bitbucket: Pull Request: " + pr.getId(),
                  false,
                  settings);
        }
      }
    }
  }
}
