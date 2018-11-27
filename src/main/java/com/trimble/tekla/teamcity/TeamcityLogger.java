/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.teamcity;

import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.setting.Settings;
import com.trimble.tekla.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jocs
 */
public class TeamcityLogger {
  public static void logMessage(RepositoryHookContext context, String message) {
    System.out.println("[TeamcityTriggerHook] : " + message);
    Logger.getLogger(TeamcityLogger.class.getName()).log(Level.INFO, message);
    if (context == null) {
      return;
    }

    Boolean isDebugEnabled = context.getSettings().getBoolean(Field.DEBUG, false);

    if(isDebugEnabled) {
      System.out.println("[TeamcityTriggerHook] : " + message);
    }
  }

  public static void logMessage(Settings settings, String message) {
    System.out.println("[TeamcityTriggerHook] : " + message);
    Logger.getLogger(TeamcityLogger.class.getName()).log(Level.INFO, message);
    if (settings == null) {
      return;
    }

    Boolean isDebugEnabled = settings.getBoolean(Field.DEBUG, false);

    if(isDebugEnabled) {
      System.out.println("[TeamcityTriggerHook] : " + message);
    }
  }

}
