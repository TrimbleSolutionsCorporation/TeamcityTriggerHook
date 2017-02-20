package com.trimble.tekla.rest;

import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.permission.PermissionValidationService;
import com.atlassian.bitbucket.repository.RefService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.rest.RestResource;
import com.atlassian.bitbucket.rest.util.ResourcePatterns;
import com.atlassian.bitbucket.rest.util.RestUtils;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.sun.jersey.spi.resource.Singleton;
import com.trimble.tekla.SettingsService;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * REST configuration
 *
 */
@Path(ResourcePatterns.REPOSITORY_URI)
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ RestUtils.APPLICATION_JSON_UTF8 })
@Singleton
@AnonymousAllowed
public class TeamctiyRest extends RestResource {

  private static final Logger log = LoggerFactory.getLogger(TeamctiyRest.class);
  
  private final PermissionValidationService permissionService;
  private final RefService refService;
  private final TeamcityConnector connector;
  private SettingsService settingsService;

  /**
   * Creates Rest resource for testing the Jenkins configuration
   * @param notifier The service to send Jenkins notifications
   * @param permissionValidationService A permission validation service
   * @param i18nService i18n Service
   * @param sshConfigurationService Service to check whether SSH is enabled
   * @param sshScmProtocol Resolver for generating default SSH clone url
   * @param httpScmProtocol Resolver for generating default http clone url
   * @param refService Service to get default Branch
   */
  public TeamctiyRest(PermissionValidationService permissionValidationService,
                         I18nService i18nService,
                         RefService refService,
                         SettingsService settingsService) {
    super(i18nService);
    this.settingsService = settingsService;
    this.permissionService = permissionValidationService;
    this.refService = refService;
    this.connector = new TeamcityConnector(new HttpConnector());
  }

  public TeamctiyRest(PermissionValidationService permissionValidationService,
                         I18nService i18nService,
                         RefService refService,
                         TeamcityConnector connector) {
    super(i18nService);
    this.permissionService = permissionValidationService;
    this.refService = refService;
    this.connector = connector;
  }    
    
  
/**
   * Trigger a build on the Teamcity instance using vcs root
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "loadhtml")
  @Produces(MediaType.TEXT_HTML)
  public String loadhtml(@Context Repository repository, @QueryParam("page") String page) {
    
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    InputStream is = classloader.getResourceAsStream("public/" + page);
    String file = convertStreamToString(is);
    return file;
  }          
  
/**
   * Trigger a build on the Teamcity instance using vcs root
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "loadjs")
  @Produces("text/javascript")
  public String loadjs(@Context Repository repository, @QueryParam("page") String page) {
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    InputStream is = classloader.getResourceAsStream("public/" + page);
    String file = convertStreamToString(is);
    return file;
  }
  
  @GET
  @Path(value = "loadcss")
  @Produces("text/css")
  public String loadcss(@Context Repository repository, @QueryParam("page") String page) {
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    InputStream is = classloader.getResourceAsStream("public/" + page);
    String file = convertStreamToString(is);
    return file;
  }

  @GET
  @Path(value = "loadimg")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response loadimg(@Context Repository repository, @QueryParam("img") String img) {    
    return Response.ok(getResourceAsFile("public/" + img), MediaType.APPLICATION_OCTET_STREAM)
      .header("Content-Disposition", "attachment; filename=\"" + img + "\"" )
      .build();
  }
  
  public static File getResourceAsFile(String resourcePath) {
    try {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream in = classloader.getResourceAsStream(resourcePath);      
        
        if (in == null) {
            return null;
        }

        File tempFile = File.createTempFile(String.valueOf(in.hashCode()), ".tmp");
        tempFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            //copy stream
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    } catch (IOException e) {
        return null;
    }
  }
  
  static String convertStreamToString(java.io.InputStream is) {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  @GET
  @Path(value = "triggerbuild")
  @Produces(MediaType.APPLICATION_JSON)
  public String getBuildsConfiguration(@Context Repository repository,
      @QueryParam("buildconfig") String buildconfig,
      @QueryParam("branch") String branch) {
    
    Settings settings = this.settingsService.getSettings(repository);
    
    String url = settings.getString("TeamCityUrl", "");
    String username = settings.getString("TeamCityUserName", "");
    String password = settings.getString("TeamCityPassword", "");
    
    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }
    
    TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
    String branchtoLower = branch.toLowerCase();
    if (branchtoLower.startsWith("feature/") || branchtoLower.startsWith("bugfix/") || branchtoLower.startsWith("hotfix/")) {
      this.connector.QueueBuild(conf, branch.split("/")[1], buildconfig, "Manual Trigger from Bitbucket", false);
    } else {
      this.connector.QueueBuild(conf, branch, buildconfig, "Manual Trigger from Bitbucket", false);
    }
        
    return "{\"status\": \"ok\" }";
  }
          
  @GET
  @Path(value = "builds")
  @Produces(MediaType.APPLICATION_JSON)
  public String getBuildsConfiguration(@Context Repository repository,
      @QueryParam("prid") String prid,
      @QueryParam("branch") String branch,     
      @QueryParam("hash") String hash) {
        
    Settings settings = this.settingsService.getSettings(repository);
    
    String url = settings.getString("TeamCityUrl", "");
    String username = settings.getString("TeamCityUserName", "");
    String password = settings.getString("TeamCityPassword", "");
    
    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }
    
    String [] configurations = GetBuildConfigurationsFromBranch(settings, branch);
    
    if (configurations.length == 0) {
      return "{\"status\": \"error\", \"message\": \"no build configurations defined for this branch\"}";
    }
    
    TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
    try {
      
      JSONObject jObj = new JSONObject();
      
      for (String buildConfig : configurations) {
        
        
        String returnData = this.connector.GetBuildsForBranch(conf, branch, buildConfig);
        if (returnData.contains("\"count\":0")) {
          String [] elems = branch.split("/");
          returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildConfig);
        }

        String queueData = this.connector.GetQueueDataForConfiguration(conf, buildConfig);        
        jObj.put(buildConfig, returnData);
        jObj.put(buildConfig + "_queue", queueData);        
        jObj.put(buildConfig + "_wref", url + "/viewType.html?buildTypeId=" + buildConfig);
      }
      
      return jObj.toString();
    } catch (IOException ex) {
      return "{\"status\": \"error\", \"message\": \"request throw exception" + ex.getMessage() +" \"}";
    } catch (JSONException ex) {
      return "{\"status\": \"error\", \"message\": \"cannot parse json from teamcity" + ex.getMessage() +" \"}";
    }        
  }

  @GET
  @Path(value = "externalbuilds")
  public String getExternalConfiguration(@Context Repository repository,
      @QueryParam("id") String id,
      @QueryParam("prid") String prid,
      @QueryParam("branch") String branch,
      @QueryParam("hash") String hash) {

    Settings settings = this.settingsService.getSettings(repository);    
    String url = settings.getString("TeamCityUrl", "");
    String username = settings.getString("TeamCityUserName", "");
    String password = settings.getString("TeamCityPassword", "");

    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }

    TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
    try {

        JSONObject jObj = new JSONObject();
        if ("External1Id".equals(id)) {
          String name = settings.getString("ExternalBuildsOneNameId");

          String json = "";
          if (name == null) {
            json = "{\"status\": \"ok\", \"name\": \"\"}";
          } else {
            json = "{\"status\": \"ok\", \"name\": \" " + name + "\"}";
          }

          jObj.put("ExternalBuildsOneNameId", json);

          String dependencies = settings.getString("ExternalBuildsOneDepId");

          for(String buildId : dependencies.split("\\s+")) {
            String returnData = this.connector.GetBuildsForBranch(conf, branch, buildId);
            if (returnData.contains("\"count\":0")) {
              String [] elems = branch.split("/");
              returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildId);
            }
            
            String queueData = this.connector.GetQueueDataForConfiguration(conf, buildId);        
            jObj.put(buildId + "_dep_wref", url + "/viewType.html?buildTypeId=" + buildId);
            jObj.put(buildId + "_dep", returnData);
            jObj.put(buildId + "_dep_queue", queueData);                
          }
          
          String configurationsToTrigger = settings.getString("ExternalBuildsOneConfigurationsId");
          
          for(String buildId : configurationsToTrigger.split("\\s+")) {
            String returnData = this.connector.GetBuildsForBranch(conf, branch, buildId);
            if (returnData.contains("\"count\":0")) {
              String [] elems = branch.split("/");
              returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildId);
            }
            
            String queueData = this.connector.GetQueueDataForConfiguration(conf, buildId);        
            jObj.put(buildId + "_build", returnData);
            jObj.put(buildId + "_build_wref", url + "/viewType.html?buildTypeId=" + buildId);
            jObj.put(buildId + "_build_queue", queueData);                
          }          
        
          return jObj.toString();

        } else if ("External2Id".equals(id)) {
          String name = settings.getString("ExternalBuildsTwoNameId", "");
          String json = "";
          if (name.isEmpty()) {
            json = "{\"status\": \"ok\", \"name\": \"\"}";
            jObj.put("ExternalBuildsTwoNameId", json);
            return jObj.toString();
          } else {
            json = "{\"status\": \"ok\", \"name\": \" " + name + "\"}";
            jObj.put("ExternalBuildsTwoNameId", json);
          }
          
          String dependencies = settings.getString("ExternalBuildsOneDepId");

          for(String buildId : dependencies.split("\\s+")) {
            String returnData = this.connector.GetBuildsForBranch(conf, branch, buildId);
            if (returnData.contains("\"count\":0")) {
              String [] elems = branch.split("/");
              returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildId);
            }
            
            String queueData = this.connector.GetQueueDataForConfiguration(conf, buildId);        
            jObj.put(buildId + "_dep", returnData);
            jObj.put(buildId + "_dep_wref", url + "/viewType.html?buildTypeId=" + buildId);
            jObj.put(buildId + "_dep_queue", queueData);                
          }
          
          String configurationsToTrigger = settings.getString("ExternalHooksConfiguration");
          jObj.put("ext_references", configurationsToTrigger);
          
          return jObj.toString();
        } else {
          return "{\"status\": \"error\", \"message\": \"invalid id\"}";
        }      

      } catch (JSONException ex) {
        return "{\"status\": \"error\", \"message\": \"request throw exception" + ex.getMessage() +" \"}";
      } catch (IOException ex) {      
        return "{\"status\": \"error\", \"message\": \"request throw exception" + ex.getMessage() +" \"}";
      }
  }
                
/**
   * Trigger a build on the Teamcity instance using vcs root
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "triggerexternalurl")
  public String triggerexternalurl(@Context Repository repository,
      @QueryParam("url") String url,
      @QueryParam("method") String method) {

      HttpConnector dummyConnector = new HttpConnector();
      String returnData;
    try {
      returnData = dummyConnector.Get(url);
      return "{\"status\": \"ok\", \"message\": \" " + returnData + "\" }";
    } catch (IOException ex) {
      return "{\"status\": \"failed\", \"message\": \" " + ex.getMessage() + "\" }";
    }
  }
  
  /**
   * Trigger a build on the Teamcity instance using vcs root
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "testconnection")
  public String testconnection(@Context Repository repository,
      @QueryParam("url") String url,
      @QueryParam("username") String username,
      @QueryParam("password") String password) {

      TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
      String returnData = this.connector.TestTeamcityConnection(conf);
      if (!"Ok".equals(returnData)) {
        if (returnData.contains("401")) {
          return "{\"status\": \"failed\", \"message\": \" User or password invalid\" }";
        }
        else {
          return "{\"status\": \"failed\", \"message\": \" Host is not reachable\" }";          
        }
         
        
      } else {
        return "{\"status\": \"ok\"}";
      }
  }
  
  /**
   * Trigger a build on the Teamcity instance using vcs root
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path(value = "build")
  public String getbuild(@Context Repository repository,
      @QueryParam("id") String id) {

    Settings settings = this.settingsService.getSettings(repository);
    
    String url = settings.getString("TeamCityUrl", "");
    String username = settings.getString("TeamCityUserName", "");
    String password = settings.getString("TeamCityPassword", "");
    
    if (url.isEmpty()) {
      return "{\"status\": \"error\", \"message\": \"invalid id\"}";
    }
    
    TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);    
    try {    
      return this.connector.GetBuild(conf, id);
    } catch (IOException ex) {
      return "{\"status\": \"error\", \"message\": \"" + ex.getMessage()+ "\"}";
    }
  }
  
  /**
   * Trigger a build on the Teamcity instance using vcs root
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @POST
  @Path(value = "triggervcs")
  public Response triggervcs(@Context Repository repository,
      @QueryParam("vcs") String vcs,
      @QueryParam("url") String sha1,
      @QueryParam("username") String username,
      @QueryParam("password") String password) {

    try {
      return Response.noContent().build();
    }
    catch (Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(e.getMessage()).build();
    }
  }

  private String[] GetBuildConfigurationsFromBranch(Settings settings, String branch) {
    List<String> configs = new ArrayList<String>();
    if (branch.toLowerCase().startsWith("refs/heads/feature/") || branch.toLowerCase().startsWith("feature/")) {
      String featuresConfigs = settings.getString("featureRule", "");
      if (!featuresConfigs.isEmpty()) {
        for(String buildId : featuresConfigs.split("\\s+")) {
          configs.add(buildId);        
        }
      }
    }
    
    if (branch.toLowerCase().startsWith("refs/heads/bugfix/") || branch.toLowerCase().startsWith("bugfix/")) {
      String featuresConfigs = settings.getString("bugfixRule", "");
      if (!featuresConfigs.isEmpty()) {
        for(String buildId : featuresConfigs.split("\\s+")) {
          configs.add(buildId);        
        }
      }
    }
    
    if (branch.toLowerCase().startsWith("refs/heads/hotfix/")  || branch.toLowerCase().startsWith("hotfix/")) {
      String featuresConfigs = settings.getString("hotfixRule", "");
      if (!featuresConfigs.isEmpty()) {
        for(String buildId : featuresConfigs.split("\\s+")) {
          configs.add(buildId);        
        }
      }
    }
    
    return configs.toArray(new String[configs.size()]);
  }
}
