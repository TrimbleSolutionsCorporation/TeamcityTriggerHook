/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla;

import com.atlassian.bitbucket.event.repository.RepositoryDeletedEvent;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.event.api.EventListener;

/**
 *
 * @author jocs
 */
public class TeamcityConnectionSettings {
  private final PluginSettings pluginSettings;
  public TeamcityConnectionSettings(PluginSettingsFactory factory) {
      this.pluginSettings = factory.createSettingsForKey(ConcreteSettingsService.KEY);    
  }
  
  public String getPassword(Repository repository) {
    Object passwordObj = pluginSettings.get(createKey(repository, "password"));    
    if (passwordObj == null) {
      return "";
    } else {
      String password = passwordObj.toString();
      return password;
    }
  }

  @EventListener
  public void onRepositoryDeleted(RepositoryDeletedEvent event) {
      pluginSettings.remove(createKey(event.getRepository(), "password"));
  }

  private String createKey(Repository repository, String key) {
      return "repo." + repository.getId() + ".teamcity." + key;
  }    

  public void SavePassword(String password, Repository repository) {
    String passwordKey = createKey(repository, "password");
    
    this.pluginSettings.put(passwordKey, password);
  }
}
