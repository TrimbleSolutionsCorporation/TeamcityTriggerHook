package com.trimble.tekla;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scope.Scope;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.atlassian.bitbucket.setting.SettingsValidator;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.trimble.tekla.pojo.Trigger;

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
    @Inject
    public RepositoryHookSettingsValidator(@ComponentImport final I18nResolver i18n) {
        this.i18n = i18n;
    }

    @Override
    public void validate(Settings stngs, SettingsValidationErrors sve, Scope scope) {
        try {
            validateConnectionTab(stngs, sve);
            validaterepositoryTriggersTab(stngs, sve);
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
        
        final Boolean isDebugOn = settings.getBoolean(Field.DEBUG);
                
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
            errors.addFieldError(Field.TEAMCITY_PASSWORD, this.i18n.getText("error.require.validation", this.i18n.getText("connetion.test.button")));
        }
    }

    /**
     * Validates form data in repository triggers tab
     *
     * @param settings - to be validated.
     * @param errors - callback for reporting validation errors.
     * @throws IOException if JSON parsing error occurs
     */
    private void validaterepositoryTriggersTab(final Settings settings, final SettingsValidationErrors errors) throws IOException {
        final String repositoryTriggersJson = settings.getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, Trigger> triggerMap = mapper.readValue(repositoryTriggersJson, mapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Trigger.class));
        for (final Map.Entry<String, Trigger> triggerEntry : triggerMap.entrySet()) {
            final Trigger trigger = triggerEntry.getValue();
            if (StringUtils.isBlank(trigger.getTarget())) {
                errors.addFieldError(triggerEntry.getKey(), this.i18n.getText("error.string.empty"));
            } else if (StringUtils.containsWhitespace(trigger.getTarget())) {
                errors.addFieldError(triggerEntry.getKey(), this.i18n.getText("error.string.contains.whitespace"));
            }
            try {
                final Pattern pattern = Pattern.compile(triggerEntry.getValue().getRegex(), Pattern.CASE_INSENSITIVE);
                final Matcher matcher = pattern.matcher(BRANCH_TEST_STRING);
                if (matcher.groupCount() != 1) {
                    errors.addFieldError(triggerEntry.getKey(), this.i18n.getText("error.regex.needs.capturing"));
                }
            } catch (final PatternSyntaxException e) {
                errors.addFieldError(triggerEntry.getKey(), e.getLocalizedMessage());
            }
        }
    }


}
