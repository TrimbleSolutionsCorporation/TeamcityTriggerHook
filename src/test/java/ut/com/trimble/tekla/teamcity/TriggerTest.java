/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ut.com.trimble.tekla.teamcity;

import com.trimble.tekla.helpers.ExclusionTriggers;
import com.trimble.tekla.pojo.Trigger;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import java.io.IOException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 * @author jocs
 */
public class TriggerTest {
    @Test
    public void TriggetMasterBranchTest() throws IOException
    {
      String jsonHook = "{    \"7032ca56-48e3-4119-b9f5-4fec896fbf37\": {\n" +
"        \"regex\": \"feature/(.*)\",\n" +
"        \"target\": \"Structures_Features_x64Debug\",\n" +
"        \"type\": \"build\",\n" +
"        \"triggerOnEmptyBranches\": false,\n" +
"        \"triggerOnPullRequest\": true,\n" +
"        \"cancelRunningBuilds\": true,\n" +
"        \"triggerInclusion\": \"\",\n" +
"        \"triggerExclusion\": \"Test/FarmiTests\",\n" +
"        \"downStreamTriggerType\": \"build\",\n" +
"        \"downStreamTriggerTarget\": \"\"\n" +
"    }}";
      
      Trigger [] builds = Trigger.GetBuildConfigurationsFromBranch(jsonHook, "refs/heads/feature/TTSD-14832-handle-obsolete-properties-better");
      assertEquals(1, builds.length);
      assertEquals("TTSD-14832-handle-obsolete-properties-better", builds[0].getBranchConfig());
      assertEquals(1, Trigger.GetBuildConfigurationsFromBranch(jsonHook, "feature/TTSD-14832-handle-obsolete-properties-better").length);
      assertEquals(0, Trigger.GetBuildConfigurationsFromBranch(jsonHook, "bugfix/TTSD-14832-handle-obsolete-properties-better").length);
      assertEquals(0, Trigger.GetBuildConfigurationsFromBranch(jsonHook, "refs/heads/bugfix/TTSD-14832-handle-obsolete-properties-better").length);
    }
}
