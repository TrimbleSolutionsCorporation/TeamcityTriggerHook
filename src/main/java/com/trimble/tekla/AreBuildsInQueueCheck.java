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
import com.trimble.tekla.pojo.QueueMonitorTask;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import com.trimble.tekla.teamcity.TeamcityLogger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.Nonnull;
import javax.inject.Inject;
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
  private final Map<String, TimerTask> queuedTasksOnePerTeamcityServer = new HashMap<>();
  private final Timer timer;
  private long start;

  @Autowired
  @Inject
  public AreBuildsInQueueCheck(@ComponentImport I18nService i18nService,
          final TeamcityConnectionSettings connectionSettings,
          final SettingsService settingsService) {
      this.connectionSettings = connectionSettings;
      this.settingsService = settingsService;    
      this.i18nService = i18nService;
      this.connector = new TeamcityConnector(new HttpConnector());
      this.timer = new Timer();
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

    if(!queuedTasksOnePerTeamcityServer.containsKey(teamcityAddress)) {
      TeamcityLogger.logMessage(settings.get(), "[AreBuildsInQueueCheck] Startup Queue Checker Thread");

      
      TimerTask timerTask = new QueueMonitorTask(this.connector, conf, settings.get());
      timer.schedule(timerTask, 0, 20000);    
      queuedTasksOnePerTeamcityServer.put(teamcityAddress, timerTask);
      start = System.currentTimeMillis();
    } else {
      TeamcityLogger.logMessage(settings.get(), "[AreBuildsInQueueCheck] Queue Checker Thread Started Already");
    }
    
    QueueMonitorTask schedullerTask = (QueueMonitorTask)queuedTasksOnePerTeamcityServer.get(teamcityAddress);
           
    if(!schedullerTask.IsReady()) {
      TeamcityLogger.logMessage(settings.get(), "[AreBuildsInQueueCheck] Scheduller not ready");
      long current = System.currentTimeMillis();
      long elapsed = current - start;
      TeamcityLogger.logMessage(settings.get(), "[AreBuildsInQueueCheck] Elapsed Time since Start Checker " + elapsed);
      if (current - start > 50000) {
        queuedTasksOnePerTeamcityServer.remove(teamcityAddress);
        TeamcityLogger.logMessage(settings.get(), "[AreBuildsInQueueCheck] will try to restart queue checker");
        TimerTask timerTask = new QueueMonitorTask(this.connector, conf, settings.get());
        timer.schedule(timerTask, 0, 20000);
        queuedTasksOnePerTeamcityServer.put(teamcityAddress, timerTask);
        start = System.currentTimeMillis();        
      }
      
      String summaryMsg = "Build queue checker not ready";
      String detailedMsg = "Refresh in a few seconds to verify if builds are if you have queued builds";     
      return RepositoryHookResult.rejected(summaryMsg, detailedMsg);     
    }

    final String branch = pr.getFromRef().getDisplayId();
    if(schedullerTask.AreBuildsInQueueForBranch(branch)) {
      TeamcityLogger.logMessage(settings.get(), "Builds in queue for " + branch);
      String teamcityAddressQueue = settings.get().getString("teamCityUrl") + "/queue.html";
      String summaryMsg = i18nService.getText("mergecheck.builds.inqueue.summary", "Builds in queue");
      String detailedMsg = i18nService.getText("mergecheck.builds.inqueue.detailed", "Builds are still in queue, visit: ") + teamcityAddressQueue;
     
      TeamcityLogger.logMessage(settings.get(), "[AreBuildsInQueueCheck] builds in queue for " + branch + " reject");
      return RepositoryHookResult.rejected(summaryMsg, detailedMsg);    
    } else {
      TeamcityLogger.logMessage(settings.get(), "[AreBuildsInQueueCheck] No builds in queue for " + branch);
    }
    
    return RepositoryHookResult.accepted();
  }  
}
