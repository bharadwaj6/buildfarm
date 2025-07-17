package build.buildfarm.admin.cache.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Response from flushing Action Cache entries.
 */
public class ActionCacheFlushResponse {
  private boolean success;
  private String message;
  private int entriesRemoved;
  private Map<String, Integer> entriesRemovedByBackend;

  /**
   * Creates a new ActionCacheFlushResponse instance.
   */
  public ActionCacheFlushResponse() {
    this.success = true;
    this.message = "";
    this.entriesRemoved = 0;
    this.entriesRemovedByBackend = new HashMap<>();
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
   * Adds entries removed for a specific backend.
   *
   * @param backend the backend name
   * @param entriesRemoved the number of entries removed
   */
  public void addEntriesRemovedByBackend(String backend, int entriesRemoved) {
    this.entriesRemovedByBackend.put(backend, entriesRemoved);
    this.entriesRemoved += entriesRemoved;
  }
}