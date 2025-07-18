package build.buildfarm.admin.cache.api;

import build.buildfarm.admin.cache.metrics.CacheFlushMetrics;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST resource for cache flush metrics.
 */
@Path("/admin/v1/cache/metrics")
public class CacheFlushMetricsResource {

  /**
   * Gets a summary of all cache flush metrics.
   *
   * @return a response containing the metrics summary
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMetrics() {
    Map<String, Object> metrics = CacheFlushMetrics.getMetricsSummary();
    return Response.ok(metrics).build();
  }
}