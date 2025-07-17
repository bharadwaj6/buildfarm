package build.buildfarm.admin.api;

import build.buildfarm.admin.cache.CacheFlushService;
import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST resource for cache flushing operations.
 */
@Path("/admin/v1/cache")
public class CacheFlushResource {
  private final CacheFlushService cacheFlushService;

  @Inject
  public CacheFlushResource(CacheFlushService cacheFlushService) {
    this.cacheFlushService = cacheFlushService;
  }

  /**
   * Flushes Action Cache entries based on specified criteria.
   *
   * @param request the flush request
   * @return the response
   */
  @POST
  @Path("/action/flush")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response flushActionCache(ActionCacheFlushRequest request) {
    try {
      ActionCacheFlushResponse response = cacheFlushService.flushActionCache(request);
      return Response.ok(response).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error flushing Action Cache: " + e.getMessage())
          .build();
    }
  }

  /**
   * Flushes Content Addressable Storage entries based on specified criteria.
   *
   * @param request the flush request
   * @return the response
   */
  @POST
  @Path("/cas/flush")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response flushCAS(CASFlushRequest request) {
    try {
      CASFlushResponse response = cacheFlushService.flushCAS(request);
      return Response.ok(response).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error flushing CAS: " + e.getMessage())
          .build();
    }
  }
}