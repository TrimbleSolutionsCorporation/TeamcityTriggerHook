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
 
  public static void logMessage(RepositoryHookContext context, String message) {
    
    if (context == null) {  
      LOG.error("[TeamcityTriggerHook] : Context is null, cant get debug flag");
      return;
    }

    Boolean isDebugEnabled = context.getSettings().getBoolean(Field.DEBUG, false);

    if(isDebugEnabled) {
      LOG.info("[TeamcityTriggerHook] : " + message);
    }
  }

  public static void logMessage(Settings settings, String message) {
    if (settings == null) {
      LOG.error("[TeamcityTriggerHook] : Context is null, cant get debug flag");
      return;
    }

    Boolean isDebugEnabled = settings.getBoolean(Field.DEBUG, false);
    if(isDebugEnabled) {
      LOG.info("[TeamcityTriggerHook ->] : " + message);
    }
  }
}
