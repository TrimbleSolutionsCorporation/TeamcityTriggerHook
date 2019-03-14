package com.trimble.tekla;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.trimble.tekla.pojo.Trigger;


/**
 * A Condition that passes when the webhook is enabled for the provided
 * repository.
 * From : https://github.com/Nerdwin15/stash-jenkins-postreceive-webhook
 * @author Michael Irwin (mikesir87)
 */
public class WebhookIsEnabledCondition implements Condition {

  private static final String REPOSITORY = "repository";

  private final SettingsService settingsService;

  /**
   * Create a new instance of the condition
   * @param settingsService The settings service
   */
  @Inject
  public WebhookIsEnabledCondition(final SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(final Map<String, String> context) throws PluginParseException {
    // Nothing to do here
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean shouldDisplay(final Map<String, Object> context) {
    final Object obj = context.get(REPOSITORY);
    final Object pr = context.get("pullRequest");

    if (obj == null || !(obj instanceof Repository)) {
        return false;
    }

    if (pr == null || !(pr instanceof PullRequest)) {
        return false;
    }

    final Repository repo = (Repository) obj;
    final PullRequest pullrequest = (PullRequest) pr;
    final RepositoryHook hook = this.settingsService.getRepositoryHook(repo);
    final Settings settings = this.settingsService.getSettings(repo).get();

    if (settings == null || hook == null || !hook.isEnabled()) {
      return false;
    }

    final String repositoryTriggersJson = settings.getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
    if(repositoryTriggersJson.isEmpty()) {
      return false;
    }

    // check if builds are configured
    final PullRequestRef ref = pullrequest.getFromRef();
    final String branch = ref.getId();

    try {
      final Trigger[] configurations = Trigger.GetBuildConfigurationsFromBranch(repositoryTriggersJson, branch);
      if (configurations.length > 0) {
        return true;
      }
    } catch (final IOException ex) {
    }

    return false;
  }
}
