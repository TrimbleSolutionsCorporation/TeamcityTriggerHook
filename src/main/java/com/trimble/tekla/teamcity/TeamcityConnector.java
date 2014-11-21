/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.teamcity;
/**
 *
 * @author jocs
 */
public class TeamcityConnector  {
    public final HttpConnector connector;
        
    public TeamcityConnector(HttpConnector connector) {
     this.connector = connector;     
    }
    
    public void TriggerCheckForChanges(TeamcityConfiguration conf, String vcsRoot) {
        String url = "/app/rest/debug/vcsCheckingForChangesQueue?locator=vcsRoot:"  + vcsRoot;
        this.connector.Post(conf, url, null);        
    }
    
}
