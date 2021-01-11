/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.teamcity;

import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.setting.Settings;
import com.trimble.tekla.Field;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jocs
 */
public class TeamcityLogger {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(TeamcityLogger.class);
 
  public static void logMessage(RepositoryHookContext context, String repoName, String message) {    
    if (context == null) {  
      LOG.error("[TeamcityTriggerHook][INFO][" + repoName + "] : Context is null, cant get debug flag");
      return;
    }
   
    if(isDebugEnabled(context.getSettings(), repoName)) {
      LOG.info("[TeamcityTriggerHook][INFO][" + repoName + "] : " + message);
    }
  }

  public static void logMessage(Settings settings, String repoName, String message) {
    if (settings == null) {
      LOG.error("[TeamcityTriggerHook][INFO][" + repoName + "] : Context is null, cant get debug flag");
      return;
    }

    if(isDebugEnabled(settings, repoName)) {
      LOG.info("[TeamcityTriggerHook][INFO][" + repoName + "] : " + message);
    } else {
      LOG.info("[TeamcityTriggerHook][INFO][" + repoName + "] : Logging Disable");
    }
  }

  public static void logError(RepositoryHookContext context, String repoName, String message, Throwable ex) {
    if (context == null) {
      LOG.error("[TeamcityTriggerHook][ERROR][" + repoName + "] : Context is null, cant get debug flag");
      return;
    }

    LOG.error("[TeamcityTriggerHook][ERROR][" + repoName + "] : " + message, ex);
  }

  private static boolean isDebugEnabled(Settings settings, String repoName) {
    try {
      return settings.getBoolean(Field.DEBUG);  
    } catch (Exception e) {      
      LOG.info("[TeamcityTriggerHook][INFO][" + repoName + "] : Logging is not setup, will be disabled");
      return false;
    }    
  }
}
