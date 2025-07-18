package build.buildfarm.admin.api;

import java.io.InputStream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST resource for admin UI pages.
 */
@Path("/admin/ui")
public class AdminUIResource {

  /**
   * Serves the admin dashboard page.
   *
   * @return the response containing the HTML page
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response getAdminDashboard() {
    InputStream htmlStream = getClass().getClassLoader().getResourceAsStream("static/html/admin-dashboard.html");
    
    if (htmlStream == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity("Admin dashboard page not found")
          .build();
    }
    
    return Response.ok(htmlStream).build();
  }

  /**
   * Serves the cache flush UI page.
   *
   * @return the response containing the HTML page
   */
  @GET
  @Path("/cache-flush")
  @Produces(MediaType.TEXT_HTML)
  public Response getCacheFlushUI() {
    InputStream htmlStream = getClass().getClassLoader().getResourceAsStream("static/html/cache-flush.html");
    
    if (htmlStream == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity("Cache flush UI page not found")
          .build();
    }
    
    return Response.ok(htmlStream).build();
  }
}