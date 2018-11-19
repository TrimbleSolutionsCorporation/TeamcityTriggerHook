/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.pojo;

import com.atlassian.bitbucket.setting.Settings;
import com.trimble.tekla.AreBuildsInQueueCheck;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    try {
      builds.clear();
      builds.addAll(this.connector.GetQueuedBuilds(conf, settings));
      this.isStarted = true;
    } catch (IOException ex) {
      Logger.getLogger(AreBuildsInQueueCheck.class.getName()).log(Level.SEVERE, null, ex);
    } catch (JSONException ex) {
      Logger.getLogger(AreBuildsInQueueCheck.class.getName()).log(Level.SEVERE, null, ex);
    }
  }  
}
