package com.trimble.tekla.pojo;

/**
 *
 */
public class Listener {
    private String regexp;
    private String target;
    private String targetId;

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
