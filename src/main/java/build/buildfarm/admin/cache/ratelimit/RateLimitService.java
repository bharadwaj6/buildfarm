package build.buildfarm.admin.cache.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service for rate limiting API operations.
 */
public class RateLimitService {
  private static final Logger logger = Logger.getLogger(RateLimitService.class.getName());
  
  private final Map<String, RateLimiter> operationRateLimiters = new ConcurrentHashMap<>();
  private final RateLimitConfig config;
  
  /**
   * Creates a new RateLimitService with the default configuration.
   */
  public RateLimitService() {
    this(RateLimitConfig.getDefault());
  }
  
  /**
   * Creates a new RateLimitService with the specified configuration.
   *
   * @param config the rate limit configuration
   */
  public RateLimitService(RateLimitConfig config) {
    this.config = config;
    
    // Initialize rate limiters for different operation types
    if (config.isEnabled()) {
      operationRateLimiters.put("action-cache-flush", 
          new RateLimiter(config.getMaxOperationsPerWindow(), config.getWindowSizeMs()));
      operationRateLimiters.put("cas-flush", 
          new RateLimiter(config.getMaxOperationsPerWindow(), config.getWindowSizeMs()));
    }
  }
  
  /**
   * Checks if an operation is allowed for the given user and operation type.
   *
   * @param username the username
   * @param operationType the operation type
   * @return true if the operation is allowed, false otherwise
   */
  public boolean allowOperation(String username, String operationType) {
    if (!config.isEnabled()) {
      return true;
    }
    
    RateLimiter rateLimiter = operationRateLimiters.get(operationType);
    if (rateLimiter == null) {
      logger.warning("No rate limiter found for operation type: " + operationType);
      return true;
    }
    
    return rateLimiter.allowOperation(username, operationType);
  }
  
  /**
   * Gets the number of operations performed by the given user for the given operation type
   * in the current time window.
   *
   * @param username the username
   * @param operationType the operation type
   * @return the number of operations
   */
  public int getOperationCount(String username, String operationType) {
    if (!config.isEnabled()) {
      return 0;
    }
    
    RateLimiter rateLimiter = operationRateLimiters.get(operationType);
    if (rateLimiter == null) {
      return 0;
    }
    
    return rateLimiter.getOperationCount(username, operationType);
  }
  
  /**
   * Gets the time remaining in the current window for the given user and operation type.
   *
   * @param username the username
   * @param operationType the operation type
   * @return the time remaining in milliseconds, or 0 if the window has expired
   */
  public long getTimeRemainingInWindow(String username, String operationType) {
    if (!config.isEnabled()) {
      return 0;
    }
    
    RateLimiter rateLimiter = operationRateLimiters.get(operationType);
    if (rateLimiter == null) {
      return 0;
    }
    
    return rateLimiter.getTimeRemainingInWindow(username);
  }
  
  /**
   * Gets the rate limit configuration.
   *
   * @return the configuration
   */
  public RateLimitConfig getConfig() {
    return config;
  }
}