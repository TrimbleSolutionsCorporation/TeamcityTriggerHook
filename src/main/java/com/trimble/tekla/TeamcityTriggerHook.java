package com.trimble.tekla;

import com.atlassian.bitbucket.hook.HookResponse;
import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.repository.*;
import com.atlassian.bitbucket.scm.git.GitScm;
import com.atlassian.bitbucket.setting.*;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Note that hooks can implement RepositorySettingsValidator directly.
 */
public class TeamcityTriggerHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("TeamcityTriggerHook");
    
    private final TeamcityConnector connector;
    private GitScm gitScm;

    public  TeamcityTriggerHook(final GitScm gitScm)
    {
        this.gitScm = gitScm;
        this.connector = new TeamcityConnector(new HttpConnector());
    }    
    
    public  TeamcityTriggerHook(TeamcityConnector connector, final GitScm gitScm)
    {
        this.connector = connector;        
    }    
    
    /**
     * Connects to a configured URL to notify of all changes.
     */
    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
        boolean useQueue = context.getSettings().getString("triggerType").equals("vcs");
        
        TeamcityConfiguration conf = 
                new TeamcityConfiguration(
                        context.getSettings().getString("TeamCityUrl"),
                        context.getSettings().getString("TeamCityUserName"),
                        context.getSettings().getString("TeamCityPassword") );
        
        final Repository repository = context.getRepository();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());        
        Set<String> uniqueBranches = new LinkedHashSet<String>();                
        logger.debug("[TeamcityTriggerHook] postReceive: "  + uniqueBranches.size());
        
        // combine branchs
        for(RefChange change : refChanges) {
                                    
            if (uniqueBranches.contains(change.getRefId())) {
                continue;
            }
            
            if (change.getType().equals(RefChangeType.DELETE)) {
                System.out.println("[TeamcityTriggerHook] " + timeStamp + " Skip trigger for delete operation in branch: " + change.getRefId());
                continue;            
            }
            
            String fromChange = change.getFromHash();
            if (fromChange.startsWith("0000000000000000")) {
                
                String result = gitScm.getCommandBuilderFactory().builder(repository)
                    .command("branch")
                    .argument("--contains")
                    .argument(change.getToHash())
                    .build(new StringCommandOutputHandler())
                    .call(); 

                System.out.println("[TeamcityTriggerHook] " + timeStamp + " git branch: --contains " + change.getToHash());
                System.out.println("[TeamcityTriggerHook] " + timeStamp + " git result: '" + result + "'");
                
                String[] branches = result.trim().split("\n");
                
                if (branches.length > 1) {
                    System.out.println("[TeamcityTriggerHook] " + timeStamp + " Skip trigger no commits in branch: " + change.getRefId());
                    System.out.println("[TeamcityTriggerHook] " + timeStamp + " From Hash: " + fromChange);
                    System.out.println("[TeamcityTriggerHook] " + timeStamp + " RefChange Type: " + change.getType());
                    continue;                  
                }          
            }            
                        
            uniqueBranches.add(change.getRefId());
            this.TriggerChangesFetch(context, change.getRefId(), conf, useQueue, timeStamp);
        }
    }
           
    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        boolean areAutenticationSettingsSet = true;
        if (settings.getString("TeamCityUrl", "").isEmpty()) {
            errors.addFieldError("TeamCityUrl", "Url field is blank, please provide teamcity server address and port");
            areAutenticationSettingsSet = false;
        }

        if (settings.getString("TeamCityUserName", "").isEmpty()) {
            errors.addFieldError("TeamCityUserName", "Username needs to be defined");
            areAutenticationSettingsSet = false;
        }
        
        if (settings.getString("TeamCityPassword", "").isEmpty()) {
            errors.addFieldError("TeamCityPassword", "User Password needs to be defined");
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
        
        if (!settings.getString("ExternalBuildsOneNameId", "").isEmpty()) {
          if (settings.getString("ExternalBuildsOneDepId", "").isEmpty()) {
            errors.addFieldError("ExternalBuildsOneDepId", "At least one dependency should be set");
          }
          if (settings.getString("ExternalBuildsOneConfigurationsId", "").isEmpty()) {
            errors.addFieldError("ExternalBuildsOneConfigurationsId", "At least one configuration should be set");
          }          
        }
        
        if (!settings.getString("ExternalBuildsTwoNameId", "").isEmpty()) {
          if (settings.getString("ExternalBuildsTwoDepId", "").isEmpty()) {
            errors.addFieldError("ExternalBuildsTwoDepId", "At least one dependency should be set");
          }
          if (settings.getString("ExternalHooksConfiguration", "").isEmpty()) {
            errors.addFieldError("ExternalHooksConfiguration", "At least one hook should be set for  external hooks");
          }          
        }        
    }

    private void TriggerChangesFetch(RepositoryHookContext context, String refId, TeamcityConfiguration conf, boolean useQueue, String timestamp) {
        
        String masterVcsRoot = context.getSettings().getString("masterRule");
        if(!masterVcsRoot.isEmpty() && refId.toLowerCase().equals("refs/heads/master")) {            
            if (useQueue == false) {
                boolean isDefault = context.getSettings().getBoolean("isDefaultBranch");
                  this.QueueBuild(context, masterVcsRoot, "master", conf, timestamp, isDefault);
            } else {                
                this.TriggerWithDefinition(masterVcsRoot, conf);
            }            
        }
        
        String featureVcsRoot = context.getSettings().getString("featureRule");
        if(!featureVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/feature")) {
            if (useQueue == false) {
                this.QueueBuild(context, featureVcsRoot, refId.split("/")[3], conf, timestamp, false);
            } else {            
                this.TriggerWithDefinition(featureVcsRoot, conf);            
            }
        }
        
        String bugfixVcsRoot = context.getSettings().getString("bugFixRule");
        if(!bugfixVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/bugfix")) {
            if (useQueue == false) {
                this.QueueBuild(context, bugfixVcsRoot, refId.split("/")[3], conf, timestamp, false);
            } else {                
                this.TriggerWithDefinition(bugfixVcsRoot, conf);            
            }
        }
        
        String hotfixVcsRoot = context.getSettings().getString("hotfixRule");
        if(!hotfixVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/hotfix")) {
            if (useQueue == false) {
                this.QueueBuild(context, hotfixVcsRoot, refId.split("/")[3], conf, timestamp, false);
            } else {            
                this.TriggerWithDefinition(hotfixVcsRoot, conf);            
            }
        }
        
        String releaseVcsRoot = context.getSettings().getString("releaseRule");
        if(!releaseVcsRoot.isEmpty() && refId.toLowerCase().startsWith("refs/heads/release")) {
            if (useQueue == false) {
                this.QueueBuild(context, releaseVcsRoot, refId.split("/")[3], conf, timestamp, false);
            } else {             
                this.TriggerWithDefinition(releaseVcsRoot, conf);            
            }
        }        
        
        String branchDefinition = context.getSettings().getString("BranchDefinition");
        if(!branchDefinition.isEmpty() && this.ValidateRegx(refId, branchDefinition)) {
            if (useQueue == false) {
                this.QueueBuild(context, context.getSettings().getString("BranchCustomTypes"), refId.split("/")[3], conf, timestamp, false);
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
    
    private void QueueBuild(
            RepositoryHookContext context,
            String buildIdIn,
            String branch,
            TeamcityConfiguration conf,
            String timeStamp,
            Boolean isDefault) {
        try
        {                            
            String baseUrl = context.getSettings().getString("bitbuckerUrl");
            String comment = "remote trigger from bitbucket server : server address not specified in Bitbucket";
            
            if(!baseUrl.isEmpty()) {
                comment = "remote trigger from bitbucket server : " + baseUrl + "/branches";
            }
            
            System.out.println("[TeamcityTriggerHook] " + timeStamp + " Trigger builds for branch: " + branch);            
            for(String buildId : buildIdIn.split("\\s+")) {
                try
                {                
                    System.out.println("[TeamcityTriggerHook] Trigger BuildId: " + buildId);                    
                    this.connector.QueueBuild(conf, branch, buildId, comment, isDefault);
                } catch (Exception e) {
                    System.out.println("[TeamcityTriggerHook] BuildId: " + buildId + " Failed");
                }                     
            }                
        } catch (Exception e) {
            System.out.println("[TeamcityTriggerHook] Invalid build configuration data: " + buildIdIn);
        }  
    }    

}
