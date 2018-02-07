package com.trimble.tekla.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
import com.trimble.tekla.SettingsService;
import com.trimble.tekla.TeamcityConnectionSettings;
import com.trimble.tekla.teamcity.HttpConnector;
import com.trimble.tekla.teamcity.TeamcityConfiguration;
import com.trimble.tekla.teamcity.TeamcityConnector;

/**
 * REST configuration
 */
@Path(ResourcePatterns.REPOSITORY_URI)
@Consumes({MediaType.APPLICATION_JSON})
@Produces({RestUtils.APPLICATION_JSON_UTF8})
@Singleton
@AnonymousAllowed
public class TeamctiyRest extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(TeamctiyRest.class);

    private final TeamcityConnector connector;
    private final SettingsService settingsService;
    private final TeamcityConnectionSettings connectionSettings;

    /**
     * Creates Rest resource for testing the Jenkins configuration
     *
     * @param i18nService i18n Service
     */
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
    public String triggerBuild(@Context final Repository repository, @QueryParam("buildconfig") final String buildconfig, @QueryParam("branch") final String branch) {

        final Settings settings = this.settingsService.getSettings(repository);

        if (settings == null) {
            return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
        }

        final String url = settings.getString("TeamCityUrl", "");
        final String username = settings.getString("TeamCityUserName", "");
        final String password = this.connectionSettings.getPassword(repository);

        if (url.isEmpty()) {
            return "{\"status\": \"error\", \"message\": \"invalid id\"}";
        }

        final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
        final String branchtoLower = branch.toLowerCase();
        if (branchtoLower.startsWith("feature/") || branchtoLower.startsWith("bugfix/") || branchtoLower.startsWith("hotfix/")) {
            this.connector.QueueBuild(conf, branch.split("/")[1], buildconfig, "Manual Trigger from Bitbucket", false, settings);
        } else {
            this.connector.QueueBuild(conf, branch, buildconfig, "Manual Trigger from Bitbucket", false, settings);
        }

        return "{\"status\": \"ok\" }";
    }

    @GET
    @Path(value = "builds")
    @Produces(MediaType.APPLICATION_JSON)
    public String getBuildsConfiguration(@Context final Repository repository, @QueryParam("prid") final String prid, @QueryParam("branch") final String branch,
            @QueryParam("hash") final String hash) {

        final Settings settings = this.settingsService.getSettings(repository);

        if (settings == null) {
            return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
        }

        if (settings == null) {
            return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
        }

        final String url = settings.getString("TeamCityUrl", "");
        final String username = settings.getString("TeamCityUserName", "");
        final String password = this.connectionSettings.getPassword(repository);

        if (password.isEmpty()) {
            return "{\"status\": \"error\", \"message\": \"password is empty\"}";
        }

        if (url.isEmpty()) {
            return "{\"status\": \"error\", \"message\": \"invalid id\"}";
        }

        final String[] configurations = GetBuildConfigurationsFromBranch(settings, branch);

        if (configurations.length == 0) {
            return "{\"status\": \"error\", \"message\": \"no build configurations defined for this branch\"}";
        }

        final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
        try {

            final JSONObject jObj = new JSONObject();

            for (final String buildConfig : configurations) {

                String returnData = this.connector.GetBuildsForBranch(conf, branch, buildConfig, settings);
                if (returnData.contains("\"count\":0")) {
                    final String[] elems = branch.split("/");
                    returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildConfig, settings);
                }

                final String queueData = this.connector.GetQueueDataForConfiguration(conf, buildConfig, settings);
                jObj.put(buildConfig, returnData);
                jObj.put(buildConfig + "_queue", queueData);
                jObj.put(buildConfig + "_wref", url + "/viewType.html?buildTypeId=" + buildConfig);
            }

            return jObj.toString();
        } catch (final IOException ex) {
            return "{\"status\": \"error\", \"message\": \"request throw exception" + ex.getMessage() + " \"}";
        } catch (final JSONException ex) {
            return "{\"status\": \"error\", \"message\": \"cannot parse json from teamcity" + ex.getMessage() + " \"}";
        }
    }

    @GET
    @Path(value = "externalbuilds")
    public String getExternalConfiguration(@Context final Repository repository, @QueryParam("id") final String id, @QueryParam("prid") final String prid, @QueryParam("branch") final String branch,
            @QueryParam("hash") final String hash) {

        final Settings settings = this.settingsService.getSettings(repository);

        if (settings == null) {
            return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
        }

        if (settings == null) {
            return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
        }

        final String url = settings.getString("TeamCityUrl", "");
        final String username = settings.getString("TeamCityUserName", "");
        final String password = this.connectionSettings.getPassword(repository);

        if (url.isEmpty()) {
            return "{\"status\": \"error\", \"message\": \"invalid id\"}";
        }

        if (password.isEmpty()) {
            return "{\"status\": \"error\", \"message\": \"password is empty\"}";
        }

        final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
        try {

            final JSONObject jObj = new JSONObject();
            if ("External1IdFeature".equals(id)) {
                final String name = settings.getString("ExternalBuildsOneNameIdFeature");

                if (!branch.toLowerCase().contains("feature/")) {
                    return "{\"status\": \"error\", \"message\": \"applies only to feature branch\"}";
                }

                String json = "";
                if (name == null) {
                    json = "{\"status\": \"ok\", \"name\": \"\"}";
                } else {
                    json = "{\"status\": \"ok\", \"name\": \" " + name + "\"}";
                }

                jObj.put("ExternalBuildsOneNameIdFeature", json);

                final String dependencies = settings.getString("ExternalBuildsOneDepIdFeature");

                for (final String buildId : dependencies.split("\\s+")) {
                    String returnData = this.connector.GetBuildsForBranch(conf, branch, buildId, settings);
                    if (returnData.contains("\"count\":0")) {
                        final String[] elems = branch.split("/");
                        returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildId, settings);
                    }

                    final String queueData = this.connector.GetQueueDataForConfiguration(conf, buildId, settings);
                    jObj.put(buildId + "_dep_wref", url + "/viewType.html?buildTypeId=" + buildId);
                    jObj.put(buildId + "_dep", returnData);
                    jObj.put(buildId + "_dep_queue", queueData);
                }

                final String configurationsToTrigger = settings.getString("ExternalBuildsOneConfigurationsIdFeature");

                for (final String buildId : configurationsToTrigger.split("\\s+")) {
                    String returnData = this.connector.GetBuildsForBranch(conf, branch, buildId, settings);
                    if (returnData.contains("\"count\":0")) {
                        final String[] elems = branch.split("/");
                        returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildId, settings);
                    }

                    final String queueData = this.connector.GetQueueDataForConfiguration(conf, buildId, settings);
                    jObj.put(buildId + "_build", returnData);
                    jObj.put(buildId + "_build_wref", url + "/viewType.html?buildTypeId=" + buildId);
                    jObj.put(buildId + "_build_queue", queueData);
                }

                return jObj.toString();

            } else if ("External1IdBugFix".equals(id)) {
                if (!branch.toLowerCase().contains("bugfix/")) {
                    return "{\"status\": \"error\", \"message\": \"applies only to bugfix branch\"}";
                }

                final String name = settings.getString("ExternalBuildsOneNameIdBugFix");

                if (name == null) {
                    return "{\"status\": \"error\", \"message\": \"BugFix rule not defined\"}";
                }

                String json = "";
                if (name == null) {
                    json = "{\"status\": \"ok\", \"name\": \"\"}";
                } else {
                    json = "{\"status\": \"ok\", \"name\": \" " + name + "\"}";
                }

                jObj.put("ExternalBuildsOneNameIdBugFix", json);

                final String dependencies = settings.getString("ExternalBuildsOneDepIdBugFix");

                for (final String buildId : dependencies.split("\\s+")) {
                    String returnData = this.connector.GetBuildsForBranch(conf, branch, buildId, settings);
                    if (returnData.contains("\"count\":0")) {
                        final String[] elems = branch.split("/");
                        returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildId, settings);
                    }

                    final String queueData = this.connector.GetQueueDataForConfiguration(conf, buildId, settings);
                    jObj.put(buildId + "_dep_wref", url + "/viewType.html?buildTypeId=" + buildId);
                    jObj.put(buildId + "_dep", returnData);
                    jObj.put(buildId + "_dep_queue", queueData);
                }

                final String configurationsToTrigger = settings.getString("ExternalBuildsOneConfigurationsIdBugFix");

                for (final String buildId : configurationsToTrigger.split("\\s+")) {
                    String returnData = this.connector.GetBuildsForBranch(conf, branch, buildId, settings);
                    if (returnData.contains("\"count\":0")) {
                        final String[] elems = branch.split("/");
                        returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildId, settings);
                    }

                    final String queueData = this.connector.GetQueueDataForConfiguration(conf, buildId, settings);
                    jObj.put(buildId + "_build", returnData);
                    jObj.put(buildId + "_build_wref", url + "/viewType.html?buildTypeId=" + buildId);
                    jObj.put(buildId + "_build_queue", queueData);
                }

                return jObj.toString();

            } else if ("External1IdHotFix".equals(id)) {
                if (!branch.toLowerCase().contains("hotfix/")) {
                    return "{\"status\": \"error\", \"message\": \"applies only to hotfix branch\"}";
                }

                final String name = settings.getString("ExternalBuildsOneNameIdHotFix");

                if (name == null) {
                    return "{\"status\": \"error\", \"message\": \"HotFix rule not defined\"}";
                }
                String json = "";
                if (name == null) {
                    json = "{\"status\": \"ok\", \"name\": \"\"}";
                } else {
                    json = "{\"status\": \"ok\", \"name\": \" " + name + "\"}";
                }

                jObj.put("ExternalBuildsOneNameIdHotFix", json);

                final String dependencies = settings.getString("ExternalBuildsOneDepIdHotFix");

                for (final String buildId : dependencies.split("\\s+")) {
                    String returnData = this.connector.GetBuildsForBranch(conf, branch, buildId, settings);
                    if (returnData.contains("\"count\":0")) {
                        final String[] elems = branch.split("/");
                        returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildId, settings);
                    }

                    final String queueData = this.connector.GetQueueDataForConfiguration(conf, buildId, settings);
                    jObj.put(buildId + "_dep_wref", url + "/viewType.html?buildTypeId=" + buildId);
                    jObj.put(buildId + "_dep", returnData);
                    jObj.put(buildId + "_dep_queue", queueData);
                }

                final String configurationsToTrigger = settings.getString("ExternalBuildsOneConfigurationsIdHotFix");

                for (final String buildId : configurationsToTrigger.split("\\s+")) {
                    String returnData = this.connector.GetBuildsForBranch(conf, branch, buildId, settings);
                    if (returnData.contains("\"count\":0")) {
                        final String[] elems = branch.split("/");
                        returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildId, settings);
                    }

                    final String queueData = this.connector.GetQueueDataForConfiguration(conf, buildId, settings);
                    jObj.put(buildId + "_build", returnData);
                    jObj.put(buildId + "_build_wref", url + "/viewType.html?buildTypeId=" + buildId);
                    jObj.put(buildId + "_build_queue", queueData);
                }

                return jObj.toString();

            } else if ("External2Id".equals(id)) {
                if (branch.toLowerCase().contains("feature/") || branch.toLowerCase().contains("bugfix/")) {

                } else {
                    return "{\"status\": \"error\", \"message\": \"applies only to feature and bugfix branch\"}";
                }
                final String name = settings.getString("ExternalBuildsTwoNameId", "");
                String json = "";
                if (name.isEmpty()) {
                    json = "{\"status\": \"ok\", \"name\": \"\"}";
                    jObj.put("ExternalBuildsTwoNameId", json);
                    return jObj.toString();
                } else {
                    json = "{\"status\": \"ok\", \"name\": \" " + name + "\"}";
                    jObj.put("ExternalBuildsTwoNameId", json);
                }

                final String hookconfig = settings.getString("ExternalHooksConfigurationV2");

                final JSONArray jsonObj = new JSONArray(hookconfig);
                final JSONArray extRef = new JSONArray();

                for (int i = 0; i < jsonObj.length(); i++) {
                    final JSONObject build = jsonObj.getJSONObject(i);
                    final String dependencies = build.getString("dependencies");
                    final String source = build.getString("source");

                    if (!branch.toLowerCase().startsWith(source)) {
                        continue;
                    }

                    for (final String buildId : dependencies.split("\\s+")) {
                        String returnData = this.connector.GetBuildsForBranch(conf, branch, buildId, settings);
                        if (returnData.contains("\"count\":0")) {
                            final String[] elems = branch.split("/");
                            returnData = this.connector.GetBuildsForBranch(conf, elems[elems.length - 1], buildId, settings);
                        }

                        final String queueData = this.connector.GetQueueDataForConfiguration(conf, buildId, settings);
                        jObj.put(buildId + "_dep", returnData);
                        jObj.put(buildId + "_dep_wref", url + "/viewType.html?buildTypeId=" + buildId);
                        jObj.put(buildId + "_dep_queue", queueData);
                    }

                    extRef.put(build.toString());

                }
                jObj.put("ext_references", extRef.toString());

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
            returnData = dummyConnector.Get(url, this.settingsService.getSettings(repository));
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
    public Response testconnection(@Context final Repository repository, @QueryParam("url") final String url, @QueryParam("username") final String username,
            @QueryParam("password") final String password) {

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

        final Settings settings = this.settingsService.getSettings(repository);

        if (settings == null) {
            return "{\"status\": \"error\", \"message\": \"hook not configured\"}";
        }

        final String url = settings.getString("TeamCityUrl", "");
        final String username = settings.getString("TeamCityUserName", "");
        final String password = this.connectionSettings.getPassword(repository);

        if (url.isEmpty()) {
            return "{\"status\": \"error\", \"message\": \"invalid id\"}";
        }

        if (password.isEmpty()) {
            return "{\"status\": \"error\", \"message\": \"password is empty\"}";
        }

        final TeamcityConfiguration conf = new TeamcityConfiguration(url, username, password);
        try {
            return this.connector.GetBuild(conf, id, this.settingsService.getSettings(repository));
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

    private String[] GetBuildConfigurationsFromBranch(final Settings settings, final String branch) {
        final List<String> configs = new ArrayList<>();
        if (branch.toLowerCase().startsWith("refs/heads/feature/") || branch.toLowerCase().startsWith("feature/")) {
            final String featuresConfigs = settings.getString("featureRule", "");
            if (!featuresConfigs.isEmpty()) {
                for (final String buildId : featuresConfigs.split("\\s+")) {
                    configs.add(buildId);
                }
            }
        }

        if (branch.toLowerCase().startsWith("refs/heads/bugfix/") || branch.toLowerCase().startsWith("bugfix/")) {
            final String featuresConfigs = settings.getString("bugFixRule", "");
            if (!featuresConfigs.isEmpty()) {
                for (final String buildId : featuresConfigs.split("\\s+")) {
                    configs.add(buildId);
                }
            }
        }

        if (branch.toLowerCase().startsWith("refs/heads/hotfix/") || branch.toLowerCase().startsWith("hotfix/")) {
            final String featuresConfigs = settings.getString("hotfixRule", "");
            if (!featuresConfigs.isEmpty()) {
                for (final String buildId : featuresConfigs.split("\\s+")) {
                    configs.add(buildId);
                }
            }
        }

        return configs.toArray(new String[configs.size()]);
    }
}
