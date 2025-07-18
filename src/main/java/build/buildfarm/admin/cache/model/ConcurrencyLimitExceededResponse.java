package build.buildfarm.admin.cache.model;

/**
 * Response when a concurrency limit is exceeded.
 */
public class ConcurrencyLimitExceededResponse {
  private final String errorCode = "CONCURRENCY_LIMIT_EXCEEDED";
  private final String message;
  private final int activeOperations;
  private final int maxConcurrentOperations;
  
  /**
   * Creates a new ConcurrencyLimitExceededResponse.
   *
   * @param message the error message
   * @param activeOperations the number of active operations
   * @param maxConcurrentOperations the maximum number of concurrent operations allowed
   */
  public ConcurrencyLimitExceededResponse(
      String message,
      int activeOperations,
      int maxConcurrentOperations) {
    this.message = message;
    this.activeOperations = activeOperations;
    this.maxConcurrentOperations = maxConcurrentOperations;
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
   * Gets the number of active operations.
   *
   * @return the number of active operations
   */
  public int getActiveOperations() {
    return activeOperations;
  }
  
  /**
   * Gets the maximum number of concurrent operations allowed.
   *
   * @return the maximum number of concurrent operations allowed
   */
  public int getMaxConcurrentOperations() {
    return maxConcurrentOperations;
  }
}