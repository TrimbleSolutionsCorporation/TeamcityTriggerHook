package ut.com.trimble.tekla;

import com.trimble.tekla.TeamcityTriggerHook;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}