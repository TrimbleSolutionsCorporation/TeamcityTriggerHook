package com.trimble.tekla;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.bitbucket.event.repository.RepositoryDeletedEvent;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.event.api.EventListener;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Manages storing and retrieving of TeamCity user password
 */
public class TeamcityConnectionSettings {

    private final PluginSettings pluginSettings;

    /**
     * Constructor
     *
     * @param factory - auto injected {@link PluginSettingsFactory}
     */
    public TeamcityConnectionSettings(final PluginSettingsFactory factory) {
        this.pluginSettings = factory.createSettingsForKey(ConcreteSettingsService.KEY);
    }

    /**
     * Retrieves TeamCity password for specified repository
     *
     * @param repository - {@link Repository}
     * @return password
     */
    public String getPassword(final Repository repository) {
        final Object passwordObj = this.pluginSettings.get(createKey(repository));
        if (null == passwordObj) {
            return StringUtils.EMPTY;
        } else {
            return passwordObj.toString();
        }
    }

    /**
     * Event listener to remove stored TeamCity password on repository deletion
     *
     * @param event - {@link RepositoryDeletedEvent}
     */
    @EventListener
    public void onRepositoryDeleted(final RepositoryDeletedEvent event) {
        this.pluginSettings.remove(createKey(event.getRepository()));
    }

    /**
     * Generates a unique key for storing TeamCity password for a specific repository
     *
     * @param repository - {@link Repository}
     * @return unique key
     */
    private String createKey(final Repository repository) {
        return String.format("repo.%s.teamcity.password", repository.getId());
    }

    /**
     * Stores TeamCity password for specified repository
     *
     * @param password - TeamCity password
     * @param repository - {@link Repository}
     */
    public void savePassword(final String password, final Repository repository) {
        this.pluginSettings.put(createKey(repository), password);
    }
}
