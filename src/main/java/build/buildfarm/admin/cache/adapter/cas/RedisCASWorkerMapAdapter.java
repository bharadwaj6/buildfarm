package build.buildfarm.admin.cache.adapter.cas;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import com.google.common.base.Preconditions;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/** Implementation of {@link CASAdapter} for Redis-backed CAS worker map. */
public class RedisCASWorkerMapAdapter implements CASAdapter {

  private static final Logger logger = Logger.getLogger(RedisCASWorkerMapAdapter.class.getName());

  // Prometheus metrics
  private static final Counter casFlushOperationsCounter =
      Counter.build()
          .name("cas_redis_worker_map_flush_operations_total")
          .help("Total number of Redis CAS worker map flush operations")
          .register();

  private static final Counter casEntriesRemovedCounter =
      Counter.build()
          .name("cas_redis_worker_map_entries_removed_total")
          .help("Total number of Redis CAS worker map entries removed")
          .register();

  private static final Gauge casBytesReclaimedGauge =
      Gauge.build()
          .name("cas_redis_worker_map_bytes_reclaimed_total")
          .help("Total bytes reclaimed from Redis CAS worker map")
          .register();

  private final UnifiedJedis jedis;
  private final String mapName;

  /**
   * Creates a new RedisCASWorkerMapAdapter instance.
   *
   * @param jedis the Redis client
   * @param mapName the name of the CAS worker map in Redis
   */
  public RedisCASWorkerMapAdapter(UnifiedJedis jedis, String mapName) {
    this.jedis = Preconditions.checkNotNull(jedis, "jedis");
    this.mapName = Preconditions.checkNotNull(mapName, "mapName");
  }

  @Override
  public FlushResult flushEntries(FlushCriteria criteria) {
    Preconditions.checkNotNull(criteria, "criteria");

    casFlushOperationsCounter.inc();

    FlushScope scope = criteria.getScope();
    if (scope == null) {
      return new FlushResult(false, "Unknown flush scope: null", 0, 0);
    }

    switch (scope) {
      case ALL:
        return flushAllEntries();
      case INSTANCE:
        return flushInstanceEntries(criteria.getInstanceName());
      case DIGEST_PREFIX:
        return flushDigestPrefixEntries(criteria.getDigestPrefix());
      default:
        return new FlushResult(false, "Unknown flush scope: " + scope, 0, 0);
    }
  }

  /**
   * Flushes all CAS worker map entries from Redis.
   *
   * @return the result of the flush operation
   */
  private FlushResult flushAllEntries() {
    try {
      List<String> keysToDelete = findAllCasWorkerMapKeys();
      int entriesRemoved = deleteKeys(keysToDelete);

      // Since we're dealing with metadata, not actual blobs, we don't have a precise
      // measure of bytes reclaimed. We'll use a nominal value per entry.
      long bytesReclaimed = estimateBytesReclaimed(entriesRemoved);

      String message =
          String.format("Flushed %d CAS worker map entries from Redis", entriesRemoved);
      updateMetrics(entriesRemoved, bytesReclaimed);

      return new FlushResult(true, message, entriesRemoved, bytesReclaimed);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to flush all CAS worker map entries", e);
      return new FlushResult(
          false, "Failed to flush CAS worker map entries: " + e.getMessage(), 0, 0);
    }
  }

  /**
   * Flushes CAS worker map entries for a specific instance from Redis.
   *
   * @param instanceName the instance name
   * @return the result of the flush operation
   */
  private FlushResult flushInstanceEntries(String instanceName) {
    // In the current implementation, CAS worker map entries are not separated by instance
    // This is a placeholder for future implementation
    // For now, we'll return a result indicating that no entries were removed

    String message =
        String.format(
            "Instance-specific flush not supported for Redis CAS worker map. No entries removed for"
                + " instance %s",
            instanceName);

    return new FlushResult(true, message, 0, 0);
  }

  /**
   * Flushes CAS worker map entries with a specific digest prefix from Redis.
   *
   * @param digestPrefix the digest prefix
   * @return the result of the flush operation
   */
  private FlushResult flushDigestPrefixEntries(String digestPrefix) {
    try {
      List<String> keysToDelete = findCasWorkerMapKeysWithDigestPrefix(digestPrefix);
      int entriesRemoved = deleteKeys(keysToDelete);

      // Since we're dealing with metadata, not actual blobs, we don't have a precise
      // measure of bytes reclaimed. We'll use a nominal value per entry.
      long bytesReclaimed = estimateBytesReclaimed(entriesRemoved);

      String message =
          String.format(
              "Flushed %d CAS worker map entries with digest prefix %s from Redis",
              entriesRemoved, digestPrefix);

      updateMetrics(entriesRemoved, bytesReclaimed);

      return new FlushResult(true, message, entriesRemoved, bytesReclaimed);
    } catch (Exception e) {
      logger.log(
          Level.SEVERE,
          "Failed to flush CAS worker map entries with digest prefix: " + digestPrefix,
          e);
      return new FlushResult(
          false, "Failed to flush CAS worker map entries: " + e.getMessage(), 0, 0);
    }
  }

  /**
   * Finds all CAS worker map keys in Redis.
   *
   * @return a list of Redis keys for CAS worker map entries
   */
  private List<String> findAllCasWorkerMapKeys() {
    List<String> keys = new ArrayList<>();
    String pattern = mapName + ":*";
    String cursor = "0";

    do {
      ScanResult<String> scanResult =
          jedis.scan(cursor, new ScanParams().match(pattern).count(1000));
      cursor = scanResult.getCursor();
      keys.addAll(scanResult.getResult());
    } while (!cursor.equals("0"));

    return keys;
  }

  /**
   * Finds CAS worker map keys with a specific digest prefix in Redis.
   *
   * @param digestPrefix the digest prefix
   * @return a list of Redis keys for CAS worker map entries with the digest prefix
   */
  private List<String> findCasWorkerMapKeysWithDigestPrefix(String digestPrefix) {
    List<String> matchingKeys = new ArrayList<>();
    List<String> allKeys = findAllCasWorkerMapKeys();

    for (String key : allKeys) {
      if (hasDigestPrefix(key, digestPrefix)) {
        matchingKeys.add(key);
      }
    }

    return matchingKeys;
  }

  /**
   * Checks if a Redis key has a specific digest prefix.
   *
   * @param key the Redis key to check
   * @param digestPrefix the digest prefix
   * @return true if the key has the digest prefix, false otherwise
   */
  private boolean hasDigestPrefix(String key, String digestPrefix) {
    // Extract the digest part from the key
    // Key format is mapName:digest
    String[] parts = key.split(":");
    if (parts.length != 2) {
      return false;
    }

    String digest = parts[1];
    return digest.startsWith(digestPrefix);
  }

  /**
   * Deletes the given Redis keys.
   *
   * @param keys the Redis keys to delete
   * @return the number of keys successfully deleted
   */
  private int deleteKeys(List<String> keys) {
    if (keys.isEmpty()) {
      return 0;
    }

    String[] keysArray = keys.toArray(new String[0]);
    long deletedCount = jedis.del(keysArray);

    return (int) deletedCount;
  }

  /**
   * Estimates the number of bytes reclaimed based on the number of entries removed. This is an
   * approximation since we don't have direct access to the size of each entry.
   *
   * @param entriesRemoved the number of entries removed
   * @return the estimated number of bytes reclaimed
   */
  private long estimateBytesReclaimed(int entriesRemoved) {
    // Assume an average of 100 bytes per entry (key + set of worker names)
    // This is just an estimate and could be refined with more accurate measurements
    return entriesRemoved * 100L;
  }

  /**
   * Updates metrics for the flush operation.
   *
   * @param entriesRemoved the number of entries removed
   * @param bytesReclaimed the number of bytes reclaimed
   */
  private void updateMetrics(int entriesRemoved, long bytesReclaimed) {
    casEntriesRemovedCounter.inc(entriesRemoved);
    casBytesReclaimedGauge.inc(bytesReclaimed);
  }
}
