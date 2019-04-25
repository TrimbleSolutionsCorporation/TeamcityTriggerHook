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
public class AreBuildsInQueueCheck implements RepositoryMergeCheck {

  private final I18nService i18nService;
  private final TeamcityConnectionSettings connectionSettings;
  private final SettingsService settingsService;
  private final TeamcityConnector connector;

  @Autowired
  @Inject
  public AreBuildsInQueueCheck(@ComponentImport I18nService i18nService,
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

    Optional<Settings> settings = this.settingsService.getSettings(repository);
    
    if(!settings.isPresent()) {
      return RepositoryHookResult.accepted();
    }

    TeamcityLogger.logMessage(settings.get(), "[AreBuildsInQueueCheck] Queue Checker Started");
    final String teamcityAddress = settings.get().getString("teamCityUrl");
    if(teamcityAddress == null || "".equals(teamcityAddress)) {
      return RepositoryHookResult.accepted();
    }
    
    final String password = this.connectionSettings.getPassword(repository);
    final TeamcityConfiguration conf
            = new TeamcityConfiguration(
                    settings.get().getString("teamCityUrl"),
                    settings.get().getString("teamCityUserName"),
                    password);
           
    final String branch = pr.getFromRef().getDisplayId();
    if(AreBuildsInQueueForBranch(branch, conf, settings.get())) {
      TeamcityLogger.logMessage(settings.get(), "Builds in queue for " + branch);
      String teamcityAddressQueue = settings.get().getString("teamCityUrl") + "/queue.html";
      String summaryMsg = i18nService.getText("mergecheck.builds.inqueue.summary", "Builds in queue");
      String detailedMsg = i18nService.getText("mergecheck.builds.inqueue.detailed", "Builds are still in queue, visit: ") + teamcityAddressQueue;
     
      TeamcityLogger.logMessage(settings.get(), "[AreBuildsInQueueCheck] builds in queue for " + branch + " reject");
      return RepositoryHookResult.rejected(summaryMsg, detailedMsg);    
    } 

    TeamcityLogger.logMessage(settings.get(), "[AreBuildsInQueueCheck] No builds in queue for " + branch);    
    return RepositoryHookResult.accepted();
  }
  
   private Boolean AreBuildsInQueueForBranch(String branch,
          final TeamcityConfiguration conf,
          final Settings settings) {    
    List<TeamcityQueuedElement> queue;    
    try {
      queue = this.connector.GetQueuedBuilds(conf, settings, branch);
      TeamcityLogger.logMessage(settings, "[AreBuildsInQueueCheck] Detected: " + queue.size() + " in queue.");
      return !queue.isEmpty();
    } catch (IOException | JSONException ex) {
      TeamcityLogger.logMessage(settings, "[AreBuildsInQueueCheck] Exception " + ex.getMessage());       
    }
    
    return false;    
  } 
}
