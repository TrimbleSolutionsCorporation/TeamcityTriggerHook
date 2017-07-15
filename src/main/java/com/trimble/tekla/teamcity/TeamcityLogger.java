/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.teamcity;

import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.setting.Settings;

/**
 *
 * @author jocs
 */
public class TeamcityLogger {
  public static void logMessage(RepositoryHookContext context, String message) {
    if (context == null) {
      return;
    }
    
    Boolean isDebugEnabled = context.getSettings().getBoolean("useDebugEnabled", false);
    
    if(isDebugEnabled) {
      System.out.println("[TeamcityTriggerHook] : " + message);
    }
  }
  
  public static void logMessage(Settings settings, String message) {
    if (settings == null) {
      return;
    }
    
    Boolean isDebugEnabled = settings.getBoolean("useDebugEnabled", false);
    
    if(isDebugEnabled) {
      System.out.println("[TeamcityTriggerHook] : " + message);
    }
  }  

}
