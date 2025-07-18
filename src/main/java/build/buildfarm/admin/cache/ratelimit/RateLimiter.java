package build.buildfarm.admin.cache.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * A simple rate limiter that limits the number of operations per time window.
 */
public class RateLimiter {
  private static final Logger logger = Logger.getLogger(RateLimiter.class.getName());
  
  private final Map<String, Map<String, AtomicInteger>> userOperationCounts = new ConcurrentHashMap<>();
  private final Map<String, Long> windowStartTimes = new ConcurrentHashMap<>();
  private final int maxOperationsPerWindow;
  private final long windowSizeMs;
  
  /**
   * Creates a new RateLimiter.
   *
   * @param maxOperationsPerWindow the maximum number of operations allowed per time window
   * @param windowSizeMs the size of the time window in milliseconds
   */
  public RateLimiter(int maxOperationsPerWindow, long windowSizeMs) {
    this.maxOperationsPerWindow = maxOperationsPerWindow;
    this.windowSizeMs = windowSizeMs;
  }
  
  /**
   * Checks if an operation is allowed for the given user and operation type.
   *
   * @param username the username
   * @param operationType the operation type
   * @return true if the operation is allowed, false otherwise
   */
  public boolean allowOperation(String username, String operationType) {
    long currentTime = System.currentTimeMillis();
    
    // Reset counts if the window has expired
    windowStartTimes.compute(username, (key, startTime) -> {
      if (startTime == null || currentTime - startTime > windowSizeMs) {
        // Reset counts for this user
        userOperationCounts.put(username, new ConcurrentHashMap<>());
        return currentTime;
      }
      return startTime;
    });
    
    // Get or create the operation counts for this user
    Map<String, AtomicInteger> operationCounts = userOperationCounts.computeIfAbsent(
        username, k -> new ConcurrentHashMap<>());
    
    // Get or create the count for this operation type
    AtomicInteger count = operationCounts.computeIfAbsent(
        operationType, k -> new AtomicInteger(0));
    
    // Check if the operation is allowed
    if (count.get() >= maxOperationsPerWindow) {
      logger.warning(String.format(
          "Rate limit exceeded for user '%s' and operation '%s': %d operations in %d ms",
          username, operationType, count.get(), windowSizeMs));
      return false;
    }
    
    // Increment the count and allow the operation
    count.incrementAndGet();
    return true;
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
    Map<String, AtomicInteger> operationCounts = userOperationCounts.get(username);
    if (operationCounts == null) {
      return 0;
    }
    
    AtomicInteger count = operationCounts.get(operationType);
    return count != null ? count.get() : 0;
  }
  
  /**
   * Gets the time remaining in the current window for the given user.
   *
   * @param username the username
   * @return the time remaining in milliseconds, or 0 if the window has expired
   */
  public long getTimeRemainingInWindow(String username) {
    Long startTime = windowStartTimes.get(username);
    if (startTime == null) {
      return 0;
    }
    
    long timeElapsed = System.currentTimeMillis() - startTime;
    return Math.max(0, windowSizeMs - timeElapsed);
  }
}