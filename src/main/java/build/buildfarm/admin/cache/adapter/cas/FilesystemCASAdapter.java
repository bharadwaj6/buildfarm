package build.buildfarm.admin.cache.adapter.cas;

import build.bazel.remote.execution.v2.DigestFunction;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.cas.cfc.CASFileCache;
import build.buildfarm.common.DigestUtil;
import build.buildfarm.v1test.Digest;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Implementation of {@link CASAdapter} for filesystem-backed Content Addressable Storage. */
public class FilesystemCASAdapter implements CASAdapter {

  private static final Logger logger = Logger.getLogger(FilesystemCASAdapter.class.getName());

  // Prometheus metrics
  private static final Counter casFlushOperationsCounter =
      Counter.build()
          .name("cas_filesystem_flush_operations_total")
          .help("Total number of filesystem CAS flush operations")
          .register();

  private static final Counter casEntriesRemovedCounter =
      Counter.build()
          .name("cas_filesystem_entries_removed_total")
          .help("Total number of filesystem CAS entries removed")
          .register();

  private static final Gauge casBytesReclaimedGauge =
      Gauge.build()
          .name("cas_filesystem_bytes_reclaimed_total")
          .help("Total bytes reclaimed from filesystem CAS")
          .register();

  private final CASFileCache fileCache;

  /**
   * Creates a new FilesystemCASAdapter instance.
   *
   * @param fileCache the CAS file cache
   */
  public FilesystemCASAdapter(CASFileCache fileCache) {
    this.fileCache = Preconditions.checkNotNull(fileCache, "fileCache");
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
   * Flushes all CAS entries from the filesystem.
   *
   * @return the result of the flush operation
   */
  private FlushResult flushAllEntries() {
    try {
      List<Path> filesToDelete = findAllCasFiles();
      long bytesReclaimed = calculateTotalSize(filesToDelete);
      int entriesRemoved = deleteFiles(filesToDelete);

      String message = String.format("Flushed %d CAS entries from filesystem", entriesRemoved);
      updateMetrics(entriesRemoved, bytesReclaimed);

      return new FlushResult(true, message, entriesRemoved, bytesReclaimed);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to flush all CAS entries", e);
      return new FlushResult(false, "Failed to flush CAS entries: " + e.getMessage(), 0, 0);
    }
  }

  /**
   * Flushes CAS entries for a specific instance from the filesystem.
   *
   * @param instanceName the instance name
   * @return the result of the flush operation
   */
  private FlushResult flushInstanceEntries(String instanceName) {
    try {
      List<Path> filesToDelete = findCasFilesForInstance(instanceName);
      long bytesReclaimed = calculateTotalSize(filesToDelete);
      int entriesRemoved = deleteFiles(filesToDelete);

      String message =
          String.format(
              "Flushed %d CAS entries for instance %s from filesystem",
              entriesRemoved, instanceName);
      updateMetrics(entriesRemoved, bytesReclaimed);

      return new FlushResult(true, message, entriesRemoved, bytesReclaimed);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to flush CAS entries for instance: " + instanceName, e);
      return new FlushResult(false, "Failed to flush CAS entries: " + e.getMessage(), 0, 0);
    }
  }

  /**
   * Flushes CAS entries with a specific digest prefix from the filesystem.
   *
   * @param digestPrefix the digest prefix
   * @return the result of the flush operation
   */
  private FlushResult flushDigestPrefixEntries(String digestPrefix) {
    try {
      List<Path> filesToDelete = findCasFilesWithDigestPrefix(digestPrefix);
      long bytesReclaimed = calculateTotalSize(filesToDelete);
      int entriesRemoved = deleteFiles(filesToDelete);

      String message =
          String.format(
              "Flushed %d CAS entries with digest prefix %s from filesystem",
              entriesRemoved, digestPrefix);
      updateMetrics(entriesRemoved, bytesReclaimed);

      return new FlushResult(true, message, entriesRemoved, bytesReclaimed);
    } catch (IOException e) {
      logger.log(
          Level.SEVERE, "Failed to flush CAS entries with digest prefix: " + digestPrefix, e);
      return new FlushResult(false, "Failed to flush CAS entries: " + e.getMessage(), 0, 0);
    }
  }

  /**
   * Finds all CAS files in the filesystem.
   *
   * @return a list of paths to CAS files
   * @throws IOException if an I/O error occurs
   */
  @VisibleForTesting
  List<Path> findAllCasFiles() throws IOException {
    Path cacheRoot = fileCache.getRoot();
    List<Path> result = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(cacheRoot)) {
      result =
          paths.filter(Files::isRegularFile).filter(this::isCasFile).collect(Collectors.toList());
    }

    return result;
  }

  /**
   * Finds CAS files for a specific instance in the filesystem.
   *
   * @param instanceName the instance name
   * @return a list of paths to CAS files for the instance
   * @throws IOException if an I/O error occurs
   */
  @VisibleForTesting
  List<Path> findCasFilesForInstance(String instanceName) throws IOException {
    // In the current implementation, CAS files are not separated by instance
    // This is a placeholder for future implementation
    // For now, we'll return an empty list
    return ImmutableList.of();
  }

  /**
   * Finds CAS files with a specific digest prefix in the filesystem.
   *
   * @param digestPrefix the digest prefix
   * @return a list of paths to CAS files with the digest prefix
   * @throws IOException if an I/O error occurs
   */
  @VisibleForTesting
  List<Path> findCasFilesWithDigestPrefix(String digestPrefix) throws IOException {
    Path cacheRoot = fileCache.getRoot();
    List<Path> result = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(cacheRoot)) {
      result =
          paths
              .filter(Files::isRegularFile)
              .filter(this::isCasFile)
              .filter(path -> hasDigestPrefix(path, digestPrefix))
              .collect(Collectors.toList());
    }

    return result;
  }

  /**
   * Checks if a file is a CAS file.
   *
   * @param path the path to check
   * @return true if the file is a CAS file, false otherwise
   */
  @VisibleForTesting
  boolean isCasFile(Path path) {
    String fileName = path.getFileName().toString();

    // CAS files have a specific naming pattern: hash_exec or hash
    // They don't end with "_dir" which indicates a directory entry
    return !fileName.endsWith("_dir") && (fileName.contains("_") || isLikelyValidHash(fileName));
  }

  /**
   * Checks if a string is likely a valid hash. This is a simple implementation that checks if the
   * string contains only hexadecimal characters.
   *
   * @param hash the hash to check
   * @return true if the hash is likely valid, false otherwise
   */
  private boolean isLikelyValidHash(String hash) {
    // Simple check: hash should be at least 8 characters long and contain only hex characters
    if (hash.length() < 8) {
      return false;
    }

    for (char c : hash.toCharArray()) {
      if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if a file has a specific digest prefix.
   *
   * @param path the path to check
   * @param digestPrefix the digest prefix
   * @return true if the file has the digest prefix, false otherwise
   */
  @VisibleForTesting
  boolean hasDigestPrefix(Path path, String digestPrefix) {
    String fileName = path.getFileName().toString();

    // Extract the hash part from the file name
    String hash;
    if (fileName.contains("_")) {
      // Format: hash_exec or digestFunction_hash_exec
      String[] parts = fileName.split("_");
      if (parts.length == 2) {
        hash = parts[0]; // hash_exec format
      } else if (parts.length == 3) {
        hash = parts[1]; // digestFunction_hash_exec format
      } else {
        return false;
      }
    } else {
      // Just the hash
      hash = fileName;
    }

    return hash.startsWith(digestPrefix);
  }

  /**
   * Calculates the total size of the given files.
   *
   * @param files the files to calculate the size of
   * @return the total size in bytes
   */
  @VisibleForTesting
  long calculateTotalSize(List<Path> files) {
    long totalSize = 0;

    for (Path file : files) {
      try {
        totalSize += Files.size(file);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to get size of file: " + file, e);
      }
    }

    return totalSize;
  }

  /**
   * Deletes the given files.
   *
   * @param files the files to delete
   * @return the number of files successfully deleted
   */
  @VisibleForTesting
  int deleteFiles(List<Path> files) {
    int deletedCount = 0;

    for (Path file : files) {
      try {
        // First, try to remove the file from the CAS cache if it's tracked there
        String fileName = file.getFileName().toString();
        if (fileName.contains("_exec")) {
          // This is an executable file
          String[] parts = fileName.split("_");
          String hash = parts.length == 3 ? parts[1] : parts[0];
          Digest digest =
              DigestUtil.buildDigest(hash, Files.size(file), DigestFunction.Value.SHA256);
          // We can't directly call invalidateWrite as it's not public
          // This is a limitation of the current implementation
        } else if (!fileName.contains("_")) {
          // This is a regular file with just the hash
          Digest digest =
              DigestUtil.buildDigest(fileName, Files.size(file), DigestFunction.Value.SHA256);
          // We can't directly call invalidateWrite as it's not public
          // This is a limitation of the current implementation
        }

        // Now delete the file
        Files.delete(file);
        deletedCount++;
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to delete file: " + file, e);
      }
    }

    return deletedCount;
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
