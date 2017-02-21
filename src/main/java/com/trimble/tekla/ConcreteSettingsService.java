package com.trimble.tekla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.hook.repository.RepositoryHookService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.user.SecurityService;

/**
 * Default implementation of the {@link SettingsService} interface that uses
 * a SecurityService to ensure that the current user has the ability to retrieve
 * the webhook settings.
 * From: https://github.com/Nerdwin15/stash-jenkins-postreceive-webhook
 * @author Michael Irwin (mikesir87)
 */
public class ConcreteSettingsService implements SettingsService {

  private static final Logger LOGGER = 
      LoggerFactory.getLogger(ConcreteSettingsService.class);
  
  public static String KEY = "com.trimble.tekla:TeamcityTriggerHook";

  private RepositoryHookService hookService;
  private SecurityService securityService;
  
  /**
   * Create a new instance.
   * @param hookService The repository hook service
   * @param securityService The security service
   */
  public ConcreteSettingsService(RepositoryHookService hookService,
      SecurityService securityService) {
    this.hookService = hookService;
    this.securityService = securityService;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public RepositoryHook getRepositoryHook(final Repository repository) {
    try {
      return securityService.withPermission(Permission.REPO_ADMIN, "Retrieving repository hook")
              .call(() -> hookService.getByKey(repository, ConcreteSettingsService.KEY));
    } catch (Exception e) {
      LOGGER.error("Unexpected exception trying to get repository hook", e);
      return null;
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public Settings getSettings(final Repository repository) {
    try {
      return securityService.withPermission(Permission.REPO_ADMIN, "Retrieving settings")
              .call(() -> hookService.getSettings(repository, ConcreteSettingsService.KEY));
    } catch (Exception e) {
      LOGGER.error("Unexpected exception trying to get webhook settings", e);
      return null;
    }
  }
}
