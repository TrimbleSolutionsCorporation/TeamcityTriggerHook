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
    
    public void QueueBuild(TeamcityConfiguration conf, String branch, String buildid, String comment) {
        String url = "/app/rest/buildQueue";
        this.connector.PostPayload(conf, url, GetPayload(branch, buildid, comment));        
    }    
    
    private String GetPayload(String branch, String buildid, String comment) {
        StringBuilder builder = new StringBuilder();
        if (!"".equals(branch)) {
            builder.append(String.format("<build branchName=\"%s\">", branch));
            builder.append(String.format("<buildType id=\"%s\"/>", buildid));
            builder.append(String.format("<comment><text>%s</text></comment>", comment));
            builder.append("</build>");
        } else {
            builder.append("<build>");
            builder.append(String.format("<buildType id=\"%s\"/>", buildid));
            builder.append(String.format("<comment><text>%s</text></comment>", comment));
            builder.append("</build>");        
        }
        
        return builder.toString();
    }
}
