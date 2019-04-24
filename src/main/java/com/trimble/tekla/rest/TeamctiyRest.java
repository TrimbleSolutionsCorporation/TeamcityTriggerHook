package com.trimble.tekla.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.rest.RestResource;
import com.atlassian.bitbucket.rest.util.ResourcePatterns;
import com.atlassian.bitbucket.rest.util.RestUtils;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.spi.resource.Singleton;
import com.trimble.tekla.Constant;
import com.trimble.tekla.Field;
import com.trimble.tekla.SettingsService;
import com.trimble.tekla.TeamcityConnectionSettings;
import com.trimble.tekla.pojo.Trigger;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import java.util.Arrays;

/**
 * REST configuration
 */
@Path(ResourcePatterns.REPOSITORY_URI)
@Consumes({MediaType.APPLICATION_JSON})
@Produces({RestUtils.APPLICATION_JSON_UTF8})
@Singleton
@AnonymousAllowed
public class TeamctiyRest extends RestResource {
  
  private final TeamcityConnector connector;
  private final SettingsService settingsService;
  private final TeamcityConnectionSettings connectionSettings;

  /**
   * Creates Rest resource for testing the Jenkins configuration
   *
   * @param i18nService i18n Service
   */
  @Inject
  public TeamctiyRest(final I18nService i18nService, final SettingsService settingsService, final TeamcityConnectionSettings connectionSettings) {
    super(i18nService);
    this.connectionSettings = connectionSettings;
    this.settingsService = settingsService;
    this.connector = new TeamcityConnector(new HttpConnector());
  }

  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "loadhtml")
  @Produces(MediaType.TEXT_HTML)
  public String loadhtml(@Context final Repository repository, @QueryParam("page") final String page) {

    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    return file;
  }

  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repository The repository to trigger
     * @param page
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "loadjs")
  @Produces("text/javascript")
  public String loadjs(@Context final Repository repository, @QueryParam("page") final String page) {
    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    return file;
  }

  @GET
  @Path(value = "loadcss")
  @Produces("text/css")
  public String loadcss(@Context final Repository repository, @QueryParam("page") final String page) {
    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    return file;
  }

  @GET
  @Path(value = "loadimg")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response loadimg(@Context final Repository repository, @QueryParam("img") final String img) {
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
          @Context final Repository repository,
          @QueryParam("buildconfig") final String buildconfig,
          @QueryParam("branch") final String branch,
          @QueryParam("prid") final String prid) throws IOException {

    final Settings settings = this.settingsService.getSettings(repository).get();

    if (settings == null) {
      return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
    }

    final String url = settings.getString("teamCityUrl", "");
    final String username = settings.getString("teamCityUserName", "");
    final String password = this.connectionSettings.getPassword(repository);

    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }

    final String repositoryTriggersJson = settings.getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
    if (repositoryTriggersJson.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"hook not configured properly\"}";
    }

    final Trigger[] configurations = Trigger.GetBuildConfigurationsFromBranch(repositoryTriggersJson, branch);

    if (configurations.length == 0) {
      return "{\"status\": \"error\", \"message\": \"no build configurations defined for this branch\"}";
    }
        
    final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
    
    StringBuilder builder = new StringBuilder();
    builder.append("ok will trigger: ").append(configurations.length);
            
    for (final Trigger buildConfig : configurations) {
      String []donwstramTriggers = buildConfig.getDownStreamTriggerTarget().split(",");
      if (buildConfig.getTarget().equals(buildconfig) || Arrays.asList(donwstramTriggers).contains(buildconfig)) {
        builder.append(" trigger ")
                .append(buildconfig)
                .append(" ")
                .append(buildConfig.getBranchConfig());
        builder.append(" ")
                .append(this.connector.QueueBuild(conf, buildConfig.getBranchConfig(), buildconfig, "Manual Trigger from Bitbucket: Pull Request: " + prid, false, settings)); // handle error todo
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
          @Context final Repository repository,
          @QueryParam("prid") final String prid,
          @QueryParam("branch") final String branch,
          @QueryParam("hash") final String hash) throws IOException {

    final Settings settings = this.settingsService.getSettings(repository).get();

    if (settings == null) {
      return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
    }

    if (settings == null) {
      return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
    }

    final String url = settings.getString("teamCityUrl", "");
    final String username = settings.getString("teamCityUserName", "");
    final String password = this.connectionSettings.getPassword(repository);

    if (password.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"password is empty\"}";
    }

    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }

    final String repositoryTriggersJson = settings.getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
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
          final String returnData = this.connector.GetBuildsForBranch(conf, buildConfig.getBranchConfig(), buildConfig.getTarget(), settings);
          final String queueData = this.connector.GetQueueDataForConfiguration(conf, buildConfig.getTarget(), settings);
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
          @Context final Repository repository,
          @QueryParam("id") final String id,
          @QueryParam("prid") final String prid,
          @QueryParam("branch") final String branch,
          @QueryParam("hash") final String hash) {

    final Settings settings = this.settingsService.getSettings(repository).get();

    if (settings == null) {
      return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
    }

    if (settings == null) {
      return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
    }

    final String url = settings.getString("teamCityUrl", "");
    final String username = settings.getString("teamCityUserName", "");
    final String password = this.connectionSettings.getPassword(repository);

    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }

    if (password.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"password is empty\"}";
    }

    final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
    try {
      final String repositoryTriggersJson = settings.getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
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
            final String returnData = this.connector.GetBuildsForBranch(conf, buildConfig.getBranchConfig(), depBuildId, settings);
            final String queueData = this.connector.GetQueueDataForConfiguration(conf, depBuildId, settings);            
            jObj.put(depBuildId + "_dep_wref", url + "/viewType.html?buildTypeId=" + depBuildId);
            jObj.put(depBuildId + "_dep", returnData);
            jObj.put(depBuildId + "_dep_queue", queueData);

            final String [] downBuildIds = buildConfig.getDownStreamTriggerTarget().split(",");           
            for (String downBuildId : downBuildIds) {
                final String returnDataBuildDep = this.connector.GetBuildsForBranch(conf, buildConfig.getBranchConfig(), downBuildId, settings);
                final String queueDataBuildDep = this.connector.GetQueueDataForConfiguration(conf, downBuildId, settings);
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
            
            final String returnData = this.connector.GetBuildsForBranch(conf, buildConfig.getBranchConfig(), depBuildId, settings);
            final String queueData = this.connector.GetQueueDataForConfiguration(conf, depBuildId, settings);
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
  public String triggerexternalurl(@Context final Repository repository, @QueryParam("url") final String url, @QueryParam("method") final String method) {

    final HttpConnector dummyConnector = new HttpConnector();
    String returnData;
    try {
      returnData = dummyConnector.Get(url, this.settingsService.getSettings(repository).get());
      return "{\"status\": \"ok\", \"message\": \" " + returnData + "\" }";
    } catch (final IOException ex) {
      return "{\"status\": \"failed\", \"message\": \" " + ex.getMessage() + "\" }";
    }
  }

  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repository - {@link Repository}
   * @param url - url to TeamCity server
   * @param username - TeamCity user name
   * @param password - TeamCity user password
   * @return "OK" if it worked. Otherwise, an error message.
   */
  @GET
  @Path(value = "testconnection")
  @Produces("text/plain; charset=UTF-8")
  public Response testconnection(
          @Context final Repository repository,
          @QueryParam("url") final String url,
          @QueryParam("username") final String username,
          @QueryParam("password") final String password,
          @QueryParam("debugon") final String isDebugOn) {

    String realPasswordValue = password;
    if (Constant.TEAMCITY_PASSWORD_SAVED_VALUE.equals(realPasswordValue)) {
      realPasswordValue = this.connectionSettings.getPassword(repository);
    }

    final Client restClient = Client.create(Constant.REST_CLIENT_CONFIG);
    restClient.addFilter(new HTTPBasicAuthFilter(username, realPasswordValue));

    try {
      final ClientResponse response = restClient.resource(url + "/app/rest/builds?locator=lookupLimit:0").accept(MediaType.APPLICATION_XML).get(ClientResponse.class);
      if (ClientResponse.Status.OK == response.getClientResponseStatus()) {
        this.connectionSettings.savePassword(realPasswordValue, repository);
        return Response.ok(Constant.TEAMCITY_PASSWORD_SAVED_VALUE).build();
      } else {
        return Response.status(response.getClientResponseStatus()).entity(response.getEntity(String.class)).build();
      }
    } catch (final UniformInterfaceException | ClientHandlerException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    } finally {
      restClient.destroy();
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
  public String getbuild(@Context final Repository repository, @QueryParam("id") final String id) {

    final Settings settings = this.settingsService.getSettings(repository).get();

    if (settings == null) {
      return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
    }

    final String url = settings.getString("teamCityUrl", "");
    final String username = settings.getString("teamCityUserName", "");
    final String password = this.connectionSettings.getPassword(repository);

    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }

    if (password.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"password is empty\"}";
    }

    final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
    try {
      return this.connector.GetBuild(conf, id, this.settingsService.getSettings(repository).get());
    } catch (final IOException ex) {
      return "{\"status\": \"error\", \"message\": \"" + ex.getMessage() + "\"}";
    }
  }

  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @POST
  @Path(value = "triggervcs")
  public Response triggervcs(@Context final Repository repository, @QueryParam("vcs") final String vcs, @QueryParam("url") final String sha1, @QueryParam("username") final String username,
          @QueryParam("password") final String password) {

    try {
      return Response.noContent().build();
    } catch (final Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
  }
}
