package build.buildfarm.admin.cache.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.adapter.ac.ActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.cas.CASAdapter;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for {@link CacheFlushMetrics} with {@link CacheFlushServiceImpl}.
 */
@RunWith(JUnit4.class)
public class CacheFlushMetricsIntegrationTest {

  private CollectorRegistry registry;
  private CacheFlushService cacheFlushService;
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
    when(mockRedisActionCacheAdapter.flushEntries(
        new FlushCriteria(FlushScope.ALL, null, null)))
        .thenReturn(new FlushResult(true, "Success", 10, 0));
    
    when(mockFilesystemCASAdapter.flushEntries(
        new FlushCriteria(FlushScope.ALL, null, null)))
        .thenReturn(new FlushResult(true, "Success", 5, 1024));
    
    // Create adapter maps
    Map<String, ActionCacheAdapter> actionCacheAdapters = new HashMap<>();
    actionCacheAdapters.put("redis", mockRedisActionCacheAdapter);
    
    Map<String, CASAdapter> casAdapters = new HashMap<>();
    casAdapters.put("filesystem", mockFilesystemCASAdapter);
    
    // Create the service
    cacheFlushService = new CacheFlushServiceImpl(actionCacheAdapters, casAdapters);
  }

  @After
  public void tearDown() {
    // Clear all metrics after each test
    registry.clear();
  }

  @Test
  public void testActionCacheFlushRecordsMetrics() {
    // Create a request to flush the Action Cache
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    request.setFlushInMemory(false);
    
    // Perform the flush operation
    cacheFlushService.flushActionCache(request);
    
    // Verify the operation metric was recorded
    double operationCount = registry.getSampleValue(
        "cache_flush_operations_total",
        new String[] {"cache_type", "backend", "scope", "success"},
        new String[] {"action-cache", "redis", "ALL", "true"});
    
    assertEquals("Should record one successful operation", 1.0, operationCount, 0.001);
    
    // Verify the entries removed metric was recorded
    double entriesRemovedCount = registry.getSampleValue(
        "cache_flush_entries_removed_total",
        new String[] {"cache_type", "backend"},
        new String[] {"action-cache", "redis"});
    
    assertEquals("Should record 10 entries removed", 10.0, entriesRemovedCount, 0.001);
  }

  @Test
  public void testCASFlushRecordsMetrics() {
    // Create a request to flush the CAS
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(false);
    request.setFlushRedisWorkerMap(false);
    
    // Perform the flush operation
    cacheFlushService.flushCAS(request);
    
    // Verify the operation metric was recorded
    double operationCount = registry.getSampleValue(
        "cache_flush_operations_total",
        new String[] {"cache_type", "backend", "scope", "success"},
        new String[] {"cas", "filesystem", "ALL", "true"});
    
    assertEquals("Should record one successful operation", 1.0, operationCount, 0.001);
    
    // Verify the entries removed metric was recorded
    double entriesRemovedCount = registry.getSampleValue(
        "cache_flush_entries_removed_total",
        new String[] {"cache_type", "backend"},
        new String[] {"cas", "filesystem"});
    
    assertEquals("Should record 5 entries removed", 5.0, entriesRemovedCount, 0.001);
    
    // Verify the bytes reclaimed metric was recorded
    double bytesReclaimedCount = registry.getSampleValue(
        "cache_flush_bytes_reclaimed_total",
        new String[] {"cache_type", "backend"},
        new String[] {"cas", "filesystem"});
    
    assertEquals("Should record 1024 bytes reclaimed", 1024.0, bytesReclaimedCount, 0.001);
  }
}