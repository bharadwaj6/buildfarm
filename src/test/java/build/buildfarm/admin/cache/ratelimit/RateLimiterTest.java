package build.buildfarm.admin.cache.ratelimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link RateLimiter}.
 */
@RunWith(JUnit4.class)
public class RateLimiterTest {
  
  private RateLimiter rateLimiter;
  private static final String TEST_USER = "test-user";
  private static final String TEST_OPERATION = "test-operation";
  
  @Before
  public void setUp() {
    // Create a rate limiter with a limit of 3 operations per 1000ms
    rateLimiter = new RateLimiter(3, 1000);
  }
  
  @Test
  public void testAllowOperation_underLimit() {
    // First operation should be allowed
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertEquals(1, rateLimiter.getOperationCount(TEST_USER, TEST_OPERATION));
    
    // Second operation should be allowed
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertEquals(2, rateLimiter.getOperationCount(TEST_USER, TEST_OPERATION));
    
    // Third operation should be allowed
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertEquals(3, rateLimiter.getOperationCount(TEST_USER, TEST_OPERATION));
  }
  
  @Test
  public void testAllowOperation_atLimit() {
    // Perform operations up to the limit
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    
    // Fourth operation should be denied
    assertFalse(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertEquals(3, rateLimiter.getOperationCount(TEST_USER, TEST_OPERATION));
  }
  
  @Test
  public void testAllowOperation_differentUsers() {
    // First user performs operations
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertFalse(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    
    // Second user should be allowed to perform operations
    String secondUser = "second-user";
    assertTrue(rateLimiter.allowOperation(secondUser, TEST_OPERATION));
    assertEquals(1, rateLimiter.getOperationCount(secondUser, TEST_OPERATION));
  }
  
  @Test
  public void testAllowOperation_differentOperations() {
    // First operation type
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    assertFalse(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    
    // Second operation type should be allowed
    String secondOperation = "second-operation";
    assertTrue(rateLimiter.allowOperation(TEST_USER, secondOperation));
    assertEquals(1, rateLimiter.getOperationCount(TEST_USER, secondOperation));
  }
  
  @Test
  public void testGetTimeRemainingInWindow() {
    // Perform an operation to start the window
    assertTrue(rateLimiter.allowOperation(TEST_USER, TEST_OPERATION));
    
    // Time remaining should be positive and less than or equal to the window size
    long timeRemaining = rateLimiter.getTimeRemainingInWindow(TEST_USER);
    assertTrue(timeRemaining > 0);
    assertTrue(timeRemaining <= 1000);
  }
}