/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.pojo;

/**
 *
 * @author jocs
 */
public class TeamcityQueuedElement {

  private String branch;
  private String buildType;
  private String webUrl;
  private String id;
  
  public String getBranch() {
    return this.branch;
  }

  public void setBranch(final String branch) {
    this.branch = branch;
  }

  public String getId() {
    return this.id;
  }

  public String getWebUrl() {
    return this.webUrl;
  }

  public String getBuildType() {
    return this.buildType;
  }
  
  public void setId(String id) {
    this.id = id;
  }

  public void setWebUrl(String webUrl) {
    this.webUrl = webUrl;
  }

  public void setBuildType(String buildType) {
    this.buildType = buildType;
  }
}
