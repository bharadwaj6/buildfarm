package build.buildfarm.admin.cache.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import build.buildfarm.admin.cache.metrics.CacheFlushMetrics;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link CacheFlushMetricsResource}.
 */
@RunWith(JUnit4.class)
public class CacheFlushMetricsResourceTest {

  private CacheFlushMetricsResource resource;

  @Before
  public void setUp() {
    resource = new CacheFlushMetricsResource();
  }

  @Test
  public void testGetMetrics() {
    // Record some metrics
    CacheFlushMetrics.recordFlushOperation("action-cache", "redis", "ALL", true);
    CacheFlushMetrics.recordEntriesRemoved("action-cache", "redis", 10);
    
    CacheFlushMetrics.recordFlushOperation("cas", "filesystem", "ALL", true);
    CacheFlushMetrics.recordEntriesRemoved("cas", "filesystem", 5);
    CacheFlushMetrics.recordBytesReclaimed("cas", "filesystem", 1024);
    
    // Get metrics from the resource
    Response response = resource.getMetrics();
    
    // Verify the response
    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof Map);
    
    @SuppressWarnings("unchecked")
    Map<String, Object> metrics = (Map<String, Object>) response.getEntity();
    
    // Verify the metrics structure
    assertTrue(metrics.containsKey("cache_types"));
    
    @SuppressWarnings("unchecked")
    Map<String, Map<String, Object>> cacheTypes = 
        (Map<String, Map<String, Object>>) metrics.get("cache_types");
    
    // Verify Action Cache metrics
    assertTrue(cacheTypes.containsKey("action-cache"));
    Map<String, Object> actionCacheMetrics = cacheTypes.get("action-cache");
    assertEquals(1.0, actionCacheMetrics.get("operations_success"));
    assertEquals(10.0, actionCacheMetrics.get("entries_removed"));
    
    // Verify CAS metrics
    assertTrue(cacheTypes.containsKey("cas"));
    Map<String, Object> casMetrics = cacheTypes.get("cas");
    assertEquals(1.0, casMetrics.get("operations_success"));
    assertEquals(5.0, casMetrics.get("entries_removed"));
    assertEquals(1024.0, casMetrics.get("bytes_reclaimed"));
  }
}