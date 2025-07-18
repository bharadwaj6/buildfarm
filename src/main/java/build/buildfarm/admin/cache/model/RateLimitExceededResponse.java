package build.buildfarm.admin.cache.model;

/**
 * Response when a rate limit is exceeded.
 */
public class RateLimitExceededResponse {
  private final String errorCode = "RATE_LIMIT_EXCEEDED";
  private final String message;
  private final int operationsPerformed;
  private final int maxOperationsAllowed;
  private final long windowSizeMs;
  private final long timeRemainingMs;
  
  /**
   * Creates a new RateLimitExceededResponse.
   *
   * @param message the error message
   * @param operationsPerformed the number of operations performed in the current window
   * @param maxOperationsAllowed the maximum number of operations allowed per window
   * @param windowSizeMs the size of the time window in milliseconds
   * @param timeRemainingMs the time remaining in the current window in milliseconds
   */
  public RateLimitExceededResponse(
      String message,
      int operationsPerformed,
      int maxOperationsAllowed,
      long windowSizeMs,
      long timeRemainingMs) {
    this.message = message;
    this.operationsPerformed = operationsPerformed;
    this.maxOperationsAllowed = maxOperationsAllowed;
    this.windowSizeMs = windowSizeMs;
    this.timeRemainingMs = timeRemainingMs;
  }
  
  /**
   * Gets the error code.
   *
   * @return the error code
   */
  public String getErrorCode() {
    return errorCode;
  }
  
  /**
   * Gets the error message.
   *
   * @return the error message
   */
  public String getMessage() {
    return message;
  }
  
  /**
   * Gets the number of operations performed in the current window.
   *
   * @return the number of operations
   */
  public int getOperationsPerformed() {
    return operationsPerformed;
  }
  
  /**
   * Gets the maximum number of operations allowed per window.
   *
   * @return the maximum number of operations
   */
  public int getMaxOperationsAllowed() {
    return maxOperationsAllowed;
  }
  
  /**
   * Gets the size of the time window in milliseconds.
   *
   * @return the window size
   */
  public long getWindowSizeMs() {
    return windowSizeMs;
  }
  
  /**
   * Gets the time remaining in the current window in milliseconds.
   *
   * @return the time remaining
   */
  public long getTimeRemainingMs() {
    return timeRemainingMs;
  }
}