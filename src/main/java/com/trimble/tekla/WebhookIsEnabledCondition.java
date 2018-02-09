package com.trimble.tekla;

import java.util.Map;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.trimble.tekla.pojo.Listener;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;


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

    if (settings == null || hook == null || !hook.isEnabled()) {
      return false;
    }

    // check if builds are configured
    PullRequestRef ref = pullrequest.getFromRef();
    String branch = ref.getId();
    
    final String repositoryListenersJson = settings.getString(Field.REPOSITORY_LISTENERS_JSON, StringUtils.EMPTY);
    if(repositoryListenersJson.isEmpty()) {
      return false;
    }
    
    final ObjectMapper mapper = new ObjectMapper();
    final Map<String, Listener> listenerMap;
    try {
      listenerMap = mapper.readValue(repositoryListenersJson, mapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Listener.class));
      for (final Map.Entry<String, Listener> listenerEntry : listenerMap.entrySet()) {
       Pattern pattern = Pattern.compile(listenerEntry.getValue().getRegexp());
       Matcher matcher = pattern.matcher(branch);
       if (matcher.find()) {
         return true;
       }
      }
    } catch (IOException ex) {
      return false;
    }
    
    return false;            
  }
}
