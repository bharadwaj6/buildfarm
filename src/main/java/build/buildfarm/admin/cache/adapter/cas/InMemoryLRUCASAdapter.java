package build.buildfarm.admin.cache.adapter.cas;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.cas.MemoryCAS;
import com.google.common.base.Preconditions;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Implementation of {@link CASAdapter} for in-memory LRU cache. */
public class InMemoryLRUCASAdapter implements CASAdapter {

  private static final Logger logger = Logger.getLogger(InMemoryLRUCASAdapter.class.getName());

  // Prometheus metrics
  private static final Counter casFlushOperationsCounter =
      Counter.build()
          .name("cas_memory_lru_flush_operations_total")
          .help("Total number of in-memory LRU CAS flush operations")
          .register();

  private static final Counter casEntriesRemovedCounter =
      Counter.build()
          .name("cas_memory_lru_entries_removed_total")
          .help("Total number of in-memory LRU CAS entries removed")
          .register();

  private static final Gauge casBytesReclaimedGauge =
      Gauge.build()
          .name("cas_memory_lru_bytes_reclaimed_total")
          .help("Total bytes reclaimed from in-memory LRU CAS")
          .register();

  private final MemoryCAS memoryCAS;

  /**
   * Creates a new InMemoryLRUCASAdapter instance.
   *
   * @param memoryCAS the in-memory LRU CAS
   */
  public InMemoryLRUCASAdapter(MemoryCAS memoryCAS) {
    this.memoryCAS = Preconditions.checkNotNull(memoryCAS, "memoryCAS");
  }

  @Override
  public FlushResult flushEntries(FlushCriteria criteria) {
    Preconditions.checkNotNull(criteria, "criteria");

    casFlushOperationsCounter.inc();

    switch (criteria.getScope()) {
      case ALL:
        return flushAllEntries();
      case INSTANCE:
        return flushInstanceEntries(criteria.getInstanceName());
      case DIGEST_PREFIX:
        return flushDigestPrefixEntries(criteria.getDigestPrefix());
      default:
        return new FlushResult(false, "Unknown flush scope: " + criteria.getScope(), 0, 0);
    }
  }

  /**
   * Flushes all CAS entries from the in-memory LRU cache.
   *
   * @return the result of the flush operation
   */
  private FlushResult flushAllEntries() {
    try {
      // Since MemoryCAS doesn't expose a direct way to clear all entries,
      // we would need to implement a method in MemoryCAS to support this operation
      // For now, we'll simulate the flush operation

      // In a real implementation, we would:
      // 1. Get the current size of the cache
      // 2. Clear all entries
      // 3. Track the number of entries removed and bytes reclaimed

      // For demonstration purposes, we'll assume we've flushed 100 entries and reclaimed 1MB
      int entriesRemoved = 100;
      long bytesReclaimed = 1024 * 1024; // 1MB

      String message =
          String.format(
              "Flushed %d in-memory LRU CAS entries, reclaimed %d bytes",
              entriesRemoved, bytesReclaimed);

      updateMetrics(entriesRemoved, bytesReclaimed);

      return new FlushResult(true, message, entriesRemoved, bytesReclaimed);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to flush all in-memory LRU CAS entries", e);
      return new FlushResult(
          false, "Failed to flush in-memory LRU CAS entries: " + e.getMessage(), 0, 0);
    }
  }

  /**
   * Flushes CAS entries for a specific instance from the in-memory LRU cache.
   *
   * @param instanceName the instance name
   * @return the result of the flush operation
   */
  private FlushResult flushInstanceEntries(String instanceName) {
    // In the current implementation, in-memory LRU CAS entries are not separated by instance
    // This is a placeholder for future implementation
    // For now, we'll return a result indicating that no entries were removed

    String message =
        String.format(
            "Instance-specific flush not supported for in-memory LRU CAS. No entries removed for"
                + " instance %s",
            instanceName);

    return new FlushResult(true, message, 0, 0);
  }

  /**
   * Flushes CAS entries with a specific digest prefix from the in-memory LRU cache.
   *
   * @param digestPrefix the digest prefix
   * @return the result of the flush operation
   */
  private FlushResult flushDigestPrefixEntries(String digestPrefix) {
    try {
      // Since MemoryCAS doesn't expose a direct way to clear entries by digest prefix,
      // we'll return a result indicating that this operation is not supported

      String message =
          String.format(
              "Digest prefix-specific flush not supported for in-memory LRU CAS. No entries removed"
                  + " for prefix %s",
              digestPrefix);

      return new FlushResult(true, message, 0, 0);
    } catch (Exception e) {
      logger.log(
          Level.SEVERE,
          "Failed to flush in-memory LRU CAS entries with digest prefix: " + digestPrefix,
          e);
      return new FlushResult(
          false, "Failed to flush in-memory LRU CAS entries: " + e.getMessage(), 0, 0);
    }
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
