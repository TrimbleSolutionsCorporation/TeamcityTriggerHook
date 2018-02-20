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
  private String type;
  private String target;
  private boolean triggerOnPullRequest;
  private String downStreamUrl;
  private String downStreamTriggerType;
  private boolean cancelRunningBuilds;
  private boolean triggerOnEmptyBranches;

  public String getBranchConfig() {
    return this.branchConfig;
  }

  public void setBranchConfig(final String branchConfig) {
    this.branchConfig = branchConfig;
  }

  public boolean isCancelRunningBuilds() {
    return this.cancelRunningBuilds;
  }

  public void setCancelRunningBuilds(final boolean cancelRunningBuilds) {
    this.cancelRunningBuilds = cancelRunningBuilds;
  }

  public boolean isTriggerOnPullRequest() {
    return this.triggerOnPullRequest;
  }

  public void setTriggerOnPullRequest(final boolean triggerOnPullRequest) {
    this.triggerOnPullRequest = triggerOnPullRequest;
  }

  public String getDownStreamTriggerType() {
    return this.downStreamTriggerType;
  }

  public void setDownStreamTriggerType(final String downStreamTriggerType) {
    this.downStreamTriggerType = downStreamTriggerType;
  }

  public String getDownStreamUrl() {
    return this.downStreamUrl;
  }

  public void setDownStreamUrl(final String downStreamUrl) {
    this.downStreamUrl = downStreamUrl;
  }

  public String getRegexp() {
    return this.regexp;
  }

  public void setRegexp(final String regexp) {
    this.regexp = regexp;
  }

  public String getType() {
    return this.type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getTarget() {
    return this.target;
  }

  public void setTarget(final String target) {
    this.target = target;
  }

  public boolean isTriggerOnEmptyBranches() {
    return triggerOnEmptyBranches;
  }

  public void setTriggerOnEmptyBranches(boolean triggerOnEmptyBranches) {
    this.triggerOnEmptyBranches = triggerOnEmptyBranches;
  }

  public static Listener[] GetBuildConfigurationsFromBranch(final String jsonConfiguration, final String branch) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final Map<String, Listener> triggerMap;
    final List<Listener> configs = new ArrayList<>();
    triggerMap = mapper.readValue(jsonConfiguration, mapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Listener.class));
    for (final Map.Entry<String, Listener> triggerEntry : triggerMap.entrySet()) {
      final Pattern pattern = Pattern.compile(triggerEntry.getValue().getRegexp(), Pattern.CASE_INSENSITIVE);
      final Matcher matcher = pattern.matcher(branch);
      if (matcher.find()) {
        triggerEntry.getValue().setBranchConfig(matcher.group(matcher.groupCount()));
        configs.add(triggerEntry.getValue());
      }
    }

    return configs.toArray(new Listener[configs.size()]);
  }
}
