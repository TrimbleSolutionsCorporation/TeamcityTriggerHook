package com.trimble.tekla;

import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.repository.*;
import com.atlassian.bitbucket.setting.*;
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
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("StashTeamcityHook-TeamcityTriggerHook");
    
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
        boolean useQueue = context.getSettings().getBoolean("useQueue", false);
        TeamcityConfiguration conf = 
                new TeamcityConfiguration(
                        context.getSettings().getString("url"),
                        context.getSettings().getString("username"),
                        context.getSettings().getString("password") );
        
        Set<String> uniqueBranches = new LinkedHashSet<String>();                
        logger.debug("[TeamcityTriggerHook] postReceive: "  + uniqueBranches.size() + " using queue trigger: " + useQueue);
        
        // combine branchs
        for(RefChange change : refChanges) {
                                    
            if (uniqueBranches.contains(change.getRefId())) {
                continue;
            }
            
            if (change.getType().equals(RefChangeType.DELETE)) {
                System.out.println("[TeamcityTriggerHook] Skip trigger for delete operation in branch: " + change.getRefId());
                continue;            
            }
            
            String fromChange = change.getFromHash();
            if (fromChange.startsWith("0000000000000000")) {
                System.out.println("[TeamcityTriggerHook] Skip trigger no commits in branch: " + change.getRefId());
                continue;            
            }            
                        
            uniqueBranches.add(change.getRefId());
            this.TriggerChangesFetch(context, change.getRefId(), conf, useQueue);
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

    private void TriggerChangesFetch(RepositoryHookContext context, String refId, TeamcityConfiguration conf, boolean useQueue) {
        
        String masterVcsRoot = context.getSettings().getString("masterRule");
        if(!masterVcsRoot.isEmpty() && refId.toLowerCase().equals("refs/heads/master")) {
            if (useQueue) {
                this.QueueBuild(context, masterVcsRoot, "master", conf);
            } else {
                this.TriggerWithDefinition(masterVcsRoot, conf);
            }            
        }
        
        String featureVcsRoot = context.getSettings().getString("featureRule");
        if(!featureVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/feature")) {
            if (useQueue) {
                this.QueueBuild(context, featureVcsRoot, refId.split("/")[3], conf);
            } else {            
                this.TriggerWithDefinition(featureVcsRoot, conf);            
            }
        }
        
        String bugfixVcsRoot = context.getSettings().getString("bugFixRule");
        if(!bugfixVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/bugfix")) {
            if (useQueue) {
                this.QueueBuild(context, bugfixVcsRoot, refId.split("/")[3], conf);
            } else {                
                this.TriggerWithDefinition(bugfixVcsRoot, conf);            
            }
        }
        
        String hotfixVcsRoot = context.getSettings().getString("hotfixRule");
        if(!hotfixVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/hotfix")) {
            if (useQueue) {
                this.QueueBuild(context, hotfixVcsRoot, refId.split("/")[3], conf);
            } else {            
                this.TriggerWithDefinition(hotfixVcsRoot, conf);            
            }
        }
        
        String releaseVcsRoot = context.getSettings().getString("releaseRule");
        if(!releaseVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/release")) {
            if (useQueue) {
                this.QueueBuild(context, releaseVcsRoot, refId.split("/")[3], conf);
            } else {             
                this.TriggerWithDefinition(releaseVcsRoot, conf);            
            }
        }        
        
        String branchDefinition = context.getSettings().getString("BranchDefinition");
        if(!branchDefinition.isEmpty() && this.ValidateRegx(refId, branchDefinition)) {
            if (useQueue) {
                this.QueueBuild(context, context.getSettings().getString("BranchCustomTypes"), refId.split("/")[3], conf);
            } else {              
                this.TriggerWithDefinition(context.getSettings().getString("BranchCustomTypes"), conf);            
            }
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
                    System.out.println("[TeamcityTriggerHook] Trigger Check for Changes in: " + vcsRoot);
                    this.connector.TriggerCheckForChanges(conf, vcsRoot);
                } catch (Exception e) {
                    System.out.println("[TeamcityTriggerHook] Trigger Check for Changes in: " + vcsRoot + " Failed");
                }                     
            }                
        } catch (Exception e) {
            System.out.println("[TeamcityTriggerHook] Invalid vcs configuration data: " + definition);
        }  
    }
    
    private void QueueBuild(RepositoryHookContext context, String buildIdIn, String branch, TeamcityConfiguration conf) {
        try
        {                            
            String baseUrl = context.getSettings().getString("bitbuckerUrl");
            String comment = "remote trigger from bitbucket server : server address not specified in Bitbucket";
            
            if(!baseUrl.isEmpty()) {
                comment = "remote trigger from bitbucket server : " + baseUrl + "/branches";
            }
            
            System.out.println("[TeamcityTriggerHook] Trigger builds for branch: " + branch);            
            for(String buildId : buildIdIn.split("\\s+")) {
                try
                {                
                    System.out.println("[TeamcityTriggerHook] Trigger BuildId: " + buildId);                    
                    this.connector.QueueBuild(conf, branch, buildId, comment);
                } catch (Exception e) {
                    System.out.println("[TeamcityTriggerHook] BuildId: " + buildId + " Failed");
                }                     
            }                
        } catch (Exception e) {
            System.out.println("[TeamcityTriggerHook] Invalid build configuration data: " + buildIdIn);
        }  
    }    
}
