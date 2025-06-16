package ut.com.trimble.tekla.teamcity;

import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TeamcityConnectorTest
{
    //@Test
    // public void TriggetMasterBranchTest()
    // {
    //     TeamcityConfiguration conf = new TeamcityConfiguration("host", "user", "pass");
    //     HttpConnector connector = mock(HttpConnector.class);
    //     TeamcityConnector component = new TeamcityConnector(connector);
    //     component.TriggerCheckForChanges(conf, "vcsRootForRepo", null, "repo");        
    //     verify(connector).Post(conf, "/app/rest/debug/vcsCheckingForChangesQueue?locator=vcsRoot:vcsRootForRepo", null, null, "repo");
    // }
}