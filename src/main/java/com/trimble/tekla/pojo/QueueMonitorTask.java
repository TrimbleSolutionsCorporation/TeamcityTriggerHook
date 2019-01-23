/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.pojo;

import com.atlassian.bitbucket.setting.Settings;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import com.trimble.tekla.teamcity.TeamcityLogger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/**
 *
 * @author jocs
 */
public class QueueMonitorTask  extends TimerTask {

  private final TeamcityConnector connector;
  private final TeamcityConfiguration conf;
  private final Settings settings;
  final List<TeamcityQueuedElement> builds = new ArrayList<>();
  private boolean isStarted;
  public QueueMonitorTask(final TeamcityConnector connector,
          final TeamcityConfiguration conf,
          final Settings settings) {
    this.connector = connector;
    this.conf = conf;
    this.settings = settings;
    this.isStarted = false;
    TeamcityLogger.logMessage(settings, "[QueueMonitorTask] Started...");
  }
  
  public Boolean IsReady() {
    return this.isStarted;
  }  
  
  public Boolean AreBuildsInQueueForBranch(String branch) {
    for (TeamcityQueuedElement build : builds) {
      if(build.getBranch().equals(branch)) {
        return true;
      }
    }
    
    return false;
  }
  
  @Override
  public void run() {
    TeamcityLogger.logMessage(settings, "[QueueCheckerThread] Run Schedulle Checker Task");
    try {
      this.isStarted = false; 
      builds.clear();
      List<TeamcityQueuedElement> queue = this.connector.GetQueuedBuilds(conf, settings);
      TeamcityLogger.logMessage(settings, "[QueueCheckerThread] Queue Contains: " + queue.size() + " elemetns");
      builds.addAll(queue);
      this.isStarted = true;
    } catch (IOException ex) {
      TeamcityLogger.logMessage(settings, "[QueueCheckerThread] in exception: " + ex.getMessage());
      Logger.getLogger("QueueCheckerThread").log(Level.SEVERE, "IO exception getting Queue, TC down?", ex);
    } catch (JSONException ex) {
      TeamcityLogger.logMessage(settings, "[QueueCheckerThread] json exception: " + ex.getMessage());
      Logger.getLogger("QueueCheckerThread").log(Level.SEVERE, "Invalid Json getting Queue", ex);
    } catch (Exception ex) {
      TeamcityLogger.logMessage(settings, "[QueueCheckerThread] other exception: " + ex.getMessage());
      Logger.getLogger("QueueCheckerThread").log(Level.SEVERE, "Invalid Json getting Queue", ex);
    }
    TeamcityLogger.logMessage(settings, "[QueueCheckerThread] Run Schedulle Checker Task Done");
  }  
}
