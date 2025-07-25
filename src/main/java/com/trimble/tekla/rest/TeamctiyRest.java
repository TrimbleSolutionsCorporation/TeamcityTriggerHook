package com.trimble.tekla.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.rest.v2.api.resolver.RepositoryResolver;
import com.atlassian.bitbucket.rest.v2.api.util.ResourcePatterns;
import com.atlassian.bitbucket.setting.Settings;
import com.trimble.tekla.Constant;
import com.trimble.tekla.Field;
import com.trimble.tekla.SettingsService;
import com.trimble.tekla.TeamcityConnectionSettings;
import com.trimble.tekla.pojo.Trigger;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import static com.atlassian.plugins.osgi.javaconfig.OsgiServices.importOsgiService;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BeanParam;

import com.atlassian.bitbucket.hook.repository.RepositoryHookService;

/**
 * REST configuration
 */
@Path(ResourcePatterns.REPOSITORY_URI)
public class TeamctiyRest {
  
  //private final TeamcityConnector connector;
  //private final SettingsService settingsService;
  //private final TeamcityConnectionSettings connectionSettings;
  //private final AuthenticationContext authContext;
  //private final PullRequestService pullRequestService;

  /**
   * Creates Rest resource for testing the Jenkins configuration
   *
   * @param i18nService i18n Service
   */
  //@Inject
  //public TeamctiyRest(final SettingsService settingsService,
  //                    final TeamcityConnectionSettings connectionSettings,
  //                    final AuthenticationContext authContext,
  //                    final PullRequestService pullRequestService) {
  //  this.connectionSettings = connectionSettings;
  //  this.settingsService = settingsService;
  //  this.authContext = authContext;
  //  this.pullRequestService = pullRequestService;
  //  this.connector = new TeamcityConnector(new HttpConnector());
  //}

  /**
   * Test Hello
   * curl --verbose --silent --user admin:admin --request GET --url 'http://localhost:7990/bitbucket/rest/teamcity/latest/projects/PROJECT_1/repos/rep_1/hello' --header 'Accept: application/json' --header 'X-Atlassian-Token: no-check'
   */    
  @GET
  @Path("/hello")
  @Produces(MediaType.APPLICATION_JSON)
  public Response doGet() throws Exception {
      return Response.ok()
              .entity("{\"hello\":\"world\"}")
              .type(MediaType.APPLICATION_JSON)
              .build();
  }

  /**
   * Trigger a build on the Teamcity instance using vcs root
   * curl --verbose --silent --user admin:admin --request GET --url 'http://localhost:7990/bitbucket/rest/teamcity/latest/projects/PROJECT_1/repos/rep_1/loadhtml?page=index.js'
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "loadhtml")
  @Produces(MediaType.TEXT_HTML)
  public String loadhtml(@BeanParam final RepositoryResolver repositoryResolver, @QueryParam("page") final String page) {

    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    return file;
  }

  /**
   * Trigger a build on the Teamcity instance using vcs root
   * curl --verbose --silent --user admin:admin --request GET --url 'http://localhost:7990/bitbucket/rest/teamcity/latest/projects/PROJECT_1/repos/rep_1/loadjs?page=index.js'
   * @param repository The repository to trigger
     * @param page
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "loadjs")
  @Produces("text/javascript")
  public String loadjs(@BeanParam final RepositoryResolver repositoryResolver, @QueryParam("page") final String page) {
    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    return file;
  }

  @GET
  @Path(value = "loadcss")
  @Produces("text/css")
  public String loadcss(@BeanParam final RepositoryResolver repositoryResolver, @QueryParam("page") final String page) {
    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    return file;
  }

  @GET
  @Path(value = "loadimg")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response loadimg(@BeanParam final RepositoryResolver repositoryResolver, @QueryParam("img") final String img) {
    return Response.ok(getResourceAsFile("public/" + img), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=\"" + img + "\"").build();
  }

  public static File getResourceAsFile(final String resourcePath) {
    try {
      final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
      final InputStream in = classloader.getResourceAsStream(resourcePath);

      if (in == null) {
        return null;
      }

      final File tempFile = File.createTempFile(String.valueOf(in.hashCode()), ".tmp");
      tempFile.deleteOnExit();

      try (FileOutputStream out = new FileOutputStream(tempFile)) {
        // copy stream
        final byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
          out.write(buffer, 0, bytesRead);
        }
      }
      return tempFile;
    } catch (final IOException e) {
      return null;
    }
  }

  static String convertStreamToString(final java.io.InputStream is) {
    final java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  @GET
  @Path(value = "triggerbuild")
  @Produces(MediaType.APPLICATION_JSON)
  public String triggerBuild(
          @BeanParam final RepositoryResolver repositoryResolver,
          @BeanParam final SettingsService settingsService,
          @BeanParam final TeamcityConnectionSettings connectionSettings,
          @QueryParam("buildconfig") final String buildconfig,
          @QueryParam("branch") final String branch,
          @QueryParam("prid") final String prid) throws IOException {

    final Optional<Settings> settings = settingsService.getSettings(repositoryResolver.getRepository());

    if (settings == null) {
      return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
    }

    final String url = settings.get().getString(Field.TEAMCITY_URL, "");
    final String username = settings.get().getString(Field.TEAMCITY_USERNAME, "");
    final String password = connectionSettings.getPassword(repositoryResolver.getRepository());

    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }

    final String repositoryTriggersJson = settings.get().getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
    if (repositoryTriggersJson.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"hook not configured properly\"}";
    }

    final Trigger[] configurations = Trigger.GetBuildConfigurationsFromBranch(repositoryTriggersJson, branch);

    if (configurations.length == 0) {
      return "{\"status\": \"error\", \"message\": \"no build configurations defined for this branch\"}";
    }
        
    final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
    
    StringBuilder builder = new StringBuilder();
    TeamcityConnector connector = new TeamcityConnector(new HttpConnector());
    builder.append("ok will trigger: ").append(configurations.length);
            
    for (final Trigger buildConfig : configurations) {
      String []donwstramTriggers = buildConfig.getDownStreamTriggerTarget().split(",");
      if (buildConfig.getTarget().equals(buildconfig) || Arrays.asList(donwstramTriggers).contains(buildconfig)) {
        builder.append(" trigger ")
                .append(buildconfig)
                .append(" ")
                .append(buildConfig.getBranchConfig());
        builder.append(" ")
                .append(connector.QueueBuild(conf,
                                              buildConfig.getBranchConfig(),
                                              buildconfig,
                                              "Manual Trigger from Bitbucket: Pull Request: " + prid,
                                              false,
                                              settings.get(),
                                              repositoryResolver.getRepository().getName(), null)); // handle error todo
      } else {
        builder.append(" trigger skipped ")
                .append(buildconfig)
                .append(" different than ")
                .append(buildConfig.getTarget())
                .append(" for branch config ")
                .append(buildConfig.getBranchConfig());
      }    
    }
    return "{\"status\": \"" + builder.toString() + "\" }";
  }

  @GET
  @Path(value = "builds")
  @Produces(MediaType.APPLICATION_JSON)
  public String getBuildsConfiguration(
          @BeanParam final RepositoryResolver repositoryResolver,
          @BeanParam final TeamcityConnectionSettings connectionSettings,
          @BeanParam final SettingsService settingsService,
          @QueryParam("prid") final String prid,
          @QueryParam("branch") final String branch,
          @QueryParam("hash") final String hash) throws IOException {

    final Optional<Settings> settings = settingsService.getSettings(repositoryResolver.getRepository());
    if(!settings.isPresent()) {
      return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
    }

    final String url = settings.get().getString(Field.TEAMCITY_URL, "");
    final String username = settings.get().getString("teamCityUserName", "");
    final String password = connectionSettings.getPassword(repositoryResolver.getRepository());

    if (password.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"password is empty\"}";
    }

    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }

    final String repositoryTriggersJson = settings.get().getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
    if (repositoryTriggersJson.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"hook not configured properly\"}";
    }

    final Trigger[] configurations = Trigger.GetBuildConfigurationsFromBranch(repositoryTriggersJson, branch);

    if (configurations.length == 0) {
      return "{\"status\": \"error\", \"message\": \"no build configurations defined for this branch\"}";
    }

    final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
    try {

      final JSONObject jObj = new JSONObject();

      for (final Trigger buildConfig : configurations) {
        if(buildConfig.isHideOnPullRequest()) {
          continue;
        }
        try {
          TeamcityConnector connector = new TeamcityConnector(new HttpConnector());
          final String returnData = connector.GetBuildsForBranch(conf, buildConfig.getBranchConfig(), buildConfig.getTarget(), settings.get(), repositoryResolver.getRepository().getName(), false);
          final String queueData = connector.GetQueueDataForConfiguration(conf, buildConfig.getTarget(), settings.get(), repositoryResolver.getRepository().getName());
          jObj.put(buildConfig.getTarget(), returnData);
          jObj.put(buildConfig.getTarget() + "_queue", queueData);
          jObj.put(buildConfig.getTarget() + "_wref", url + "/viewType.html?buildTypeId=" + buildConfig);

        } catch (final IOException ex) {
          jObj.put(buildConfig.getTarget(), "{\"exception\": \"Build Id for configuration throw exception\"}");
        }
      }

      return jObj.toString();
    } catch (final JSONException ex) {
      return "{\"status\": \"error\", \"message\": \"cannot parse json from teamcity" + ex.getMessage() + " \"}";
    }
  }

  @GET
  @Path(value = "externalbuilds")
  public String getExternalConfiguration(
          @BeanParam final RepositoryResolver repositoryResolver,
          @BeanParam final SettingsService settingsService,
          @BeanParam final TeamcityConnectionSettings connectionSettings,
          @QueryParam("id") final String id,
          @QueryParam("prid") final String prid,
          @QueryParam("branch") final String branch,
          @QueryParam("hash") final String hash) {

    
    final Optional<Settings> settings = settingsService.getSettings(repositoryResolver.getRepository());

    if (!settings.isPresent()) {
      return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
    }

    final String url = settings.get().getString(Field.TEAMCITY_URL, "");
    final String username = settings.get().getString("teamCityUserName", "");
    final String password = connectionSettings.getPassword(repositoryResolver.getRepository());

    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }

    if (password.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"password is empty\"}";
    }

    final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
    try {
      final String repositoryTriggersJson = settings.get().getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
      if (repositoryTriggersJson.isEmpty()) {
        return "{\"status\": \"error\", \"message\": \"hook not configured properly\"}";
      }


      if ("External1Id".equals(id)) {
        final JSONObject jObj = new JSONObject();
        jObj.put("ExternalBuildsOneNameId", "{\"status\": \"ok\", \"name\": \"Tests\"}");
        final Trigger[] configurations = Trigger.GetBuildConfigurationsFromBranch(repositoryTriggersJson, branch);
        for (final Trigger buildConfig : configurations) {
          if ("build".equals(buildConfig.getDownStreamTriggerType()) && !"".equals(buildConfig.getDownStreamTriggerTarget())) {
            final String depBuildId = buildConfig.getTarget();
            TeamcityConnector connector = new TeamcityConnector(new HttpConnector());
            final String returnData = connector.GetBuildsForBranch(conf, buildConfig.getBranchConfig(), depBuildId, settings.get(), repositoryResolver.getRepository().getName(), false);
            final String queueData = connector.GetQueueDataForConfiguration(conf, depBuildId, settings.get(), repositoryResolver.getRepository().getName());
            jObj.put(depBuildId + "_dep_wref", url + "/viewType.html?buildTypeId=" + depBuildId);
            jObj.put(depBuildId + "_dep", returnData);
            jObj.put(depBuildId + "_dep_queue", queueData);

            final String [] downBuildIds = buildConfig.getDownStreamTriggerTarget().split(",");           
            for (String downBuildId : downBuildIds) {
                final String returnDataBuildDep = connector.GetBuildsForBranch(conf, buildConfig.getBranchConfig(), downBuildId, settings.get(), repositoryResolver.getRepository().getName(), false);
                final String queueDataBuildDep = connector.GetQueueDataForConfiguration(conf, downBuildId, settings.get(), repositoryResolver.getRepository().getName());
                jObj.put(downBuildId + "_build", returnDataBuildDep);
                jObj.put(downBuildId + "_build_branch", buildConfig.getBranchConfig());                
                jObj.put(downBuildId + "_build_wref", url + "/viewType.html?buildTypeId=" + downBuildId);
                jObj.put(downBuildId + "_build_queue", queueDataBuildDep);            
            }
          }
        }
        return jObj.toString();
      } else if ("External2Id".equals(id)) {
        final JSONObject jObj = new JSONObject();
        jObj.put("ExternalBuildsTwoNameId", "{\"status\": \"ok\", \"name\": \"External Triggers\"}");
        final JSONArray extRef = new JSONArray();
        final Trigger[] configurations = Trigger.GetBuildConfigurationsFromBranch(repositoryTriggersJson, branch);
        for (final Trigger buildConfig : configurations) {
          if ("rest".equals(buildConfig.getDownStreamTriggerType()) ||
              "tab".equals(buildConfig.getDownStreamTriggerType()) && !"".equals(buildConfig.getDownStreamTriggerTarget())) {
            final String depBuildId = buildConfig.getTarget();
            TeamcityConnector connector = new TeamcityConnector(new HttpConnector());
            
            final String returnData = connector.GetBuildsForBranch(conf, buildConfig.getBranchConfig(), depBuildId, settings.get(), repositoryResolver.getRepository().getName(), false);
            final String queueData = connector.GetQueueDataForConfiguration(conf, depBuildId, settings.get(), repositoryResolver.getRepository().getName());
            jObj.put(depBuildId + "_dep", returnData);
            jObj.put(depBuildId + "_dep_wref", url + "/viewType.html?buildTypeId=" + depBuildId);
            jObj.put(depBuildId + "_dep_queue", queueData);

            final String downBuildId = buildConfig.getDownStreamTriggerTarget();            
            // external trigger configuration
            final JSONObject build = new JSONObject();
            build.put("type", buildConfig.getDownStreamTriggerType());
            if ("".equals(buildConfig.getDownStreamTriggerDescription())) {
              build.put("desc", "Trigger with description not provided."); 
            } else {
              build.put("desc", buildConfig.getDownStreamTriggerDescription()); 
            }
            build.put("url", downBuildId);
            build.put("dependencies", depBuildId);
            build.put("source", branch);
            extRef.put(build.toString());

            // add external trigger
            jObj.put("ext_references", extRef.toString());
          }
        }
        return jObj.toString();
      } else {
        return "{\"status\": \"error\", \"message\": \"invalid id\"}";
      }

    } catch (final JSONException ex) {
      return "{\"status\": \"error\", \"message\": \"request throw exception" + ex.getMessage() + " \"}";
    } catch (final IOException ex) {
      return "{\"status\": \"error\", \"message\": \"request throw exception" + ex.getMessage() + " \"}";
    }
  }

  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "triggerexternalurl")
  public String triggerexternalurl(
    @BeanParam final RepositoryResolver repositoryResolver,
    @BeanParam final SettingsService settingsService,
    @QueryParam("url") final String url,
    @QueryParam("method") final String method) {

    final HttpConnector dummyConnector = new HttpConnector();
    String returnData;
    try {
      final Optional<Settings> settings = settingsService.getSettings(repositoryResolver.getRepository());

      if (!settings.isPresent()) {
        return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
      }
    
      returnData = dummyConnector.Get(url, settings.get(), repositoryResolver.getRepository().getName());
      return "{\"status\": \"ok\", \"message\": \" " + returnData + "\" }";
    } catch (final IOException ex) {
      return "{\"status\": \"failed\", \"message\": \" " + ex.getMessage() + "\" }";
    }
  }

   /*
    * test with 
    curl --verbose --silent --user admin:admin --request GET --url 'http://localhost:7990/bitbucket/rest/teamcity/latest/projects/PROJECT_1/repos/rep_1/testconnection?url=asdas&username=asdas&password=asdas&debugon=asdas' --header 'X-Atlassian-Token: no-check'
    */
  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repositoryResolver - {@link Repository}
   * @param url - url to TeamCity server
   * @param username - TeamCity user name
   * @param password - TeamCity user password
   * @return "OK" if it worked. Otherwise, an error message.
   */
  //@GET
  //@Path(value = "testconnection")
  //@Produces("text/plain; charset=UTF-8")
  //public Response testconnection(
  //        @BeanParam  RepositoryResolver repositoryResolver, 
  //        @QueryParam("url") final String url,
  //        @QueryParam("username") final String username,
  //        @QueryParam("password") final String password,
  //        @QueryParam("debugon") final String isDebugOn) {
//
  //      
  //      return Response.ok()
  //              .entity(repositoryResolver.getRepository().getName())
  //              .type(MediaType.APPLICATION_JSON)
  //              .build();
  //}  

  
  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repositoryResolver - {@link Repository}
   * @param url - url to TeamCity server
   * @param username - TeamCity user name
   * @param password - TeamCity user password
   * @return "OK" if it worked. Otherwise, an error message.
   */
  @GET
  @Path(value = "testconnection")
  @Produces("text/plain; charset=UTF-8")
  public Response testconnection(
          @BeanParam RepositoryResolver repositoryResolver,
          @BeanParam final TeamcityConnectionSettings connectionSettings,
          @QueryParam("url") final String url,
          @QueryParam("username") final String username,
          @QueryParam("password") final String password,
          @QueryParam("debugon") final String isDebugOn) {

    String realPasswordValue = password;
    if (Constant.TEAMCITY_PASSWORD_SAVED_VALUE.equals(realPasswordValue)) {
        realPasswordValue = connectionSettings.getPassword(repositoryResolver.getRepository());
    }

    java.net.HttpURLConnection connection = null;
    try {
        String endpoint = url;
        if (!endpoint.endsWith("/")) {
            endpoint += "/";
        }
        endpoint += "app/rest/builds?locator=lookupLimit:0";
        java.net.URL requestUrl = java.net.URI.create(endpoint).toURL();
        connection = (java.net.HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/xml");
        // Basic Auth
        String auth = username + ":" + realPasswordValue;
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            connectionSettings.savePassword(realPasswordValue, repositoryResolver.getRepository());
            return Response.ok(Constant.TEAMCITY_PASSWORD_SAVED_VALUE).build();
        } else {
            String errorMsg;
            try (InputStream errorStream = connection.getErrorStream()) {
                if (errorStream != null) {
                    errorMsg = convertStreamToString(errorStream);
                } else {
                    errorMsg = "HTTP error code: " + responseCode;
                }
            }
            return Response.status(responseCode).entity(errorMsg).build();
        }
    } catch (Exception e) {
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    } finally {
        if (connection != null) {
            connection.disconnect();
        }
    }
  }

  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "build")
  public String getbuild(
    @BeanParam final RepositoryResolver repositoryResolver,
    @BeanParam final SettingsService settingsService,
    @BeanParam final TeamcityConnectionSettings connectionSettings,
    @QueryParam("id") final String id) {

    final Optional<Settings> settings = settingsService.getSettings(repositoryResolver.getRepository());

    if (!settings.isPresent()) {
      return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
    }
 
    final String url = settings.get().getString(Field.TEAMCITY_URL, "");
    final String username = settings.get().getString("teamCityUserName", "");
    final String password = connectionSettings.getPassword(repositoryResolver.getRepository());

    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }

    if (password.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"password is empty\"}";
    }

    final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
    TeamcityConnector connector = new TeamcityConnector(new HttpConnector());
    try {     
      return connector.GetBuild(conf, id, settings.get(), repositoryResolver.getRepository().getName());
    } catch (final IOException ex) {
      return "{\"status\": \"error\", \"message\": \"" + ex.getMessage() + "\"}";
    }
  }

  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repositoryResolver The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @POST
  @Path(value = "triggervcs")
  public Response triggervcs(
    @BeanParam final RepositoryResolver repositoryResolver,
    @QueryParam("vcs") final String vcs,
    @QueryParam("url") final String sha1,
    @QueryParam("username") final String username,
    @QueryParam("password") final String password) {
   try {
      return Response.noContent().build();
    } catch (final Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
  }

  // curl --verbose --silent --user admin:admin --request GET --url 'http://localhost:7990/bitbucket/rest/teamcity/latest/projects/PROJECT_1/repos/rep_1/getHookEnabled?prid=1' --header 'X-Atlassian-Token: no-check'
  @GET
  @Path(value = "getHookEnabled")
  public Response getHookEnabled(
    @BeanParam final RepositoryResolver repositoryResolver,
    @BeanParam final SettingsService settingsService,
    @BeanParam final PullRequestService pullRequestService,
    @BeanParam final RepositoryHookService hookService,
    @QueryParam("prid") final String prid) {

      if (repositoryResolver.getRepository() == null) {
        return Response.ok(false).build();
      }
      try {    
        final RepositoryHook hook = settingsService.getRepositoryHook(repositoryResolver.getRepository());
        final Optional<Settings> settings = settingsService.getSettings2(repositoryResolver.getRepository(), hookService);
        if(!settings.isPresent()) {
          return Response.ok(false).build();
        }      
  
        if (hook == null || !hook.isEnabled()) {
          return Response.ok(false).build();
        }
        
        final PullRequest pullRequest = pullRequestService.getById(repositoryResolver.getRepository().getId(), Long.parseLong(prid));
        if (pullRequest == null) {
          return Response.ok(false).build();
        }
  
        final String repositoryTriggersJson = settings.get().getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
        if (repositoryTriggersJson.isEmpty()) {
          return Response.ok(false).build();
        }
  
        // check if builds are configured
        final PullRequestRef ref = pullRequest.getFromRef();
        final String branch = ref.getId();
  
        final Trigger[] configurations = Trigger.GetBuildConfigurationsFromBranch(repositoryTriggersJson, branch);
        if (configurations.length > 0) {
          return Response.ok(true).build();
        }
      } catch (final Exception ex) {
        // no prb
      }
  
      return Response.ok(false).build();
  }
}
