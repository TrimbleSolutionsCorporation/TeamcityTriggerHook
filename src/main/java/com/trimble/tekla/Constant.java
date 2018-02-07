package com.trimble.tekla;

import java.util.Map;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/**
 * Miscellaneous named constants
 */
public final class Constant {

    public static final ClientConfig REST_CLIENT_CONFIG = new DefaultClientConfig();

    static {
        final Map<String, Object> properties = REST_CLIENT_CONFIG.getProperties();
        properties.put(ClientConfig.PROPERTY_READ_TIMEOUT, 60000);
        properties.put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 10000);
    }

    public static final String TEAMCITY_PASSWORD_SAVED_VALUE = "PASSWORD✅SAVED";

    private Constant() {
        throw new IllegalStateException("Constant class");
    }
}
