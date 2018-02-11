package com.trimble.tekla.pojo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 */
public class Listener {
  private String branchConfig;
  private String regexp;
  private String target;
  private String targetId;
  private Boolean triggerOnPullRequest;
  private String downStreamUrl;
  private String downStreamTriggerType;

  public String getBranchConfig() {
    return this.branchConfig;
  }

  public void setBranchConfig(String branchConfig) {
    this.branchConfig = branchConfig;
  }

  public Boolean getTriggerOnPullRequest() {
    return this.triggerOnPullRequest;
  }

  public void setTriggerOnPullRequest(Boolean triggerOnPullRequest) {
    this.triggerOnPullRequest = triggerOnPullRequest;
  }

  public String getDownStreamTriggerType() {
    return this.downStreamTriggerType;
  }

  public void setDownStreamTriggerType(String downStreamTriggerType) {
    this.downStreamTriggerType = downStreamTriggerType;
  }

  public String getDownStreamUrl() {
    return this.downStreamUrl;
  }

  public void setDownStreamUrl(String downStreamUrl) {
    this.downStreamUrl = downStreamUrl;
  }

  public String getRegexp() {
    return this.regexp;
  }

  public void setRegexp(final String regexp) {
    this.regexp = regexp;
  }

  public String getTarget() {
    return this.target;
  }

  public void setTarget(final String target) {
    this.target = target;
  }

  public String getTargetId() {
    return this.targetId;
  }

  public void setTargetId(final String targetId) {
    this.targetId = targetId;
  }
  
  public static Listener[] GetBuildConfigurationsFromBranch(final String jsonConfiguration, final String branch) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final Map<String, Listener> listenerMap;
    final List<Listener> configs = new ArrayList<>();
    listenerMap = mapper.readValue(jsonConfiguration, mapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Listener.class));
    for (final Map.Entry<String, Listener> listenerEntry : listenerMap.entrySet()) {
      final Pattern pattern = Pattern.compile(listenerEntry.getValue().getRegexp());
      final Matcher matcher = pattern.matcher(branch);
      while (matcher.find()) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
          listenerEntry.getValue().setBranchConfig(matcher.group(i));
          configs.add(listenerEntry.getValue());
        }
      }
    }

    return configs.toArray(new Listener[configs.size()]);
  } 
}
