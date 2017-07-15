package com.trimble.tekla;

import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.repository.*;
import com.atlassian.bitbucket.scm.git.GitScm;
import com.atlassian.bitbucket.setting.*;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import com.trimble.tekla.teamcity.TeamcityLogger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Note that hooks can implement RepositorySettingsValidator directly.
 */
public class TeamcityTriggerHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("TeamcityTriggerHook");

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
  public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
    boolean useQueue = context.getSettings().getString("triggerType").equals("vcs");
    String password = this.connectionSettings.getPassword(context.getRepository());

    if (password.isEmpty()) {
      TeamcityLogger.logMessage(context, "postReceive: Teamcity secret password not set. Please set password so accounts dont get locked.");
      return;
    }

    TeamcityConfiguration conf
            = new TeamcityConfiguration(
                    context.getSettings().getString("TeamCityUrl"),
                    context.getSettings().getString("TeamCityUserName"),
                    password);

    final Repository repository = context.getRepository();
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    Set<String> uniqueBranches = new LinkedHashSet<String>();
    TeamcityLogger.logMessage(context, "postReceive: " + uniqueBranches.size());

    // combine branchs
    for (RefChange change : refChanges) {

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
      this.TriggerChangesFetch(context, change.getRef().getId(), conf, useQueue, timeStamp);
    }
  }

  @Override
  public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
    boolean areAutenticationSettingsSet = true;
    if (settings.getString("TeamCityUrl", "").isEmpty()) {
      errors.addFieldError("TeamCityUrl", "Url field is blank, please provide teamcity server address and port");
      areAutenticationSettingsSet = false;
    }

    if (settings.getString("TeamCityUserName", "").isEmpty()) {
      errors.addFieldError("TeamCityUserName", "Username needs to be defined");
      areAutenticationSettingsSet = false;
    }

    String teamCityPasswordOk = settings.getString("TeamCityPasswordOk", "");
    if (teamCityPasswordOk.isEmpty()) {
      errors.addFieldError("TeamCityPasswordOk", "Test and Save Connection needs to be clicked to validate user credentials");
      areAutenticationSettingsSet = false;
    } else {
      if (!teamCityPasswordOk.equals("OkPassword")) {
        errors.addFieldError("TeamCityPasswordOk", "Credentials are incorrect.");
        areAutenticationSettingsSet = false;
      }
    }

    if (!areAutenticationSettingsSet) {
      return;
    }

    if (settings.getString("masterRule", "").isEmpty()
            && settings.getString("bugFixRule", "").isEmpty()
            && settings.getString("featureRule", "").isEmpty()
            && settings.getString("hotfixRule", "").isEmpty()) {
      errors.addFieldError("masterRule", "At least on configuration should be set");
      errors.addFieldError("bugFixRule", "At least on configuration should be set");
      errors.addFieldError("hotfixRule", "At least on configuration should be set");
      errors.addFieldError("featureRule", "At least on configuration should be set");
    }

    if (!settings.getString("ExternalBuildsOneNameIdFeature", "").isEmpty()) {
      if (settings.getString("ExternalBuildsOneDepIdFeature", "").isEmpty()) {
        errors.addFieldError("ExternalBuildsOneDepIdFeature", "At least one dependency should be set");
      }
      String configuration = settings.getString("ExternalBuildsOneConfigurationsIdFeature", "");
      if (configuration.isEmpty()) {
        errors.addFieldError("ExternalBuildsOneConfigurationsIdFeature", "At least one configuration should be set");
      }

      if (settings.getString("featureRule", "").isEmpty()) {
        errors.addFieldError("ExternalBuildsOneInvalidDepFeature", "External builds have been set, a rule for feature branch needs to be set in the build configuration rules.");
      }
    }

    if (!settings.getString("ExternalBuildsOneNameIdBugFix", "").isEmpty()) {
      if (settings.getString("ExternalBuildsOneDepIdBugFix", "").isEmpty()) {
        errors.addFieldError("ExternalBuildsOneDepIdBugFix", "At least one dependency should be set");
      }
      String configuration = settings.getString("ExternalBuildsOneConfigurationsIdBugFix", "");
      if (configuration.isEmpty()) {
        errors.addFieldError("ExternalBuildsOneConfigurationsIdBugFix", "At least one configuration should be set");
      }

      if (settings.getString("bugFixRule", "").isEmpty()) {
        errors.addFieldError("ExternalBuildsOneInvalidDepBugFix", "External builds have been set, a rule for bugfix branch needs to be set in the build configuration rules.");
      }
    }

    if (!settings.getString("ExternalBuildsOneNameIdHotFix", "").isEmpty()) {
      if (settings.getString("ExternalBuildsOneDepIdHotFix", "").isEmpty()) {
        errors.addFieldError("ExternalBuildsOneDepIdHotFix", "At least one dependency should be set");
      }
      String configuration = settings.getString("ExternalBuildsOneConfigurationsIdHotFix", "");
      if (configuration.isEmpty()) {
        errors.addFieldError("ExternalBuildsOneConfigurationsIdHotFix", "At least one configuration should be set");
      }

      if (settings.getString("hotfixRule", "").isEmpty()) {
        errors.addFieldError("ExternalBuildsOneInvalidDepHotFix", "External builds have been set, a rule for hotfix branch needs to be set in the build configuration rules.");
      }
    }

    if (!settings.getString("ExternalBuildsTwoNameId", "").isEmpty()) {
      if (settings.getString("ExternalBuildsTwoDepId", "").isEmpty()) {
        errors.addFieldError("ExternalBuildsTwoDepId", "At least one dependency should be set");
      }
      String configuration = settings.getString("ExternalHooksConfigurationV2", "");
      if (configuration.isEmpty() || configuration.equals("[]") || configuration.equals("{}")) {
        errors.addFieldError("ExternalHooksConfigurationV2", "At least one hook should be set for  external hooks");
      }
    }
  }

  private void TriggerChangesFetch(RepositoryHookContext context, String refId, TeamcityConfiguration conf, boolean useQueue, String timestamp) {

    String masterVcsRoot = context.getSettings().getString("masterRule");
    if (!masterVcsRoot.isEmpty() && refId.toLowerCase().equals("refs/heads/master")) {
      if (useQueue == false) {
        boolean isDefault = context.getSettings().getBoolean("isDefaultBranch");
        this.QueueBuild(context, masterVcsRoot, "master", conf, timestamp, isDefault, context.getSettings());
      } else {
        this.TriggerWithDefinition(context, masterVcsRoot, conf, context.getSettings());
      }
    }

    String featureVcsRoot = context.getSettings().getString("featureRule");
    if (!featureVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/feature")) {
      Boolean isTriggerOnPr = context.getSettings().getBoolean("usePrFromFeature", false);
      if (isTriggerOnPr) {
        return;
      }
      if (useQueue == false) {
        this.QueueBuild(context, featureVcsRoot, refId.split("/")[3], conf, timestamp, false, context.getSettings());
      } else {
        this.TriggerWithDefinition(context, featureVcsRoot, conf, context.getSettings());
      }
    }

    String bugfixVcsRoot = context.getSettings().getString("bugFixRule");
    if (!bugfixVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/bugfix")) {
      Boolean isTriggerOnPr = context.getSettings().getBoolean("usePrFrombugFix", false);
      if (isTriggerOnPr) {
        return;
      }
      if (useQueue == false) {
        this.QueueBuild(context, bugfixVcsRoot, refId.split("/")[3], conf, timestamp, false, context.getSettings());
      } else {
        this.TriggerWithDefinition(context, bugfixVcsRoot, conf, context.getSettings());
      }
    }

    String hotfixVcsRoot = context.getSettings().getString("hotfixRule");
    if (!hotfixVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/hotfix")) {
      Boolean isTriggerOnPr = context.getSettings().getBoolean("usePrFromhotFix", false);
      if (isTriggerOnPr) {
        return;
      }
      if (useQueue == false) {
        this.QueueBuild(context, hotfixVcsRoot, refId.split("/")[3], conf, timestamp, false, context.getSettings());
      } else {
        this.TriggerWithDefinition(context, hotfixVcsRoot, conf, context.getSettings());
      }
    }

    String releaseVcsRoot = context.getSettings().getString("releaseRule");
    if (!releaseVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/release")) {
      if (useQueue == false) {
        this.QueueBuild(context, releaseVcsRoot, refId.split("/")[3], conf, timestamp, false, context.getSettings());
      } else {
        this.TriggerWithDefinition(context, releaseVcsRoot, conf, context.getSettings());
      }
    }

    String branchDefinition = context.getSettings().getString("BranchDefinition");
    TeamcityLogger.logMessage(context, "Regx Branch Definition: " + branchDefinition);
    if (!branchDefinition.isEmpty() && this.ValidateRegx(refId, branchDefinition)) {
      TeamcityLogger.logMessage(context, "Regx Validated");
      if (useQueue == false) {
        String[] elemets = refId.split("/");
        this.QueueBuild(context, context.getSettings().getString("BranchCustomTypes"), elemets[elemets.length - 1], conf, timestamp, false, context.getSettings());
      } else {
        this.TriggerWithDefinition(context, context.getSettings().getString("BranchCustomTypes"), conf, context.getSettings());
      }
    }
  }

  public boolean ValidateRegx(String data, String regx) {
    if (data.toLowerCase().equals("refs/heads/master")) {
      return false;
    }

    if (data.toLowerCase().startsWith("refs/heads/feature")) {
      return false;
    }

    if (data.toLowerCase().startsWith("refs/heads/bugfix")) {
      return false;
    }

    if (data.toLowerCase().startsWith("refs/heads/hotfix")) {
      return false;
    }

    if (data.toLowerCase().startsWith("refs/heads/release")) {
      return false;
    }

    Pattern pattern = Pattern.compile(regx);
    Matcher matcher = pattern.matcher(data);
    return matcher.matches();
  }

  private void TriggerWithDefinition(RepositoryHookContext context, String definition, TeamcityConfiguration conf, Settings settings) {
    try {
      for (String vcsRoot : definition.split("\\s+")) {
        try {
          TeamcityLogger.logMessage(context, "Trigger Check for Changes in: " + vcsRoot);
          this.connector.TriggerCheckForChanges(conf, vcsRoot, settings);
        } catch (Exception e) {
          TeamcityLogger.logMessage(context, "Trigger Check for Changes in: " + vcsRoot + " Failed : " + e.getMessage());
        }
      }
    } catch (Exception e) {
      TeamcityLogger.logMessage(context, "Invalid vcs configuration data: " + definition + " - " + e.getMessage());
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
    try {
      String baseUrl = context.getSettings().getString("bitbuckerUrl");
      String comment = "remote trigger from bitbucket server : server address not specified in Bitbucket";

      if (!baseUrl.isEmpty()) {
        comment = "remote trigger from bitbucket server : " + baseUrl + "/branches";
      }

      TeamcityLogger.logMessage(context, "" + timeStamp + " Trigger builds for branch: " + branch);
      for (String buildId : buildIdIn.split("\\s+")) {
        try {
          TeamcityLogger.logMessage(context, "Trigger BuildId: " + buildId);

          if (!this.connector.IsInQueue(conf, buildId, branch, settings)) {
            this.connector.QueueBuild(conf, branch, buildId, comment, isDefault, settings);
          } else {
            TeamcityLogger.logMessage(context, "Skip already in queue: " + buildId);
          }
        } catch (Exception e) {
          TeamcityLogger.logMessage(context, "BuildId: " + buildId + " Failed");
          TeamcityLogger.logMessage(context, "Error: " + e.getMessage());
        }
      }
    } catch (Exception e) {
      TeamcityLogger.logMessage(context, "Invalid build configuration data: " + buildIdIn + " : " + e.getMessage());
    }
  }
}
