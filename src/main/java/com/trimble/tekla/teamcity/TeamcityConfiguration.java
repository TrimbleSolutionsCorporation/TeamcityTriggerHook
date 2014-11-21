/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla.teamcity;

public class TeamcityConfiguration
{
    private final String url;
    private final String user;
    private final String pass;

    public TeamcityConfiguration(String host, String user, String pass) {
        this.url = host;
        this.user = user;
        this.pass = pass;
    }

    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return user;
    }

    public String getPassWord() {
        return pass;
    }
}

