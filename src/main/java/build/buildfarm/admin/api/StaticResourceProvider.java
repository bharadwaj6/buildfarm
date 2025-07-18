package build.buildfarm.admin.api;

import java.io.InputStream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provider for static resources (CSS, JS, etc.).
 */
@Path("/static")
public class StaticResourceProvider {

  /**
   * Serves CSS files.
   *
   * @param fileName the CSS file name
   * @return the response containing the CSS file
   */
  @GET
  @Path("/css/{fileName}")
  @Produces("text/css")
  public Response getCssResource(@PathParam("fileName") String fileName) {
    return getStaticResource("static/css/" + fileName);
  }

  /**
   * Serves JavaScript files.
   *
   * @param fileName the JavaScript file name
   * @return the response containing the JavaScript file
   */
  @GET
  @Path("/js/{fileName}")
  @Produces("application/javascript")
  public Response getJsResource(@PathParam("fileName") String fileName) {
    return getStaticResource("static/js/" + fileName);
  }

  /**
   * Serves HTML files.
   *
   * @param fileName the HTML file name
   * @return the response containing the HTML file
   */
  @GET
  @Path("/html/{fileName}")
  @Produces(MediaType.TEXT_HTML)
  public Response getHtmlResource(@PathParam("fileName") String fileName) {
    return getStaticResource("static/html/" + fileName);
  }

  /**
   * Gets a static resource from the classpath.
   *
   * @param resourcePath the resource path
   * @return the response containing the resource
   */
  private Response getStaticResource(String resourcePath) {
    InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    
    if (resourceStream == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity("Resource not found: " + resourcePath)
          .build();
    }
    
    return Response.ok(resourceStream).build();
  }
}