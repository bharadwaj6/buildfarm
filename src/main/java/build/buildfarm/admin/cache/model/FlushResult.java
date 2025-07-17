package build.buildfarm.admin.cache.model;

/**
 * Result of a cache flush operation.
 */
public class FlushResult {
  private boolean success;
  private String message;
  private int entriesRemoved;
  private long bytesReclaimed;

  /**
   * Creates a new FlushResult instance.
   */
  public FlushResult() {
    this.success = true;
    this.message = "";
    this.entriesRemoved = 0;
    this.bytesReclaimed = 0;
  }

  /**
   * Creates a new FlushResult instance.
   *
   * @param success whether the operation was successful
   * @param message a message describing the result
   * @param entriesRemoved the number of entries removed
   * @param bytesReclaimed the number of bytes reclaimed
   */
  public FlushResult(boolean success, String message, int entriesRemoved, long bytesReclaimed) {
    this.success = success;
    this.message = message;
    this.entriesRemoved = entriesRemoved;
    this.bytesReclaimed = bytesReclaimed;
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
   * Merges another FlushResult into this one.
   *
   * @param other the other FlushResult to merge
   */
  public void merge(FlushResult other) {
    this.success = this.success && other.success;
    this.message = this.message + (this.message.isEmpty() ? "" : ", ") + other.message;
    this.entriesRemoved += other.entriesRemoved;
    this.bytesReclaimed += other.bytesReclaimed;
  }
}