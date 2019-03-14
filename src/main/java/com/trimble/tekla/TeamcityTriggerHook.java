package com.trimble.tekla;

import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.NoSuchCommitException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.atlassian.bitbucket.hook.repository.PostRepositoryHook;
import com.atlassian.bitbucket.hook.repository.PostRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookRequest;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.scm.git.GitScm;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.trimble.tekla.helpers.ChangesetService;
import com.trimble.tekla.helpers.ExclusionTriggers;

import com.trimble.tekla.pojo.Trigger;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import com.trimble.tekla.teamcity.TeamcityLogger;

/**
 * Note that hooks can implement RepositorySettingsValidator directly.
 */
public class TeamcityTriggerHook implements PostRepositoryHook<RepositoryHookRequest> {
  private final TeamcityConnector connector;
  private final GitScm gitScm;
  private final CommitService scmService;
  private final TeamcityConnectionSettings connectionSettings;

  @Inject
  public TeamcityTriggerHook(
          @ComponentImport final GitScm gitScm,
          @ComponentImport final CommitService scmService,
          final TeamcityConnectionSettings connectionSettings) {
    this.gitScm = gitScm;
    this.scmService = scmService;
    this.connectionSettings = connectionSettings;
    this.connector = new TeamcityConnector(new HttpConnector());
  }
    
  /**
   * Connects to a configured URL to notify of all changes.
     * @param context
     * @param hookRequest
   */
  @Override
  public void postUpdate(
          @Nonnull final PostRepositoryHookContext context,
          @Nonnull final RepositoryHookRequest hookRequest) {

    final String password = this.connectionSettings.getPassword(hookRequest.getRepository());

    if (password.isEmpty()) {
      TeamcityLogger.logMessage(context, "postReceive: Teamcity secret password not set. Please set password so accounts dont get locked.");
      return;
    }
    
    final String repositoryTriggersJson = context.getSettings().getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
    if (repositoryTriggersJson.isEmpty()) {
      return;
    }
    

    final TeamcityConfiguration conf = new TeamcityConfiguration(
        context.getSettings().getString(Field.TEAMCITY_URL),
        context.getSettings().getString(Field.TEAMCITY_USERNAME),
        password);

    final Repository repository = hookRequest.getRepository();
    final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    final Set<String> uniqueBranches = new LinkedHashSet<>();
    TeamcityLogger.logMessage(context, "postReceive: " + uniqueBranches.size());
    
    for (final RefChange change : hookRequest.getRefChanges()) {
      final String referenceId = change.getRef().getId();               
      if (uniqueBranches.contains(referenceId)) {
        continue;
      }
      
      Trigger[] configurations = GetConfigurations(context, repositoryTriggersJson, referenceId);
      if (configurations.length == 0) {
        continue;
      }
              
      if (change.getType().equals(RefChangeType.DELETE)) {
        TeamcityLogger.logMessage(context, "" + timeStamp + " Skip trigger for delete operation in branch: " + referenceId);
        continue;
      }

      uniqueBranches.add(referenceId);
      
      TeamcityLogger.logMessage(context, "Trigger From Ref: " + referenceId);
      try {
        final boolean isEmptyBranch = isEmptyBranch(context, timeStamp, repository, change);
        final Iterable<String> changedFiles = ChangesetService.GetChangedFiles(scmService, repository, change);     
        for(Trigger configuration : configurations) {         
          if (!ExclusionTriggers.ShouldTriggerOnListOfFiles(configuration.gettriggerInclusion(), configuration.gettriggerExclusion(), changedFiles)) {
            TeamcityLogger.logMessage(context, "Trigger From Ref: " + referenceId + " Excluded: " +  configuration.getTarget());          
            continue;
          }
          TeamcityLogger.logMessage(context, "Trigger From Ref: " + referenceId + " Target: " + configuration.getTarget());
          TriggerBuild(configuration, context, referenceId, conf, timeStamp, isEmptyBranch);
        }        
      } catch (NoSuchCommitException ex) {
        TeamcityLogger.logMessage(context, "No commit Exception: " + ex.getCommitId() + " " + referenceId);
        TeamcityLogger.logMessage(context, "Stacktrace: " + ex.getStackTrace() + " " + referenceId);
      } catch (IOException ex) {
        TeamcityLogger.logMessage(context, "Failed to trigger: " + ex.getMessage() + " " + referenceId);
        TeamcityLogger.logMessage(context, "Stacktrace: " + ex.getStackTrace() + " " + referenceId);
      }
    }
  }

  /**
   * @param context - {@link PostRepositoryHookContext}
   * @param timeStamp - {@link String} - timestamp for tracing logging to system out
   * @param repository - {@link Repository}
   * @param change - {@link RefChange}
   * @return true if specified {@link RefChange change} is a branch and last commit on it belongs to multiple branches
   */
  private boolean isEmptyBranch(final PostRepositoryHookContext context, final String timeStamp, final Repository repository, final RefChange change) {
    boolean isEmptyBranch = false;
    final String fromChange = change.getFromHash();
    if ("0000000000000000000000000000000000000000".equals(fromChange) && StandardRefType.BRANCH == change.getRef().getType()) {

      final String result = this.gitScm.getCommandBuilderFactory().builder(repository)
          .command("branch")
          .argument("--contains")
          .argument(change.getToHash())
          .build(new StringCommandOutputHandler())
          .call();

      TeamcityLogger.logMessage(context, "" + timeStamp + " git branch: --contains " + change.getToHash());
      TeamcityLogger.logMessage(context, "" + timeStamp + " git result: '" + result + "'");

      final String[] branches = result.trim().split("\n");

      if (branches.length > 1) {
        TeamcityLogger.logMessage(context, "" + timeStamp + " Skip trigger no commits in branch: " + change.getRef().getId());
        TeamcityLogger.logMessage(context, "" + timeStamp + " From Hash: " + fromChange);
        TeamcityLogger.logMessage(context, "" + timeStamp + " RefChange Type: " + change.getType());
        isEmptyBranch = true;
      }
    }
    return isEmptyBranch;
  }

  private void TriggerBuild(final Trigger buildConfig, final RepositoryHookContext context, final String refId, final TeamcityConfiguration conf, final String timestamp, final boolean isEmptyBranch) throws IOException {
    if (buildConfig.isTriggerOnPullRequest() || isEmptyBranch && !buildConfig.isTriggerOnEmptyBranches()) {
      TeamcityLogger.logMessage(context, "Skipped <Return>: " + buildConfig.getTarget() + " RefChange Type: " + refId);
      if(buildConfig.isTriggerOnPullRequest()) {
        TeamcityLogger.logMessage(context, "Skipped <buildConfig.isTriggerOnPullRequest() false>: " + buildConfig.getTarget() + " RefChange Type: " + refId);
      }
      if(isEmptyBranch) {
        TeamcityLogger.logMessage(context, "Skipped <isEmptyBranch true>: " + buildConfig.getTarget() + " RefChange Type: " + refId);
      }
      if(buildConfig.isTriggerOnEmptyBranches()) {
        TeamcityLogger.logMessage(context, "Skipped <buildConfig.isTriggerOnEmptyBranches() true>: " + buildConfig.getTarget() + " RefChange Type: " + refId);
      }
      return;
    }

    if (buildConfig.getType().equals("vcs")) {
      TriggerCheckForChanges(context, buildConfig.getTarget(), conf, context.getSettings());
    }

    if (buildConfig.getType().equals("build")) {
      TeamcityLogger.logMessage(context, "Will Try To Que: " + buildConfig.getTarget() + " RefChange Type: " + refId);
      QueueBuild(
          context,
          buildConfig.getTarget(),
          buildConfig.getBranchConfig(),
          buildConfig.isCancelRunningBuilds(),
          conf,
          timestamp,
          false,
          context.getSettings());
    }
  }

  private void TriggerCheckForChanges(final RepositoryHookContext context, final String vcsRoot, final TeamcityConfiguration conf, final Settings settings) {
    try {
      TeamcityLogger.logMessage(context, "Trigger Check for Changes in: " + vcsRoot);
      this.connector.TriggerCheckForChanges(conf, vcsRoot, settings);
    } catch (final Exception e) {
      TeamcityLogger.logMessage(context, "Trigger Check for Changes in: " + vcsRoot + " Failed : " + e.getMessage());
    }
  }

  private void QueueBuild(
      final RepositoryHookContext context,
      final String buildIdIn,
      final String branch,
      final Boolean cancelRunningBuilds,
      final TeamcityConfiguration conf,
      final String timeStamp,
      final Boolean isDefault,
      final Settings settings) {

    final String baseUrl = context.getSettings().getString(Field.BITBUCKET_URL);
    String comment = "remote trigger from bitbucket server : server address not specified in Bitbucket";

    if (!baseUrl.isEmpty()) {
      comment = "remote trigger from bitbucket server : " + baseUrl + "/branches";
    }

    TeamcityLogger.logMessage(context, "" + timeStamp + " Trigger builds for branch: " + branch);
    try {
      TeamcityLogger.logMessage(context, "Trigger BuildId: " + buildIdIn + " " + branch);

      if (!this.connector.IsInQueue(conf, buildIdIn, branch, settings)) {
        // check if build is running
        final String buildData = this.connector.GetBuildsForBranch(conf, branch, buildIdIn, settings);

        final JSONObject obj = new JSONObject(buildData);
        final String count = obj.getString("count");

        if (count.equals("0") || !cancelRunningBuilds) {
          this.connector.QueueBuild(conf, branch, buildIdIn, comment, isDefault, settings);
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
          this.connector.QueueBuild(conf, branch, buildIdIn, comment, isDefault, settings);
          TeamcityLogger.logMessage(context, "Queued: " + buildIdIn + " " + branch);
        }
      } else {
        TeamcityLogger.logMessage(context, "Skip already in queue: " + buildIdIn + " " + branch);
      }
    } catch (final Exception e) {
      TeamcityLogger.logMessage(context, "BuildId: " + buildIdIn + " Failed " + branch);
      TeamcityLogger.logMessage(context, "Error: " + e.getMessage());
    }
  }

  private Trigger[] GetConfigurations(final RepositoryHookContext context, String repositoryTriggersJson, String refBranchId) {
    try {
      return Trigger.GetBuildConfigurationsFromBranch(repositoryTriggersJson, refBranchId);
    } catch (IOException ex) {
      TeamcityLogger.logMessage(context, "postReceive: exception on parsing trigger data: " + ex.getMessage());        
      return new Trigger[0];
    }
  }
}
