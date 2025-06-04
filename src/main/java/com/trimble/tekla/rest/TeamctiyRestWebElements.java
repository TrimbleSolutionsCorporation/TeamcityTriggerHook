package com.trimble.tekla.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.atlassian.bitbucket.rest.v2.api.resolver.RepositoryResolver;
import javax.inject.Singleton;
import javax.ws.rs.BeanParam;

/**
 * REST configuration
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({"application/json;charset=UTF-8"})
@Singleton
public class TeamctiyRestWebElements {
  
  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repository The repository to trigger
   * @return The response. Ok if it worked. Otherwise, an error.
   */
  @GET
  @Path("/loadhtml")
  @Produces(MediaType.TEXT_HTML)
  public String loadhtml(@BeanParam final RepositoryResolver repositoryResolver, @QueryParam("page") final String page) {

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
  @Path("/loadjs")
  @Produces("text/javascript")
  public String loadjs(@QueryParam("page") final String page) {
    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    return file;
  }

  @GET
  @Path("/loadcss")
  @Produces("text/css")
  public String loadcss(@QueryParam("page") final String page) {
    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    return file;
  }

  @GET
  @Path("/loadimg")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response loadimg(@QueryParam("img") final String img) {
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
    final java.util.Scanner sdata = new java.util.Scanner(is).useDelimiter("\\A");
    return sdata.hasNext() ? sdata.next() : "";
  }
}
