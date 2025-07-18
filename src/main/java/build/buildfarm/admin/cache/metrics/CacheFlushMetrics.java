package build.buildfarm.admin.cache.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import java.util.HashMap;
import java.util.Map;

/** Metrics for cache flush operations. */
public class CacheFlushMetrics {
  private static final Counter flushOperationsCounter =
      Counter.build()
          .name("cache_flush_operations_total")
          .labelNames("cache_type", "backend", "scope", "success")
          .help("Total number of cache flush operations.")
          .register();

  private static final Counter entriesRemovedCounter =
      Counter.build()
          .name("cache_flush_entries_removed_total")
          .labelNames("cache_type", "backend")
          .help("Total number of entries removed by cache flush operations.")
          .register();

  private static final Counter bytesReclaimedCounter =
      Counter.build()
          .name("cache_flush_bytes_reclaimed_total")
          .labelNames("cache_type", "backend")
          .help("Total number of bytes reclaimed by cache flush operations.")
          .register();

  /**
   * Records a cache flush operation.
   *
   * @param cacheType the type of cache (action-cache or cas)
   * @param backend the backend (redis, in-memory, filesystem, etc.)
   * @param scope the scope of the flush operation (ALL, INSTANCE, DIGEST_PREFIX)
   * @param success whether the operation was successful
   */
  public static void recordFlushOperation(
      String cacheType, String backend, String scope, boolean success) {
    flushOperationsCounter.labels(cacheType, backend, scope, String.valueOf(success)).inc();
  }

  /**
   * Records entries removed by a cache flush operation.
   *
   * @param cacheType the type of cache (action-cache or cas)
   * @param backend the backend (redis, in-memory, filesystem, etc.)
   * @param entriesRemoved the number of entries removed
   */
  public static void recordEntriesRemoved(String cacheType, String backend, int entriesRemoved) {
    if (entriesRemoved > 0) {
      entriesRemovedCounter.labels(cacheType, backend).inc(entriesRemoved);
    }
  }

  /**
   * Records bytes reclaimed by a cache flush operation.
   *
   * @param cacheType the type of cache (action-cache or cas)
   * @param backend the backend (redis, in-memory, filesystem, etc.)
   * @param bytesReclaimed the number of bytes reclaimed
   */
  public static void recordBytesReclaimed(String cacheType, String backend, long bytesReclaimed) {
    if (bytesReclaimed > 0) {
      bytesReclaimedCounter.labels(cacheType, backend).inc(bytesReclaimed);
    }
  }

  /**
   * Gets the total number of flush operations for a specific cache type, backend, scope, and
   * success status.
   *
   * @param cacheType the type of cache (action-cache or cas)
   * @param backend the backend (redis, in-memory, filesystem, etc.)
   * @param scope the scope of the flush operation (ALL, INSTANCE, DIGEST_PREFIX)
   * @param success whether the operation was successful
   * @return the total number of flush operations
   */
  public static double getFlushOperationsCount(
      String cacheType, String backend, String scope, boolean success) {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        "cache_flush_operations_total",
        new String[] {"cache_type", "backend", "scope", "success"},
        new String[] {cacheType, backend, scope, String.valueOf(success)});
  }

  /**
   * Gets the total number of entries removed for a specific cache type and backend.
   *
   * @param cacheType the type of cache (action-cache or cas)
   * @param backend the backend (redis, in-memory, filesystem, etc.)
   * @return the total number of entries removed
   */
  public static double getEntriesRemovedCount(String cacheType, String backend) {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        "cache_flush_entries_removed_total",
        new String[] {"cache_type", "backend"},
        new String[] {cacheType, backend});
  }

  /**
   * Gets the total number of bytes reclaimed for a specific cache type and backend.
   *
   * @param cacheType the type of cache (action-cache or cas)
   * @param backend the backend (redis, in-memory, filesystem, etc.)
   * @return the total number of bytes reclaimed
   */
  public static double getBytesReclaimedCount(String cacheType, String backend) {
    return CollectorRegistry.defaultRegistry.getSampleValue(
        "cache_flush_bytes_reclaimed_total",
        new String[] {"cache_type", "backend"},
        new String[] {cacheType, backend});
  }

  /**
   * Gets a summary of all cache flush metrics.
   *
   * @return a map containing all cache flush metrics
   */
  public static Map<String, Object> getMetricsSummary() {
    Map<String, Object> summary = new HashMap<>();

    // Get all cache types and backends from the metrics
    Map<String, Map<String, Object>> cacheTypes = new HashMap<>();

    // Action Cache metrics
    Map<String, Object> actionCacheMetrics = new HashMap<>();
    actionCacheMetrics.put(
        "operations_success",
        getFlushOperationsCount("action-cache", "redis", "ALL", true)
            + getFlushOperationsCount("action-cache", "in-memory", "ALL", true));
    actionCacheMetrics.put(
        "operations_failure",
        getFlushOperationsCount("action-cache", "redis", "ALL", false)
            + getFlushOperationsCount("action-cache", "in-memory", "ALL", false));
    actionCacheMetrics.put(
        "entries_removed",
        getEntriesRemovedCount("action-cache", "redis")
            + getEntriesRemovedCount("action-cache", "in-memory"));
    cacheTypes.put("action-cache", actionCacheMetrics);

    // CAS metrics
    Map<String, Object> casMetrics = new HashMap<>();
    casMetrics.put(
        "operations_success",
        getFlushOperationsCount("cas", "filesystem", "ALL", true)
            + getFlushOperationsCount("cas", "in-memory-lru", "ALL", true)
            + getFlushOperationsCount("cas", "redis-worker-map", "ALL", true));
    casMetrics.put(
        "operations_failure",
        getFlushOperationsCount("cas", "filesystem", "ALL", false)
            + getFlushOperationsCount("cas", "in-memory-lru", "ALL", false)
            + getFlushOperationsCount("cas", "redis-worker-map", "ALL", false));
    casMetrics.put(
        "entries_removed",
        getEntriesRemovedCount("cas", "filesystem")
            + getEntriesRemovedCount("cas", "in-memory-lru")
            + getEntriesRemovedCount("cas", "redis-worker-map"));
    casMetrics.put(
        "bytes_reclaimed",
        getBytesReclaimedCount("cas", "filesystem")
            + getBytesReclaimedCount("cas", "in-memory-lru")
            + getBytesReclaimedCount("cas", "redis-worker-map"));
    cacheTypes.put("cas", casMetrics);

    summary.put("cache_types", cacheTypes);

    return summary;
  }
}
