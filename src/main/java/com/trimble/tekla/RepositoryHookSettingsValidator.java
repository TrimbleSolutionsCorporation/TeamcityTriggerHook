package com.trimble.tekla;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.atlassian.bitbucket.scope.Scope;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.atlassian.bitbucket.setting.SettingsValidator;
import com.atlassian.sal.api.message.I18nResolver;
import com.trimble.tekla.pojo.Listener;

/**
 * Class for validating hook configuration form
 */
public class RepositoryHookSettingsValidator implements SettingsValidator {

    private static final Pattern URL_VALIDATION_PATTERN = Pattern.compile("^https?://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);
    private static final String BRANCH_TEST_STRING = "refs/heads/master";

    private final I18nResolver i18n;

    /**
     * Class constructor
     *
     * @param i18n - {@link I18nResolver} injected via component-import in atlassian-plugin.xml
     */
    public RepositoryHookSettingsValidator(final I18nResolver i18n) {
        this.i18n = i18n;
    }

    @Override
    public void validate(final Settings settings, final SettingsValidationErrors errors, final Scope scope) {
        try {
            validateConnectionTab(settings, errors);
            validateRepositoryListenersTab(settings, errors);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Validates form fields in connections tab
     *
     * @param settings - to be validated.
     * @param errors - callback for reporting validation errors.
     */
    private void validateConnectionTab(final Settings settings, final SettingsValidationErrors errors) {
        final String bitbucketUrl = settings.getString(Field.BITBUCKET_URL, StringUtils.EMPTY);
        if (!URL_VALIDATION_PATTERN.matcher(bitbucketUrl).matches()) {
            errors.addFieldError(Field.BITBUCKET_URL, this.i18n.getText("error.invalid.url"));
        }

        final String teamCityUrl = settings.getString(Field.TEAMCITY_URL, StringUtils.EMPTY);
        if (!URL_VALIDATION_PATTERN.matcher(teamCityUrl).matches()) {
            errors.addFieldError(Field.TEAMCITY_URL, this.i18n.getText("error.invalid.url"));
        }

        final String teamCityUserName = settings.getString(Field.TEAMCITY_USERNAME, StringUtils.EMPTY);
        if (StringUtils.EMPTY.equals(teamCityUserName)) {
            errors.addFieldError(Field.TEAMCITY_USERNAME, this.i18n.getText("error.required.field"));
        }

        final String teamCityPassword = settings.getString(Field.TEAMCITY_PASSWORD, StringUtils.EMPTY);
        if (StringUtils.EMPTY.equals(teamCityPassword)) {
            errors.addFieldError(Field.TEAMCITY_PASSWORD, this.i18n.getText("error.required.field"));
        }

        if (!Constant.TEAMCITY_PASSWORD_SAVED_VALUE.equals(teamCityPassword)) {
            errors.addFieldError(Field.TEAMCITY_PASSWORD, this.i18n.getText("error.require.validation", this.i18n.getText("connetion.button")));
        }
    }

    /**
     * Validates form data in repository listeners tab
     *
     * @param settings - to be validated.
     * @param errors - callback for reporting validation errors.
     * @throws IOException if JSON parsing error occurs
     */
    private void validateRepositoryListenersTab(final Settings settings, final SettingsValidationErrors errors) throws IOException {
        final String repositoryListenersJson = settings.getString(Field.REPOSITORY_LISTENERS_JSON, StringUtils.EMPTY);
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, Listener> listenerMap = mapper.readValue(repositoryListenersJson, mapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Listener.class));
        for (final Map.Entry<String, Listener> listenerEntry : listenerMap.entrySet()) {
            final Listener listener = listenerEntry.getValue();
            if (StringUtils.isBlank(listener.getTargetId())) {
                errors.addFieldError(listenerEntry.getKey(), this.i18n.getText("error.string.empty"));
            } else if (StringUtils.containsWhitespace(listener.getTargetId())) {
                errors.addFieldError(listenerEntry.getKey(), this.i18n.getText("error.string.contains.whitespace"));
            }
            try {
                final Pattern pattern = Pattern.compile(listenerEntry.getValue().getRegexp(), Pattern.CASE_INSENSITIVE);
                final Matcher matcher = pattern.matcher(BRANCH_TEST_STRING);
                if (matcher.groupCount() != 1) {
                    errors.addFieldError(listenerEntry.getKey(), this.i18n.getText("error.regexp.needs.capturing"));
                }
            } catch (final PatternSyntaxException e) {
                errors.addFieldError(listenerEntry.getKey(), e.getLocalizedMessage());
            }
        }
    }
}
