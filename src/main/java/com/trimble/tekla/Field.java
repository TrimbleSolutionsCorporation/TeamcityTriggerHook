package com.trimble.tekla;

/**
 * Named constants of configuration field names for hook configuration dialog form
 */
public final class Field {

    public static final String BITBUCKET_URL = "bitbucketUrl";
    public static final String TEAMCITY_URL = "teamCityUrl";
    public static final String TEAMCITY_USERNAME = "teamCityUserName";
    public static final String TEAMCITY_PASSWORD = "teamCityPassword";
    public static final String DEBUG = "isDebugEnabled";
    public static final String REPOSITORY_TRIGGERS_JSON = "repositoryTriggersJson";

    private Field() {
        throw new IllegalStateException("Constant class");
    }
}
