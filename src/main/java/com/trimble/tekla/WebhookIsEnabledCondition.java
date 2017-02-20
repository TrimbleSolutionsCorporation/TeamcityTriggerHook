package com.trimble.tekla;

import java.util.Map;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;


/**
 * A Condition that passes when the webhook is enabled for the provided
 * repository.
 * From : https://github.com/Nerdwin15/stash-jenkins-postreceive-webhook
 * @author Michael Irwin (mikesir87)
 */
public class WebhookIsEnabledCondition implements Condition {

  private static final String REPOSITORY = "repository";
  
  private SettingsService settingsService;
  
  /**
   * Create a new instance of the condition
   * @param settingsService The settings service
   */
  public WebhookIsEnabledCondition(SettingsService settingsService) {
    this.settingsService = settingsService;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void init(Map<String, String> context) throws PluginParseException {
    // Nothing to do here
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean shouldDisplay(Map<String, Object> context) {
    final Object obj = context.get(REPOSITORY);
    final Object pr = context.get("pullRequest");

    
    if (obj == null || !(obj instanceof Repository))
      return false;
    
    if (pr == null || !(pr instanceof PullRequest))
      return false;    
    
    Repository repo = (Repository) obj;
    PullRequest pullrequest = (PullRequest) pr;
    RepositoryHook hook = settingsService.getRepositoryHook(repo);
    Settings settings = settingsService.getSettings(repo);
    
    // check if builds are configured
    PullRequestRef ref = pullrequest.getFromRef();
    String branch = ref.getId();
    
    
    String root = settings.getString("featureRule", "");
    Boolean isConfigured = false;
    if(!root.isEmpty() && 
        (branch.toLowerCase().startsWith("refs/heads/feature/") || 
         branch.toLowerCase().startsWith("feature/"))) {
      isConfigured = true;
    }
    
    root = settings.getString("bugFixRule", "");
    if(!root.isEmpty() && 
        (branch.toLowerCase().startsWith("refs/heads/bugfix/") || 
         branch.toLowerCase().startsWith("bugfix/"))) {
      isConfigured = true;
    }

    root = settings.getString("hotfixRule", "");
    if(!root.isEmpty() && 
        (branch.toLowerCase().startsWith("refs/heads/hotfix/") || 
         branch.toLowerCase().startsWith("hotfix/"))) {
      isConfigured = true;
    }    
            
    return !(hook == null || !hook.isEnabled() || settings == null || !isConfigured);
  }
}
