package com.trimble.tekla;

import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.repository.*;
import com.atlassian.bitbucket.scm.git.GitScm;
import com.atlassian.bitbucket.setting.*;
import com.trimble.tekla.pojo.Listener;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import com.trimble.tekla.teamcity.TeamcityLogger;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

/**
 * Note that hooks can implement RepositorySettingsValidator directly.
 */
public class TeamcityTriggerHook implements PostRepositoryHook<RepositoryHookRequest> {

  private static final org.slf4j.Logger Logger = org.slf4j.LoggerFactory.getLogger("TeamcityTriggerHook");

  private final TeamcityConnector connector;
  private GitScm gitScm;
  private TeamcityConnectionSettings connectionSettings;

  public TeamcityTriggerHook(final GitScm gitScm, TeamcityConnectionSettings connectionSettings) {
    this.gitScm = gitScm;
    this.connectionSettings = connectionSettings;
    this.connector = new TeamcityConnector(new HttpConnector());
  }

  public TeamcityTriggerHook(TeamcityConnector connector, final GitScm gitScm, TeamcityConnectionSettings connectionSettings) {
    this.connector = connector;
    this.connectionSettings = connectionSettings;
  }

  /**
   * Connects to a configured URL to notify of all changes.
   */
    @Override
    public void postUpdate(@Nonnull PostRepositoryHookContext context, 
                           @Nonnull RepositoryHookRequest hookRequest) {

    String password = this.connectionSettings.getPassword(hookRequest.getRepository());

    if (password.isEmpty()) {
      TeamcityLogger.logMessage(context, "postReceive: Teamcity secret password not set. Please set password so accounts dont get locked.");
      return;
    }

    TeamcityConfiguration conf
            = new TeamcityConfiguration(
                    context.getSettings().getString(Field.TEAMCITY_URL),
                    context.getSettings().getString(Field.TEAMCITY_USERNAME),
                    password);

    final Repository repository = hookRequest.getRepository();
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    Set<String> uniqueBranches = new LinkedHashSet<String>();
    TeamcityLogger.logMessage(context, "postReceive: " + uniqueBranches.size());

    // combine branchs
    for (RefChange change : hookRequest.getRefChanges()) {

      if (uniqueBranches.contains(change.getRef().getId())) {
        continue;
      }

      if (change.getType().equals(RefChangeType.DELETE)) {
        TeamcityLogger.logMessage(context, "" + timeStamp + " Skip trigger for delete operation in branch: " + change.getRef().getId());
        continue;
      }

      String fromChange = change.getFromHash();
      if (fromChange.startsWith("0000000000000000")) {

        String result = gitScm.getCommandBuilderFactory().builder(repository)
                .command("branch")
                .argument("--contains")
                .argument(change.getToHash())
                .build(new StringCommandOutputHandler())
                .call();

        TeamcityLogger.logMessage(context, "" + timeStamp + " git branch: --contains " + change.getToHash());
        TeamcityLogger.logMessage(context, "" + timeStamp + " git result: '" + result + "'");

        String[] branches = result.trim().split("\n");

        if (branches.length > 1) {
          TeamcityLogger.logMessage(context, "" + timeStamp + " Skip trigger no commits in branch: " + change.getRef().getId());
          TeamcityLogger.logMessage(context, "" + timeStamp + " From Hash: " + fromChange);
          TeamcityLogger.logMessage(context, "" + timeStamp + " RefChange Type: " + change.getType());
          continue;
        }
      }

      uniqueBranches.add(change.getRef().getId());
      TeamcityLogger.logMessage(context, "Trigger From Ref: " + change.getRef().getId());
      try {
        this.TriggerBuild(context, change.getRef().getId(), conf, timeStamp);
      } catch (IOException ex) {
        TeamcityLogger.logMessage(context, "Trigger From Ref Failed Excetion: " + ex.getMessage());
      }
    }
  }
    
  private void TriggerBuild(RepositoryHookContext context, String refId, TeamcityConfiguration conf, String timestamp) throws IOException {
    final String repositoryListenersJson = context.getSettings().getString(Field.REPOSITORY_LISTENERS_JSON, StringUtils.EMPTY);
    if (repositoryListenersJson.isEmpty()) {
      return;
    }
    
    final Listener[] configurations = Listener.GetBuildConfigurationsFromBranch(repositoryListenersJson, refId);
    for (final Listener buildConfig : configurations) {
      if(buildConfig.getTriggerOnPullRequest()) {
        continue;
      }
      
      if(buildConfig.getTarget().equals("vcs")) {
        this.TriggerCheckForChanges(context, buildConfig.getTargetId(), conf, context.getSettings());
      }
      
      if(buildConfig.getTarget().equals("build")) {
        // for now we deprecate the isDefault, so that in teamcity we force to use
        // refs/heads/(master) to avoid display default
        this.QueueBuild(context, buildConfig.getTargetId(), buildConfig.getBranchConfig(), conf, timestamp, false, context.getSettings());
      }      
    }
  }

  private void TriggerCheckForChanges(RepositoryHookContext context, String vcsRoot, TeamcityConfiguration conf, Settings settings) {
    try {
      TeamcityLogger.logMessage(context, "Trigger Check for Changes in: " + vcsRoot);
      this.connector.TriggerCheckForChanges(conf, vcsRoot, settings);
    } catch (Exception e) {
      TeamcityLogger.logMessage(context, "Trigger Check for Changes in: " + vcsRoot + " Failed : " + e.getMessage());
    }
  }

  private void QueueBuild(
          RepositoryHookContext context,
          String buildIdIn,
          String branch,
          TeamcityConfiguration conf,
          String timeStamp,
          Boolean isDefault,
          Settings settings) {

    String baseUrl = context.getSettings().getString(Field.BITBUCKET_URL);
    String comment = "remote trigger from bitbucket server : server address not specified in Bitbucket";

    if (!baseUrl.isEmpty()) {
      comment = "remote trigger from bitbucket server : " + baseUrl + "/branches";
    }

    TeamcityLogger.logMessage(context, "" + timeStamp + " Trigger builds for branch: " + branch);
    try {
      TeamcityLogger.logMessage(context, "Trigger BuildId: " + buildIdIn);

      if (!this.connector.IsInQueue(conf, buildIdIn, branch, settings)) {
        this.connector.QueueBuild(conf, branch, buildIdIn, comment, isDefault, settings);
      } else {
        TeamcityLogger.logMessage(context, "Skip already in queue: " + buildIdIn);
      }
    } catch (Exception e) {
      TeamcityLogger.logMessage(context, "BuildId: " + buildIdIn + " Failed");
      TeamcityLogger.logMessage(context, "Error: " + e.getMessage());
    }    
  }
}
