package build.buildfarm.admin.cache.metrics;

import static org.junit.Assert.assertEquals;

import io.prometheus.client.CollectorRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CacheFlushMetrics}. */
@RunWith(JUnit4.class)
public class CacheFlushMetricsTest {

  private CollectorRegistry registry;

  @Before
  public void setUp() {
    // Get the default registry that our metrics are registered with
    registry = CollectorRegistry.defaultRegistry;
  }

  @After
  public void tearDown() {
    // Clear all metrics after each test
    registry.clear();
  }

  @Test
  public void testRecordFlushOperation() {
    // Record a successful flush operation
    CacheFlushMetrics.recordFlushOperation("action-cache", "redis", "ALL", true);

    // Verify the metric was recorded
    double count =
        registry.getSampleValue(
            "cache_flush_operations_total",
            new String[] {"cache_type", "backend", "scope", "success"},
            new String[] {"action-cache", "redis", "ALL", "true"});

    assertEquals("Should record one successful operation", 1.0, count, 0.001);

    // Record a failed flush operation
    CacheFlushMetrics.recordFlushOperation("action-cache", "redis", "ALL", false);

    // Verify the metric was recorded
    count =
        registry.getSampleValue(
            "cache_flush_operations_total",
            new String[] {"cache_type", "backend", "scope", "success"},
            new String[] {"action-cache", "redis", "ALL", "false"});

    assertEquals("Should record one failed operation", 1.0, count, 0.001);
  }

  @Test
  public void testRecordEntriesRemoved() {
    // Record entries removed
    CacheFlushMetrics.recordEntriesRemoved("action-cache", "redis", 10);

    // Verify the metric was recorded
    double count =
        registry.getSampleValue(
            "cache_flush_entries_removed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"action-cache", "redis"});

    assertEquals("Should record 10 entries removed", 10.0, count, 0.001);

    // Record more entries removed
    CacheFlushMetrics.recordEntriesRemoved("action-cache", "redis", 5);

    // Verify the metric was updated
    count =
        registry.getSampleValue(
            "cache_flush_entries_removed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"action-cache", "redis"});

    assertEquals("Should record 15 total entries removed", 15.0, count, 0.001);

    // Record zero entries removed (should not change the count)
    CacheFlushMetrics.recordEntriesRemoved("action-cache", "redis", 0);

    // Verify the metric was not updated
    count =
        registry.getSampleValue(
            "cache_flush_entries_removed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"action-cache", "redis"});

    assertEquals("Should still have 15 total entries removed", 15.0, count, 0.001);
  }

  @Test
  public void testRecordBytesReclaimed() {
    // Record bytes reclaimed
    CacheFlushMetrics.recordBytesReclaimed("cas", "filesystem", 1024);

    // Verify the metric was recorded
    double count =
        registry.getSampleValue(
            "cache_flush_bytes_reclaimed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"cas", "filesystem"});

    assertEquals("Should record 1024 bytes reclaimed", 1024.0, count, 0.001);

    // Record more bytes reclaimed
    CacheFlushMetrics.recordBytesReclaimed("cas", "filesystem", 2048);

    // Verify the metric was updated
    count =
        registry.getSampleValue(
            "cache_flush_bytes_reclaimed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"cas", "filesystem"});

    assertEquals("Should record 3072 total bytes reclaimed", 3072.0, count, 0.001);

    // Record zero bytes reclaimed (should not change the count)
    CacheFlushMetrics.recordBytesReclaimed("cas", "filesystem", 0);

    // Verify the metric was not updated
    count =
        registry.getSampleValue(
            "cache_flush_bytes_reclaimed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"cas", "filesystem"});

    assertEquals("Should still have 3072 total bytes reclaimed", 3072.0, count, 0.001);
  }

  @Test
  public void testMultipleCacheTypes() {
    // Record operations for different cache types
    CacheFlushMetrics.recordFlushOperation("action-cache", "redis", "ALL", true);
    CacheFlushMetrics.recordFlushOperation("cas", "filesystem", "ALL", true);

    // Verify the metrics were recorded separately
    double acCount =
        registry.getSampleValue(
            "cache_flush_operations_total",
            new String[] {"cache_type", "backend", "scope", "success"},
            new String[] {"action-cache", "redis", "ALL", "true"});

    double casCount =
        registry.getSampleValue(
            "cache_flush_operations_total",
            new String[] {"cache_type", "backend", "scope", "success"},
            new String[] {"cas", "filesystem", "ALL", "true"});

    assertEquals("Should record one action-cache operation", 1.0, acCount, 0.001);
    assertEquals("Should record one cas operation", 1.0, casCount, 0.001);
  }

  @Test
  public void testMultipleBackends() {
    // Record operations for different backends
    CacheFlushMetrics.recordEntriesRemoved("action-cache", "redis", 10);
    CacheFlushMetrics.recordEntriesRemoved("action-cache", "in-memory", 5);

    // Verify the metrics were recorded separately
    double redisCount =
        registry.getSampleValue(
            "cache_flush_entries_removed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"action-cache", "redis"});

    double inMemoryCount =
        registry.getSampleValue(
            "cache_flush_entries_removed_total",
            new String[] {"cache_type", "backend"},
            new String[] {"action-cache", "in-memory"});

    assertEquals("Should record 10 entries removed from redis", 10.0, redisCount, 0.001);
    assertEquals("Should record 5 entries removed from in-memory", 5.0, inMemoryCount, 0.001);
  }
}
