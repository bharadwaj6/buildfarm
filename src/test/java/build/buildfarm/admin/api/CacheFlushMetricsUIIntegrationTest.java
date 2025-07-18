package build.buildfarm.admin.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.adapter.ac.ActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.cas.CASAdapter;
import build.buildfarm.admin.cache.api.CacheFlushMetricsResource;
import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import build.buildfarm.admin.cache.service.CacheFlushService;
import build.buildfarm.admin.cache.service.CacheFlushServiceImpl;
import io.prometheus.client.CollectorRegistry;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for the Cache Flush Metrics UI. */
@RunWith(JUnit4.class)
public class CacheFlushMetricsUIIntegrationTest {

  private CollectorRegistry registry;
  private CacheFlushService cacheFlushService;
  private CacheFlushMetricsResource metricsResource;
  private ActionCacheAdapter mockRedisActionCacheAdapter;
  private CASAdapter mockFilesystemCASAdapter;

  @Before
  public void setUp() {
    // Get the default registry that our metrics are registered with
    registry = CollectorRegistry.defaultRegistry;

    // Create mock adapters
    mockRedisActionCacheAdapter = mock(ActionCacheAdapter.class);
    mockFilesystemCASAdapter = mock(CASAdapter.class);

    // Set up the mock adapters to return successful results
    when(mockRedisActionCacheAdapter.flushEntries(new FlushCriteria(FlushScope.ALL, null, null)))
        .thenReturn(new FlushResult(true, "Success", 10, 0));

    when(mockFilesystemCASAdapter.flushEntries(new FlushCriteria(FlushScope.ALL, null, null)))
        .thenReturn(new FlushResult(true, "Success", 5, 1024));

    // Create adapter maps
    Map<String, ActionCacheAdapter> actionCacheAdapters = new HashMap<>();
    actionCacheAdapters.put("redis", mockRedisActionCacheAdapter);

    Map<String, CASAdapter> casAdapters = new HashMap<>();
    casAdapters.put("filesystem", mockFilesystemCASAdapter);

    // Create the service and resource
    cacheFlushService = new CacheFlushServiceImpl(actionCacheAdapters, casAdapters);
    metricsResource = new CacheFlushMetricsResource();
  }

  @After
  public void tearDown() {
    // Clear all metrics after each test
    registry.clear();
  }

  @Test
  public void testMetricsUpdatedAfterFlushOperations() {
    // Perform flush operations
    ActionCacheFlushRequest acRequest = new ActionCacheFlushRequest();
    acRequest.setScope(FlushScope.ALL);
    acRequest.setFlushRedis(true);
    acRequest.setFlushInMemory(false);
    cacheFlushService.flushActionCache(acRequest);

    CASFlushRequest casRequest = new CASFlushRequest();
    casRequest.setScope(FlushScope.ALL);
    casRequest.setFlushFilesystem(true);
    casRequest.setFlushInMemoryLRU(false);
    casRequest.setFlushRedisWorkerMap(false);
    cacheFlushService.flushCAS(casRequest);

    // Get metrics from the resource
    Response response = metricsResource.getMetrics();

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

    // Verify metrics directly from the registry
    double acOperationsCount =
        registry.getSampleValue(
            "cache_flush_operations_total",
            new String[] {"cache_type", "backend", "scope", "success"},
            new String[] {"action-cache", "redis", "ALL", "true"});
    assertEquals(1.0, acOperationsCount, 0.001);

    double acEntriesRemovedCount =
        registry.getSampleValue(
            "cache_flush_entries_removed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"action-cache", "redis"});
    assertEquals(10.0, acEntriesRemovedCount, 0.001);

    double casOperationsCount =
        registry.getSampleValue(
            "cache_flush_operations_total",
            new String[] {"cache_type", "backend", "scope", "success"},
            new String[] {"cas", "filesystem", "ALL", "true"});
    assertEquals(1.0, casOperationsCount, 0.001);

    double casEntriesRemovedCount =
        registry.getSampleValue(
            "cache_flush_entries_removed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"cas", "filesystem"});
    assertEquals(5.0, casEntriesRemovedCount, 0.001);

    double casBytesReclaimedCount =
        registry.getSampleValue(
            "cache_flush_bytes_reclaimed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"cas", "filesystem"});
    assertEquals(1024.0, casBytesReclaimedCount, 0.001);
  }
}
