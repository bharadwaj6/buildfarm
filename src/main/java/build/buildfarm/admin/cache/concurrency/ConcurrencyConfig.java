package build.buildfarm.admin.cache.concurrency;

/**
 * Configuration for concurrency controls.
 */
public class ConcurrencyConfig {
  private final int maxConcurrentActionCacheFlushes;
  private final int maxConcurrentCASFlushes;
  private final long flushOperationTimeoutMs;
  private final boolean enabled;
  
  /**
   * Creates a new ConcurrencyConfig.
   *
   * @param maxConcurrentActionCacheFlushes the maximum number of concurrent Action Cache flush operations
   * @param maxConcurrentCASFlushes the maximum number of concurrent CAS flush operations
   * @param flushOperationTimeoutMs the timeout for flush operations in milliseconds
   * @param enabled whether concurrency controls are enabled
   */
  public ConcurrencyConfig(
      int maxConcurrentActionCacheFlushes,
      int maxConcurrentCASFlushes,
      long flushOperationTimeoutMs,
      boolean enabled) {
    this.maxConcurrentActionCacheFlushes = maxConcurrentActionCacheFlushes;
    this.maxConcurrentCASFlushes = maxConcurrentCASFlushes;
    this.flushOperationTimeoutMs = flushOperationTimeoutMs;
    this.enabled = enabled;
  }
  
  /**
   * Gets the maximum number of concurrent Action Cache flush operations.
   *
   * @return the maximum number of concurrent Action Cache flush operations
   */
  public int getMaxConcurrentActionCacheFlushes() {
    return maxConcurrentActionCacheFlushes;
  }
  
  /**
   * Gets the maximum number of concurrent CAS flush operations.
   *
   * @return the maximum number of concurrent CAS flush operations
   */
  public int getMaxConcurrentCASFlushes() {
    return maxConcurrentCASFlushes;
  }
  
  /**
   * Gets the timeout for flush operations in milliseconds.
   *
   * @return the timeout for flush operations in milliseconds
   */
  public long getFlushOperationTimeoutMs() {
    return flushOperationTimeoutMs;
  }
  
  /**
   * Checks if concurrency controls are enabled.
   *
   * @return true if concurrency controls are enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled;
  }
  
  /**
   * Creates a default configuration with concurrency controls enabled.
   *
   * @return the default configuration
   */
  public static ConcurrencyConfig getDefault() {
    return new ConcurrencyConfig(5, 3, 300000, true); // 5 AC flushes, 3 CAS flushes, 5 minute timeout
  }
  
  /**
   * Creates a configuration with concurrency controls disabled.
   *
   * @return the disabled configuration
   */
  public static ConcurrencyConfig disabled() {
    return new ConcurrencyConfig(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, false);
  }
}