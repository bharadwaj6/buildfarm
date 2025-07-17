package build.buildfarm.admin.cache.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Response from flushing Content Addressable Storage entries.
 */
public class CASFlushResponse {
  private boolean success;
  private String message;
  private int entriesRemoved;
  private long bytesReclaimed;
  private Map<String, Integer> entriesRemovedByBackend;
  private Map<String, Long> bytesReclaimedByBackend;

  /**
   * Creates a new CASFlushResponse instance.
   */
  public CASFlushResponse() {
    this.success = true;
    this.message = "";
    this.entriesRemoved = 0;
    this.bytesReclaimed = 0;
    this.entriesRemovedByBackend = new HashMap<>();
    this.bytesReclaimedByBackend = new HashMap<>();
  }

  /**
   * Gets whether the operation was successful.
   *
   * @return true if the operation was successful, false otherwise
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * Sets whether the operation was successful.
   *
   * @param success true if the operation was successful, false otherwise
   */
  public void setSuccess(boolean success) {
    this.success = success;
  }

  /**
   * Gets the message describing the result.
   *
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message describing the result.
   *
   * @param message the message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Gets the number of entries removed.
   *
   * @return the number of entries removed
   */
  public int getEntriesRemoved() {
    return entriesRemoved;
  }

  /**
   * Sets the number of entries removed.
   *
   * @param entriesRemoved the number of entries removed
   */
  public void setEntriesRemoved(int entriesRemoved) {
    this.entriesRemoved = entriesRemoved;
  }

  /**
   * Gets the number of bytes reclaimed.
   *
   * @return the number of bytes reclaimed
   */
  public long getBytesReclaimed() {
    return bytesReclaimed;
  }

  /**
   * Sets the number of bytes reclaimed.
   *
   * @param bytesReclaimed the number of bytes reclaimed
   */
  public void setBytesReclaimed(long bytesReclaimed) {
    this.bytesReclaimed = bytesReclaimed;
  }

  /**
   * Gets the number of entries removed by backend.
   *
   * @return the number of entries removed by backend
   */
  public Map<String, Integer> getEntriesRemovedByBackend() {
    return entriesRemovedByBackend;
  }

  /**
   * Sets the number of entries removed by backend.
   *
   * @param entriesRemovedByBackend the number of entries removed by backend
   */
  public void setEntriesRemovedByBackend(Map<String, Integer> entriesRemovedByBackend) {
    this.entriesRemovedByBackend = entriesRemovedByBackend;
  }

  /**
   * Gets the number of bytes reclaimed by backend.
   *
   * @return the number of bytes reclaimed by backend
   */
  public Map<String, Long> getBytesReclaimedByBackend() {
    return bytesReclaimedByBackend;
  }

  /**
   * Sets the number of bytes reclaimed by backend.
   *
   * @param bytesReclaimedByBackend the number of bytes reclaimed by backend
   */
  public void setBytesReclaimedByBackend(Map<String, Long> bytesReclaimedByBackend) {
    this.bytesReclaimedByBackend = bytesReclaimedByBackend;
  }

  /**
   * Adds entries removed and bytes reclaimed for a specific backend.
   *
   * @param backend the backend name
   * @param entriesRemoved the number of entries removed
   * @param bytesReclaimed the number of bytes reclaimed
   */
  public void addBackendResult(String backend, int entriesRemoved, long bytesReclaimed) {
    this.entriesRemovedByBackend.put(backend, entriesRemoved);
    this.bytesReclaimedByBackend.put(backend, bytesReclaimed);
    this.entriesRemoved += entriesRemoved;
    this.bytesReclaimed += bytesReclaimed;
  }
}