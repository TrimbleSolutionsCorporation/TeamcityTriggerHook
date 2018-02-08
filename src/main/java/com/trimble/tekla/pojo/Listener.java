package com.trimble.tekla.pojo;

/**
 *
 */
public class Listener {
    private String regexp;
    private String target;
    private String targetId;
    private Boolean triggerOnPullRequest; 
    private String downStreamUrl;
    private String downStreamTriggerType;
          
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
}
