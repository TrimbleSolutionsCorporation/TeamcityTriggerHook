package it.com.trimble.tekla;

import org.junit.Test;
import org.junit.runner.RunWith;
import com.atlassian.plugins.osgi.test.AtlassianPluginsTestRunner;
import com.trimble.tekla.TeamcityTriggerHook;
import com.atlassian.sal.api.ApplicationProperties;

import static org.junit.Assert.assertEquals;

@RunWith(AtlassianPluginsTestRunner.class)
public class TeamcityTriggerHookITTest
{
    private final ApplicationProperties applicationProperties;
    private final TeamcityTriggerHook myPluginComponent;

    public TeamcityTriggerHookITTest(ApplicationProperties applicationProperties, TeamcityTriggerHook myPluginComponent)
    {
        this.applicationProperties = applicationProperties;
        this.myPluginComponent = myPluginComponent;
    }

    @Test
    public void testMyName()
    {
        assertEquals("names do not match!", "myComponent:" + applicationProperties.getDisplayName());
    }
}