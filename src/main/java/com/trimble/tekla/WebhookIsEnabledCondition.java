package com.trimble.tekla;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.trimble.tekla.pojo.Trigger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

public class WebhookIsEnabledCondition implements Condition {
  private static final Pattern REPOREGEX = Pattern.compile(".*?/projects/(.*?)/repos/(.*?)/pull-requests/(.*?)/.*");
  private final SettingsService settingsService;
  private final RepositoryService repositoryService;
  private final PullRequestService pullRequestService;

  /**
   * Create a new instance of the condition
   *
   * @param settingsService The settings service
   */
  @Inject
  public WebhookIsEnabledCondition(final SettingsService settingsService, final RepositoryService repositoryService, final PullRequestService pullRequestService) {
    this.settingsService = settingsService;
    this.repositoryService = repositoryService;
    this.pullRequestService = pullRequestService;
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

    final Repository repository = getRepository(context);
    if (repository == null) {
      return false;
    }
    final RepositoryHook hook = this.settingsService.getRepositoryHook(repository);
    final Settings settings = this.settingsService.getSettings(repository).get();

    if (settings == null || hook == null || !hook.isEnabled()) {
      return false;
    }
    final PullRequest pullrequest = getPullRequest(context, repository);
    if (pullrequest == null) {
      return false;
    }

    final String repositoryTriggersJson = settings.getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
    if (repositoryTriggersJson.isEmpty()) {
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
  
  private PullRequest getPullRequest(Map<String, Object> context, Repository repo) {

    Object request = context.get("request");
    if (!(request instanceof HttpServletRequest)) {
      return null;
    }

    String path = ((HttpServletRequest) request).getRequestURI();
    Matcher matcher = REPOREGEX.matcher(path);
    if (matcher.matches()) {
      long prId = Long.parseLong(matcher.group(3));
      return pullRequestService.getById(repo.getId(), prId);
    }
    return null;
  }

  private Repository getRepository(Map<String, Object> context) {
    final Object obj = context.get("repository");
    if (!(obj instanceof Repository)) {
      Object request = context.get("request");
      if (!(request instanceof HttpServletRequest)) {
        return null;
      }
      String path = ((HttpServletRequest) request).getRequestURI();
      Matcher matcher = REPOREGEX.matcher(path);
      if (matcher.matches()) {
        String projectKey = matcher.group(1);
        String repoSlug = matcher.group(2);
        return repositoryService.getBySlug(projectKey, repoSlug);
      }
      return null;
    }
    return (Repository) obj;
  }  
}
