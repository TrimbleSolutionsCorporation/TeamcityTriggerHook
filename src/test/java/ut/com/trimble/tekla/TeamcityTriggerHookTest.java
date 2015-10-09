package ut.com.trimble.tekla;

import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.setting.Settings;
import com.trimble.tekla.TeamcityTriggerHook;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TeamcityTriggerHookTest
{
    @Test
    public void validaFailsRegx()
    {
        TeamcityTriggerHook component = new TeamcityTriggerHook(null);
        assertFalse(component.ValidateRegx("refs/heads/14.0", ""));
        assertFalse(component.ValidateRegx("refs/heads/14.0", "feature"));
        assertFalse(component.ValidateRegx("refs/heads/14.0", "bugfix"));
        assertFalse(component.ValidateRegx("refs/heads/14.0", "data"));
        assertFalse(component.ValidateRegx("refs/heads/14.0", "refs/heads"));
        assertFalse(component.ValidateRegx("refs/heads/master", "refs/heads/[0-9.]*"));
        assertFalse(component.ValidateRegx("refs/heads/bugfix", "refs/heads/[0-9.]*"));
        assertFalse(component.ValidateRegx("refs/heads/feature", "refs/heads/[0-9.]*"));
        assertFalse(component.ValidateRegx("refs/heads/hotfix", "refs/heads/[0-9.]*"));
        assertFalse(component.ValidateRegx("refs/heads/release", "refs/heads/[0-9.]*"));
        
        assertFalse(component.ValidateRegx("refs/heads/release", "refs/heads/[a-z.]*"));
        assertFalse(component.ValidateRegx("refs/heads/hotfix", "refs/heads/[a-z.]*"));
        assertFalse(component.ValidateRegx("refs/heads/feature", "refs/heads/[a-z.]*"));
        assertFalse(component.ValidateRegx("refs/heads/master", "refs/heads/[a-z.]*"));
        assertFalse(component.ValidateRegx("refs/heads/bugfix", "refs/heads/[a-z.]*"));
    }
    
    @Test
    public void validaRegx()
    {
        TeamcityTriggerHook component = new TeamcityTriggerHook(null);
        assertTrue(component.ValidateRegx("refs/heads/14.0", "refs/heads/[0-9.]*"));        
    }    
    
    @Test
    public void ShouldNotTriggerABuildFromMasterTestIfNotDefined()
    {
        RefChange change = mock(RefChange.class);
        when(change.getRefId()).thenReturn("refs/heads/master");
        Collection<RefChange> refChanges = new ArrayList<RefChange>();
        refChanges.add(change);
                        
        Settings settings = mock(Settings.class);
        when(settings.getString("masterRule")).thenReturn("");
        when(settings.getString("featureRule")).thenReturn("");
        when(settings.getString("bugFixRule")).thenReturn("");
        when(settings.getString("hotfixRule")).thenReturn("");
        when(settings.getString("releaseRule")).thenReturn(""); 
        when(settings.getString("BranchDefinition")).thenReturn("");
        RepositoryHookContext context = mock(RepositoryHookContext.class);
        when(context.getSettings()).thenReturn(settings);        
        TeamcityConnector connector = mock(TeamcityConnector.class);                
        TeamcityTriggerHook component = new TeamcityTriggerHook(connector);        
        component.postReceive(context, refChanges);
        verify(connector, never()).TriggerCheckForChanges(any(TeamcityConfiguration.class), any(String.class));
    }     
    
    
    @Test
    public void ShouldTriggerABuildFromMasterTestSingleRepo()
    {
        RefChange change = mock(RefChange.class);
        when(change.getRefId()).thenReturn("refs/heads/master");
        Collection<RefChange> refChanges = new ArrayList<RefChange>();
        refChanges.add(change);
                        
        Settings settings = mock(Settings.class);
        when(settings.getString("masterRule")).thenReturn("vcsroot");
        when(settings.getString("featureRule")).thenReturn("");
        when(settings.getString("bugFixRule")).thenReturn("");
        when(settings.getString("hotfixRule")).thenReturn("");
        when(settings.getString("releaseRule")).thenReturn("");   
        when(settings.getString("BranchDefinition")).thenReturn("");
        RepositoryHookContext context = mock(RepositoryHookContext.class);
        when(context.getSettings()).thenReturn(settings);
        
        TeamcityConnector connector = mock(TeamcityConnector.class);                
        TeamcityTriggerHook component = new TeamcityTriggerHook(connector);        
        component.postReceive(context, refChanges);
        verify(connector).TriggerCheckForChanges(any(TeamcityConfiguration.class), eq("vcsroot"));
    }     
    
    @Test
    public void ShouldTriggerABuildFromMasterTestMultipleRepo()
    {
        RefChange change = mock(RefChange.class);
        when(change.getRefId()).thenReturn("refs/heads/master");
        Collection<RefChange> refChanges = new ArrayList<RefChange>();
        refChanges.add(change);
                        
        Settings settings = mock(Settings.class);
        when(settings.getString("masterRule")).thenReturn("vcsroot vcsroot1");
        when(settings.getString("featureRule")).thenReturn("");
        when(settings.getString("bugFixRule")).thenReturn("");
        when(settings.getString("hotfixRule")).thenReturn("");
        when(settings.getString("releaseRule")).thenReturn("");   
        when(settings.getString("BranchDefinition")).thenReturn("");
        RepositoryHookContext context = mock(RepositoryHookContext.class);
        when(context.getSettings()).thenReturn(settings);
        
        TeamcityConnector connector = mock(TeamcityConnector.class);                
        TeamcityTriggerHook component = new TeamcityTriggerHook(connector);        
        component.postReceive(context, refChanges);
        verify(connector).TriggerCheckForChanges(any(TeamcityConfiguration.class), eq("vcsroot"));
        verify(connector).TriggerCheckForChanges(any(TeamcityConfiguration.class), eq("vcsroot1"));
    }      
}