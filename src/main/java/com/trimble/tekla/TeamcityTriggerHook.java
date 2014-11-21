package com.trimble.tekla;

import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.repository.*;
import com.atlassian.stash.setting.*;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Note that hooks can implement RepositorySettingsValidator directly.
 */
public class TeamcityTriggerHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {
    
    private final TeamcityConnector connector;

    public  TeamcityTriggerHook()
    {
        this.connector = new TeamcityConnector(new HttpConnector());
    }
    
    public  TeamcityTriggerHook(TeamcityConnector connector)
    {
        this.connector = connector;        
    }    
    
    /**
     * Connects to a configured URL to notify of all changes.
     */
    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
        TeamcityConfiguration conf = 
                new TeamcityConfiguration(
                        context.getSettings().getString("url"),
                        context.getSettings().getString("username"),
                        context.getSettings().getString("password") );
        
        Set<String> uniqueBranches = new LinkedHashSet<String>();
        
        // combine branchs
        for(RefChange change : refChanges) {
            if (uniqueBranches.contains(change.getRefId())) {
                continue;
            }
            
            uniqueBranches.add(change.getRefId());
            this.TriggerChangesFetch(context, change.getRefId(), conf);
        }
    }
           
    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        boolean areAutenticationSettingsSet = true;
        if (settings.getString("url", "").isEmpty()) {
            errors.addFieldError("url", "Url field is blank, please provide teamcity server address and port");
            areAutenticationSettingsSet = false;
        }

        if (settings.getString("username", "").isEmpty()) {
            errors.addFieldError("username", "Username needs to be defined");
            areAutenticationSettingsSet = false;
        }
        
        if (settings.getString("password", "").isEmpty()) {
            errors.addFieldError("password", "User Password needs to be defined");
            areAutenticationSettingsSet = false;
        }        
        
        if ( !areAutenticationSettingsSet) {
            return;
        }
        
        if (settings.getString("masterRule", "").isEmpty() && settings.getString("bugFixRule", "").isEmpty() && settings.getString("featureRule", "").isEmpty()) {

            errors.addFieldError("masterRule", "At least on configuration should be set");
            errors.addFieldError("bugFixRule", "At least on configuration should be set");
            errors.addFieldError("featureRule", "At least on configuration should be set");
        }                 
    }

    private void TriggerChangesFetch(RepositoryHookContext context, String refId, TeamcityConfiguration conf) {
        
        String masterVcsRoot = context.getSettings().getString("masterRule");
        if(!masterVcsRoot.isEmpty() && refId.toLowerCase().equals("refs/heads/master")) {
            this.TriggerWithDefinition(masterVcsRoot, conf);            
        }
        
        String featureVcsRoot = context.getSettings().getString("featureRule");
        if(!featureVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/feature")) {
            this.TriggerWithDefinition(featureVcsRoot, conf);            
        }
        
        String bugfixVcsRoot = context.getSettings().getString("bugFixRule");
        if(!bugfixVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/bugfix")) {
            this.TriggerWithDefinition(bugfixVcsRoot, conf);            
        }
        
        String hotfixVcsRoot = context.getSettings().getString("hotfixRule");
        if(!hotfixVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/hotfix")) {
            this.TriggerWithDefinition(hotfixVcsRoot, conf);            
        }
        
        String releaseVcsRoot = context.getSettings().getString("releaseRule");
        if(!releaseVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/release")) {
            this.TriggerWithDefinition(releaseVcsRoot, conf);            
        }        
        
        String branchDefinition = context.getSettings().getString("BranchDefinition");
        if(!branchDefinition.isEmpty() && this.ValidateRegx(refId, branchDefinition)) {
            this.TriggerWithDefinition(context.getSettings().getString("BranchCustomTypes"), conf);            
        }        
    }
    
    public boolean ValidateRegx(String data, String regx)
    {	
        if(data.toLowerCase().equals("refs/heads/master")) {
            return false;           
        }
        
        if(data.toLowerCase().startsWith("refs/heads/feature")) {
            return false;
        }
                
        if(data.toLowerCase().startsWith("refs/heads/bugfix")) {
            return false;
        }
        
        if(data.toLowerCase().startsWith("refs/heads/hotfix")) {
            return false;
        }
        
        if(data.toLowerCase().startsWith("refs/heads/release")) {
            return false;
        }
                                
        Pattern pattern = Pattern.compile(regx);
	Matcher matcher = pattern.matcher(data);                
        return matcher.matches();
    }

    private void TriggerWithDefinition(String definition, TeamcityConfiguration conf) {
        try
        {                
            for(String vcsRoot : definition.split("\\s+")) {
                try
                {                
                    this.connector.TriggerCheckForChanges(conf, definition);
                } catch (Exception e) {
                    e.printStackTrace();
                }                     
            }                
        } catch (Exception e) {
            e.printStackTrace();
        }  
    }
}
