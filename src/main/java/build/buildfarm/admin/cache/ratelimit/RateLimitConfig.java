package build.buildfarm.admin.cache.ratelimit;

/**
 * Configuration for rate limiting.
 */
public class RateLimitConfig {
  private final int maxOperationsPerWindow;
  private final long windowSizeMs;
  private final boolean enabled;
  
  /**
   * Creates a new RateLimitConfig.
   *
   * @param maxOperationsPerWindow the maximum number of operations allowed per time window
   * @param windowSizeMs the size of the time window in milliseconds
   * @param enabled whether rate limiting is enabled
   */
  public RateLimitConfig(int maxOperationsPerWindow, long windowSizeMs, boolean enabled) {
    this.maxOperationsPerWindow = maxOperationsPerWindow;
    this.windowSizeMs = windowSizeMs;
    this.enabled = enabled;
  }
  
  /**
   * Gets the maximum number of operations allowed per time window.
   *
   * @return the maximum number of operations
   */
  public int getMaxOperationsPerWindow() {
    return maxOperationsPerWindow;
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
   * Checks if rate limiting is enabled.
   *
   * @return true if rate limiting is enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled;
  }
  
  /**
   * Creates a default configuration with rate limiting enabled.
   *
   * @return the default configuration
   */
  public static RateLimitConfig getDefault() {
    return new RateLimitConfig(10, 60000, true); // 10 operations per minute
  }
  
  /**
   * Creates a configuration with rate limiting disabled.
   *
   * @return the disabled configuration
   */
  public static RateLimitConfig disabled() {
    return new RateLimitConfig(0, 0, false);
  }
}