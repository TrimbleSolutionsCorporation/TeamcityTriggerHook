/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryMergeCheck;
import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.trimble.tekla.pojo.TeamcityQueuedElement;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import com.trimble.tekla.teamcity.TeamcityLogger;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jocs
 */
public class AreBuildsInQueueOrRunningCheck implements RepositoryMergeCheck {

  private final I18nService i18nService;
  private final TeamcityConnectionSettings connectionSettings;
  private final SettingsService settingsService;
  private final TeamcityConnector connector;

  @Autowired
  @Inject
  public AreBuildsInQueueOrRunningCheck(@ComponentImport I18nService i18nService,
          final TeamcityConnectionSettings connectionSettings,
          final SettingsService settingsService) {
      this.connectionSettings = connectionSettings;
      this.settingsService = settingsService;    
      this.i18nService = i18nService;
      this.connector = new TeamcityConnector(new HttpConnector());
  }
    
  @Override
  public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext prhc, @Nonnull PullRequestMergeHookRequest t) {
    PullRequest pr = t.getPullRequest();
    Repository repository = pr.getToRef().getRepository();
    final String repoName = repository.getName();

    Optional<Settings> settings = this.settingsService.getSettings(repository);
    
    if(!settings.isPresent()) {
      return RepositoryHookResult.accepted();
    }

    TeamcityLogger.logMessage(settings.get(), repoName, "[AreBuildsInQueueOrRunningCheck] Queue Checker Started");
    final String teamcityAddress = settings.get().getString(Field.TEAMCITY_URL);
    if(teamcityAddress == null || "".equals(teamcityAddress)) {
      return RepositoryHookResult.accepted();
    }
    
    final String password = this.connectionSettings.getPassword(repository);
    final TeamcityConfiguration conf
            = new TeamcityConfiguration(
                    settings.get().getString(Field.TEAMCITY_URL),
                    settings.get().getString(Field.TEAMCITY_USERNAME),
                    password);
           
    final String branch = pr.getFromRef().getDisplayId();

    try {
      if(AreBuildsInQueueForBranch(branch, conf, settings.get(), repoName)) {
        TeamcityLogger.logMessage(settings.get(), repoName, "Builds in queue for " + branch);
        String teamcityAddressQueue = settings.get().getString(Field.TEAMCITY_URL) + "/queue.html";
        String teamcityAddressAgents = settings.get().getString(Field.TEAMCITY_URL) + "/agents.html";
        String summaryMsg = i18nService.getText("mergecheck.builds.inqueue.summary", "Builds in queue or running");
        String detailedMsg = i18nService.getText("mergecheck.builds.inqueue.detailed", "Builds are still in queue or running, visit: ") + 
          teamcityAddressQueue + " or " + teamcityAddressAgents;       
        TeamcityLogger.logMessage(settings.get(), repoName, "[AreBuildsInQueueOrRunningCheck] builds in queue for " + branch + " reject");
        return RepositoryHookResult.rejected(summaryMsg, detailedMsg);    
      }
    } catch (Exception ex) {
      TeamcityLogger.logMessage(settings.get(), repoName, "Exception retriving queued or running builds for " + branch);
      String summaryMsg = i18nService.getText("mergecheck.builds.inqueue.summary", "Exception: Cannot retrive data from Teamcity server");
      String detailedMsg = i18nService.getText("mergecheck.builds.inqueue.detailed", "Make sure: ") + teamcityAddress + " is up and running. And accessible to current Bitbucket server";     
      TeamcityLogger.logMessage(settings.get(), repoName, "[AreBuildsInQueueOrRunningCheck] exception " + ex.getMessage() + " reject");
      return RepositoryHookResult.rejected(summaryMsg, detailedMsg);  
    }
    
    TeamcityLogger.logMessage(settings.get(), repoName, "[AreBuildsInQueueOrRunningCheck] No builds in queue for " + branch);
    return RepositoryHookResult.accepted();
  }
  
   private Boolean AreBuildsInQueueForBranch(String branch,
          final TeamcityConfiguration conf,
          final Settings settings,
          final String repoName) throws IOException, JSONException {
    List<TeamcityQueuedElement> queue;    
      queue = this.connector.GetQueuedAndRunningBuilds(conf, settings, branch, repoName);
      TeamcityLogger.logMessage(settings, repoName, "[AreBuildsInQueueOrRunningCheck] Detected: " + queue.size() + " in queue.");
      return !queue.isEmpty();
  } 
}
