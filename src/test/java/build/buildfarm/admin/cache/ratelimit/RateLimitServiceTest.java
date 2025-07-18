package build.buildfarm.admin.cache.ratelimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link RateLimitService}.
 */
@RunWith(JUnit4.class)
public class RateLimitServiceTest {
  
  private RateLimitService rateLimitService;
  private static final String TEST_USER = "test-user";
  
  @Before
  public void setUp() {
    // Create a rate limit service with a custom configuration
    RateLimitConfig config = new RateLimitConfig(3, 1000, true);
    rateLimitService = new RateLimitService(config);
  }
  
  @Test
  public void testAllowOperation_actionCacheFlush() {
    // First operation should be allowed
    assertTrue(rateLimitService.allowOperation(TEST_USER, "action-cache-flush"));
    assertEquals(1, rateLimitService.getOperationCount(TEST_USER, "action-cache-flush"));
    
    // Second operation should be allowed
    assertTrue(rateLimitService.allowOperation(TEST_USER, "action-cache-flush"));
    assertEquals(2, rateLimitService.getOperationCount(TEST_USER, "action-cache-flush"));
    
    // Third operation should be allowed
    assertTrue(rateLimitService.allowOperation(TEST_USER, "action-cache-flush"));
    assertEquals(3, rateLimitService.getOperationCount(TEST_USER, "action-cache-flush"));
    
    // Fourth operation should be denied
    assertFalse(rateLimitService.allowOperation(TEST_USER, "action-cache-flush"));
    assertEquals(3, rateLimitService.getOperationCount(TEST_USER, "action-cache-flush"));
  }
  
  @Test
  public void testAllowOperation_casFlush() {
    // First operation should be allowed
    assertTrue(rateLimitService.allowOperation(TEST_USER, "cas-flush"));
    assertEquals(1, rateLimitService.getOperationCount(TEST_USER, "cas-flush"));
    
    // Second operation should be allowed
    assertTrue(rateLimitService.allowOperation(TEST_USER, "cas-flush"));
    assertEquals(2, rateLimitService.getOperationCount(TEST_USER, "cas-flush"));
    
    // Third operation should be allowed
    assertTrue(rateLimitService.allowOperation(TEST_USER, "cas-flush"));
    assertEquals(3, rateLimitService.getOperationCount(TEST_USER, "cas-flush"));
    
    // Fourth operation should be denied
    assertFalse(rateLimitService.allowOperation(TEST_USER, "cas-flush"));
    assertEquals(3, rateLimitService.getOperationCount(TEST_USER, "cas-flush"));
  }
  
  @Test
  public void testAllowOperation_differentOperationTypes() {
    // Action Cache flush operations
    assertTrue(rateLimitService.allowOperation(TEST_USER, "action-cache-flush"));
    assertTrue(rateLimitService.allowOperation(TEST_USER, "action-cache-flush"));
    assertTrue(rateLimitService.allowOperation(TEST_USER, "action-cache-flush"));
    assertFalse(rateLimitService.allowOperation(TEST_USER, "action-cache-flush"));
    
    // CAS flush operations should be tracked separately
    assertTrue(rateLimitService.allowOperation(TEST_USER, "cas-flush"));
    assertEquals(1, rateLimitService.getOperationCount(TEST_USER, "cas-flush"));
  }
  
  @Test
  public void testAllowOperation_disabledRateLimiting() {
    // Create a rate limit service with rate limiting disabled
    RateLimitConfig config = RateLimitConfig.disabled();
    RateLimitService disabledService = new RateLimitService(config);
    
    // All operations should be allowed
    for (int i = 0; i < 10; i++) {
      assertTrue(disabledService.allowOperation(TEST_USER, "action-cache-flush"));
      assertTrue(disabledService.allowOperation(TEST_USER, "cas-flush"));
    }
    
    // Operation counts should be 0 when disabled
    assertEquals(0, disabledService.getOperationCount(TEST_USER, "action-cache-flush"));
    assertEquals(0, disabledService.getOperationCount(TEST_USER, "cas-flush"));
  }
  
  @Test
  public void testGetTimeRemainingInWindow() {
    // Perform an operation to start the window
    assertTrue(rateLimitService.allowOperation(TEST_USER, "action-cache-flush"));
    
    // Time remaining should be positive and less than or equal to the window size
    long timeRemaining = rateLimitService.getTimeRemainingInWindow(TEST_USER, "action-cache-flush");
    assertTrue(timeRemaining > 0);
    assertTrue(timeRemaining <= 1000);
  }
}