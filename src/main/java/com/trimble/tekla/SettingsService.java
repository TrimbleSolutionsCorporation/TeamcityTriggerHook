package com.trimble.tekla;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.hook.repository.RepositoryHookService;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Operation;

/**
 * Utility class used to retrieve the Settings for a given repository that uses
 * a SecurityService to ensure that the current user has the ability to retrieve
 * the webhook settings.
 * From: https://github.com/Nerdwin15/stash-jenkins-postreceive-webhook
 */
@Named
public class SettingsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsService.class);

  public static String KEY = "com.trimble.tekla:teamcity-trigger-hook";

  private final RepositoryHookService hookService;
  private final SecurityService securityService;

  /**
   * Create a new instance.
   * @param hookService The repository hook service
   * @param securityService The security service
   */
  @Inject
  public SettingsService(final RepositoryHookService hookService, final SecurityService securityService) {
    this.hookService = hookService;
    this.securityService = securityService;
  }

  /**
   * Get the repository hook context for this hook in the provided repository.
   * @param repository The repository
   * @return The hook context
   */
  public RepositoryHook getRepositoryHook(final Repository repository) {
    try {
      return this.securityService.withPermission(Permission.REPO_ADMIN, "Retrieving repository hook")
              .call(new Operation<RepositoryHook, RuntimeException>() {
                @Override
                public RepositoryHook perform() throws RuntimeException {
                    return SettingsService.this.hookService.getByKey(repository, SettingsService.KEY);
                }
            });
    } catch (final Exception e) {
      LOGGER.error("Unexpected exception trying to get repository hook", e);
      return null;
    }
  }

  /**
   * Get the webhook settings for the provided repository.
   * @param repository The Repository
   * @return The webhook settings for the repository. Null if no settings set.
   */
  public Settings getSettings(final Repository repository) {
    try {
      return this.securityService.withPermission(Permission.REPO_ADMIN, "Retrieving settings")
              .call(new Operation<Settings, RuntimeException>() {
                @Override
                public Settings perform() throws RuntimeException {
                    return SettingsService.this.hookService.getSettings(repository, SettingsService.KEY);
                }
            });
    } catch (final Exception e) {
      LOGGER.error("Unexpected exception trying to get webhook settings", e);
      return null;
    }
  }
}
