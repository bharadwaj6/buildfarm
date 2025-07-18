package build.buildfarm.admin.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import build.buildfarm.admin.cache.model.FlushScope;
import build.buildfarm.admin.cache.model.RateLimitExceededResponse;
import build.buildfarm.admin.cache.ratelimit.RateLimitConfig;
import build.buildfarm.admin.cache.ratelimit.RateLimitService;
import build.buildfarm.admin.cache.service.CacheFlushService;
import java.security.Principal;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link CacheFlushResource} with rate limiting.
 */
@RunWith(JUnit4.class)
public class CacheFlushResourceRateLimitTest {
  
  private CacheFlushService mockCacheFlushService;
  private RateLimitService mockRateLimitService;
  private CacheFlushResource cacheFlushResource;
  private SecurityContext mockSecurityContext;
  private Principal mockPrincipal;
  
  @Before
  public void setUp() {
    mockCacheFlushService = mock(CacheFlushService.class);
    mockRateLimitService = mock(RateLimitService.class);
    cacheFlushResource = new CacheFlushResource(mockCacheFlushService, mockRateLimitService);
    
    // Set up security context
    mockPrincipal = mock(Principal.class);
    when(mockPrincipal.getName()).thenReturn("test-user");
    
    mockSecurityContext = mock(SecurityContext.class);
    when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
    
    // Set up cache flush service responses
    when(mockCacheFlushService.flushActionCache(any()))
        .thenReturn(new ActionCacheFlushResponse(true, "Success", 10));
    when(mockCacheFlushService.flushCAS(any()))
        .thenReturn(new CASFlushResponse(true, "Success", 10, 1024));
  }
  
  @Test
  public void testFlushActionCache_rateLimitExceeded() {
    // Set up rate limit service to deny the operation
    when(mockRateLimitService.allowOperation("test-user", "action-cache-flush")).thenReturn(false);
    when(mockRateLimitService.getOperationCount("test-user", "action-cache-flush")).thenReturn(5);
    when(mockRateLimitService.getTimeRemainingInWindow("test-user", "action-cache-flush")).thenReturn(30000L);
    
    RateLimitConfig mockConfig = mock(RateLimitConfig.class);
    when(mockConfig.getMaxOperationsPerWindow()).thenReturn(5);
    when(mockConfig.getWindowSizeMs()).thenReturn(60000L);
    when(mockRateLimitService.getConfig()).thenReturn(mockConfig);
    
    // Create a request
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    
    // Call the API
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(429, response.getStatus()); // 429 Too Many Requests
    assertEquals(RateLimitExceededResponse.class, response.getEntity().getClass());
    
    RateLimitExceededResponse errorResponse = (RateLimitExceededResponse) response.getEntity();
    assertEquals("RATE_LIMIT_EXCEEDED", errorResponse.getErrorCode());
    assertEquals(5, errorResponse.getOperationsPerformed());
    assertEquals(5, errorResponse.getMaxOperationsAllowed());
    assertEquals(60000L, errorResponse.getWindowSizeMs());
    assertEquals(30000L, errorResponse.getTimeRemainingMs());
  }
  
  @Test
  public void testFlushCAS_rateLimitExceeded() {
    // Set up rate limit service to deny the operation
    when(mockRateLimitService.allowOperation("test-user", "cas-flush")).thenReturn(false);
    when(mockRateLimitService.getOperationCount("test-user", "cas-flush")).thenReturn(5);
    when(mockRateLimitService.getTimeRemainingInWindow("test-user", "cas-flush")).thenReturn(30000L);
    
    RateLimitConfig mockConfig = mock(RateLimitConfig.class);
    when(mockConfig.getMaxOperationsPerWindow()).thenReturn(5);
    when(mockConfig.getWindowSizeMs()).thenReturn(60000L);
    when(mockRateLimitService.getConfig()).thenReturn(mockConfig);
    
    // Create a request
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    
    // Call the API
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(429, response.getStatus()); // 429 Too Many Requests
    assertEquals(RateLimitExceededResponse.class, response.getEntity().getClass());
    
    RateLimitExceededResponse errorResponse = (RateLimitExceededResponse) response.getEntity();
    assertEquals("RATE_LIMIT_EXCEEDED", errorResponse.getErrorCode());
    assertEquals(5, errorResponse.getOperationsPerformed());
    assertEquals(5, errorResponse.getMaxOperationsAllowed());
    assertEquals(60000L, errorResponse.getWindowSizeMs());
    assertEquals(30000L, errorResponse.getTimeRemainingMs());
  }
  
  @Test
  public void testFlushActionCache_rateLimitNotExceeded() {
    // Set up rate limit service to allow the operation
    when(mockRateLimitService.allowOperation("test-user", "action-cache-flush")).thenReturn(true);
    
    // Create a request
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    
    // Call the API
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(200, response.getStatus()); // 200 OK
    assertEquals(ActionCacheFlushResponse.class, response.getEntity().getClass());
    
    ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
    assertEquals(true, flushResponse.isSuccess());
    assertEquals(10, flushResponse.getEntriesRemoved());
  }
  
  @Test
  public void testFlushCAS_rateLimitNotExceeded() {
    // Set up rate limit service to allow the operation
    when(mockRateLimitService.allowOperation("test-user", "cas-flush")).thenReturn(true);
    
    // Create a request
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    
    // Call the API
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(200, response.getStatus()); // 200 OK
    assertEquals(CASFlushResponse.class, response.getEntity().getClass());
    
    CASFlushResponse flushResponse = (CASFlushResponse) response.getEntity();
    assertEquals(true, flushResponse.isSuccess());
    assertEquals(10, flushResponse.getEntriesRemoved());
    assertEquals(1024, flushResponse.getBytesReclaimed());
  }
}