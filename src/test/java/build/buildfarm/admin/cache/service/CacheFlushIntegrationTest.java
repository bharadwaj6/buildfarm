package build.buildfarm.admin.cache.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.api.CacheFlushResource;
import build.buildfarm.admin.cache.adapter.ac.ActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.ac.InMemoryActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.ac.RedisActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.cas.CASAdapter;
import build.buildfarm.admin.cache.adapter.cas.FilesystemCASAdapter;
import build.buildfarm.admin.cache.adapter.cas.InMemoryLRUCASAdapter;
import build.buildfarm.admin.cache.adapter.cas.RedisCASWorkerMapAdapter;
import build.buildfarm.admin.cache.concurrency.ConcurrencyConfig;
import build.buildfarm.admin.cache.concurrency.ConcurrencyControlService;
import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import build.buildfarm.admin.cache.ratelimit.RateLimitConfig;
import build.buildfarm.admin.cache.ratelimit.RateLimitService;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

/**
 * Integration tests for the Cache Flush API.
 * These tests verify end-to-end functionality and interactions between components.
 */
@RunWith(JUnit4.class)
public class CacheFlushIntegrationTest {

  private CacheFlushService cacheFlushService;
  private CacheFlushResource cacheFlushResource;
  private Map<String, ActionCacheAdapter> actionCacheAdapters;
  private Map<String, CASAdapter> casAdapters;
  private ConcurrencyControlService concurrencyControlService;
  private RateLimitService rateLimitService;
  private SecurityContext mockSecurityContext;
  private Principal mockPrincipal;

  @Before
  public void setUp() {
    // Set up mock adapters
    actionCacheAdapters = new HashMap<>();
    casAdapters = new HashMap<>();
    
    // Create real concurrency control service with a test configuration
    ConcurrencyConfig concurrencyConfig = new ConcurrencyConfig(3, 2, 1000, true);
    concurrencyControlService = new ConcurrencyControlService(concurrencyConfig);
    
    // Create real rate limit service with a test configuration
    RateLimitConfig rateLimitConfig = new RateLimitConfig(5, 60000, true);
    rateLimitService = new RateLimitService(rateLimitConfig);
    
    // Set up security context
    mockPrincipal = mock(Principal.class);
    when(mockPrincipal.getName()).thenReturn("test-user");
    
    mockSecurityContext = mock(SecurityContext.class);
    when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
    
    // Create the service with the mock adapters
    cacheFlushService = new CacheFlushServiceImpl(actionCacheAdapters, casAdapters, concurrencyControlService);
    
    // Create the resource with the service and rate limit service
    cacheFlushResource = new CacheFlushResource(cacheFlushService, rateLimitService);
  }

  @Test
  public void testEndToEndActionCacheFlush() {
    // Set up mock adapters
    ActionCacheAdapter mockRedisAdapter = mock(RedisActionCacheAdapter.class);
    when(mockRedisAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Redis flush successful", 50, 0));
    actionCacheAdapters.put("redis", mockRedisAdapter);
    
    ActionCacheAdapter mockInMemoryAdapter = mock(InMemoryActionCacheAdapter.class);
    when(mockInMemoryAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "In-memory flush successful", 30, 0));
    actionCacheAdapters.put("in-memory", mockInMemoryAdapter);
    
    // Create a request
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // Call the API
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(response.getEntity() instanceof ActionCacheFlushResponse);
    
    ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    assertEquals(80, flushResponse.getEntriesRemoved());
    assertEquals(2, flushResponse.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(50), flushResponse.getEntriesRemovedByBackend().get("redis"));
    assertEquals(Integer.valueOf(30), flushResponse.getEntriesRemovedByBackend().get("in-memory"));
    
    // Verify that the adapters were called with the correct criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockRedisAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockInMemoryAdapter).flushEntries(criteriaCaptor.capture());
    
    // Verify the criteria
    for (FlushCriteria criteria : criteriaCaptor.getAllValues()) {
      assertEquals(FlushScope.ALL, criteria.getScope());
      assertNull(criteria.getInstanceName());
      assertNull(criteria.getDigestPrefix());
    }
  }

  @Test
  public void testEndToEndCASFlush() {
    // Set up mock adapters
    CASAdapter mockFilesystemAdapter = mock(FilesystemCASAdapter.class);
    when(mockFilesystemAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Filesystem flush successful", 100, 1024 * 1024 * 50));
    casAdapters.put("filesystem", mockFilesystemAdapter);
    
    CASAdapter mockInMemoryLRUAdapter = mock(InMemoryLRUCASAdapter.class);
    when(mockInMemoryLRUAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "In-memory LRU flush successful", 20, 1024 * 1024 * 10));
    casAdapters.put("in-memory-lru", mockInMemoryLRUAdapter);
    
    CASAdapter mockRedisWorkerMapAdapter = mock(RedisCASWorkerMapAdapter.class);
    when(mockRedisWorkerMapAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Redis worker map flush successful", 30, 0));
    casAdapters.put("redis-worker-map", mockRedisWorkerMapAdapter);
    
    // Create a request
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    request.setFlushRedisWorkerMap(true);
    
    // Call the API
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertTrue(response.getEntity() instanceof CASFlushResponse);
    
    CASFlushResponse flushResponse = (CASFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    assertEquals(150, flushResponse.getEntriesRemoved());
    assertEquals(1024 * 1024 * 60, flushResponse.getBytesReclaimed());
    assertEquals(3, flushResponse.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(100), flushResponse.getEntriesRemovedByBackend().get("filesystem"));
    assertEquals(Integer.valueOf(20), flushResponse.getEntriesRemovedByBackend().get("in-memory-lru"));
    assertEquals(Integer.valueOf(30), flushResponse.getEntriesRemovedByBackend().get("redis-worker-map"));
    assertEquals(2, flushResponse.getBytesReclaimedByBackend().size());
    assertEquals(Long.valueOf(1024 * 1024 * 50), flushResponse.getBytesReclaimedByBackend().get("filesystem"));
    assertEquals(Long.valueOf(1024 * 1024 * 10), flushResponse.getBytesReclaimedByBackend().get("in-memory-lru"));
    
    // Verify that the adapters were called with the correct criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockFilesystemAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockInMemoryLRUAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockRedisWorkerMapAdapter).flushEntries(criteriaCaptor.capture());
    
    // Verify the criteria
    for (FlushCriteria criteria : criteriaCaptor.getAllValues()) {
      assertEquals(FlushScope.ALL, criteria.getScope());
      assertNull(criteria.getInstanceName());
      assertNull(criteria.getDigestPrefix());
    }
  }

  @Test
  public void testInstanceSpecificActionCacheFlush() {
    // Set up mock adapters
    ActionCacheAdapter mockRedisAdapter = mock(RedisActionCacheAdapter.class);
    when(mockRedisAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Redis flush successful", 25, 0));
    actionCacheAdapters.put("redis", mockRedisAdapter);
    
    // Create a request for a specific instance
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.INSTANCE);
    request.setInstanceName("test-instance");
    request.setFlushRedis(true);
    
    // Call the API
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    assertEquals(25, flushResponse.getEntriesRemoved());
    
    // Verify that the adapter was called with the correct criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockRedisAdapter).flushEntries(criteriaCaptor.capture());
    
    FlushCriteria criteria = criteriaCaptor.getValue();
    assertEquals(FlushScope.INSTANCE, criteria.getScope());
    assertEquals("test-instance", criteria.getInstanceName());
    assertNull(criteria.getDigestPrefix());
  }

  @Test
  public void testDigestPrefixSpecificCASFlush() {
    // Set up mock adapters
    CASAdapter mockFilesystemAdapter = mock(FilesystemCASAdapter.class);
    when(mockFilesystemAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Filesystem flush successful", 15, 1024 * 1024 * 5));
    casAdapters.put("filesystem", mockFilesystemAdapter);
    
    // Create a request for a specific digest prefix
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.DIGEST_PREFIX);
    request.setDigestPrefix("abc123");
    request.setFlushFilesystem(true);
    
    // Call the API
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    CASFlushResponse flushResponse = (CASFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    assertEquals(15, flushResponse.getEntriesRemoved());
    assertEquals(1024 * 1024 * 5, flushResponse.getBytesReclaimed());
    
    // Verify that the adapter was called with the correct criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockFilesystemAdapter).flushEntries(criteriaCaptor.capture());
    
    FlushCriteria criteria = criteriaCaptor.getValue();
    assertEquals(FlushScope.DIGEST_PREFIX, criteria.getScope());
    assertNull(criteria.getInstanceName());
    assertEquals("abc123", criteria.getDigestPrefix());
  }

  @Test
  public void testPartialFailureActionCacheFlush() {
    // Set up mock adapters
    ActionCacheAdapter mockRedisAdapter = mock(RedisActionCacheAdapter.class);
    when(mockRedisAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Redis flush successful", 50, 0));
    actionCacheAdapters.put("redis", mockRedisAdapter);
    
    ActionCacheAdapter mockInMemoryAdapter = mock(InMemoryActionCacheAdapter.class);
    when(mockInMemoryAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(false, "In-memory flush failed", 0, 0));
    actionCacheAdapters.put("in-memory", mockInMemoryAdapter);
    
    // Create a request
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // Call the API
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
    assertFalse(flushResponse.isSuccess());
    assertTrue(flushResponse.getMessage().contains("In-memory flush failed"));
    assertEquals(50, flushResponse.getEntriesRemoved());
    assertEquals(2, flushResponse.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(50), flushResponse.getEntriesRemovedByBackend().get("redis"));
    assertEquals(Integer.valueOf(0), flushResponse.getEntriesRemovedByBackend().get("in-memory"));
  }

  @Test
  public void testPartialFailureCASFlush() {
    // Set up mock adapters
    CASAdapter mockFilesystemAdapter = mock(FilesystemCASAdapter.class);
    when(mockFilesystemAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Filesystem flush successful", 100, 1024 * 1024 * 50));
    casAdapters.put("filesystem", mockFilesystemAdapter);
    
    CASAdapter mockInMemoryLRUAdapter = mock(InMemoryLRUCASAdapter.class);
    when(mockInMemoryLRUAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(false, "In-memory LRU flush failed", 0, 0));
    casAdapters.put("in-memory-lru", mockInMemoryLRUAdapter);
    
    // Create a request
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    
    // Call the API
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    CASFlushResponse flushResponse = (CASFlushResponse) response.getEntity();
    assertFalse(flushResponse.isSuccess());
    assertTrue(flushResponse.getMessage().contains("In-memory LRU flush failed"));
    assertEquals(100, flushResponse.getEntriesRemoved());
    assertEquals(1024 * 1024 * 50, flushResponse.getBytesReclaimed());
    assertEquals(2, flushResponse.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(100), flushResponse.getEntriesRemovedByBackend().get("filesystem"));
    assertEquals(Integer.valueOf(0), flushResponse.getEntriesRemovedByBackend().get("in-memory-lru"));
  }

  @Test
  public void testRateLimitingIntegration() {
    // Set up mock adapters
    ActionCacheAdapter mockRedisAdapter = mock(RedisActionCacheAdapter.class);
    when(mockRedisAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Redis flush successful", 10, 0));
    actionCacheAdapters.put("redis", mockRedisAdapter);
    
    // Create a request
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    
    // Call the API multiple times to hit the rate limit
    for (int i = 0; i < 5; i++) {
      Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
    
    // The next call should be rate limited
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    assertEquals(429, response.getStatus()); // 429 Too Many Requests
    
    // Verify that the adapter was called exactly 5 times
    verify(mockRedisAdapter, times(5)).flushEntries(any(FlushCriteria.class));
  }

  @Test
  public void testConcurrencyControlIntegration() throws Exception {
    // Set up mock adapters with delayed execution to simulate long-running operations
    ActionCacheAdapter mockRedisAdapter = mock(RedisActionCacheAdapter.class);
    doAnswer(invocation -> {
      Thread.sleep(500); // Simulate a long-running operation
      return new FlushResult(true, "Redis flush successful", 10, 0);
    }).when(mockRedisAdapter).flushEntries(any(FlushCriteria.class));
    actionCacheAdapters.put("redis", mockRedisAdapter);
    
    // Create a request
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    
    // Create multiple threads to call the API concurrently
    int numThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completeLatch = new CountDownLatch(numThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    
    for (int i = 0; i < numThreads; i++) {
      executor.submit(() -> {
        try {
          startLatch.await(); // Wait for all threads to be ready
          
          Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
          if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            successCount.incrementAndGet();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          completeLatch.countDown();
        }
      });
    }
    
    // Start all threads simultaneously
    startLatch.countDown();
    
    // Wait for all threads to complete
    completeLatch.await(2000, TimeUnit.MILLISECONDS);
    executor.shutdown();
    
    // Only 3 threads should have succeeded (concurrency limit is 3)
    assertEquals(3, successCount.get());
    
    // Verify that the adapter was called exactly 3 times
    verify(mockRedisAdapter, times(3)).flushEntries(any(FlushCriteria.class));
  }

  @Test
  public void testInvalidRequestHandling() {
    // Test missing scope
    ActionCacheFlushRequest request1 = new ActionCacheFlushRequest();
    request1.setFlushRedis(true);
    
    try {
      cacheFlushResource.flushActionCache(request1, mockSecurityContext);
      fail("Expected IllegalArgumentException for missing scope");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Scope must be specified"));
    }
    
    // Test missing instance name
    ActionCacheFlushRequest request2 = new ActionCacheFlushRequest();
    request2.setScope(FlushScope.INSTANCE);
    request2.setFlushRedis(true);
    
    try {
      cacheFlushResource.flushActionCache(request2, mockSecurityContext);
      fail("Expected IllegalArgumentException for missing instance name");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Instance name must be specified"));
    }
    
    // Test missing digest prefix
    CASFlushRequest request3 = new CASFlushRequest();
    request3.setScope(FlushScope.DIGEST_PREFIX);
    request3.setFlushFilesystem(true);
    
    try {
      cacheFlushResource.flushCAS(request3, mockSecurityContext);
      fail("Expected IllegalArgumentException for missing digest prefix");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Digest prefix must be specified"));
    }
    
    // Test no backends selected
    ActionCacheFlushRequest request4 = new ActionCacheFlushRequest();
    request4.setScope(FlushScope.ALL);
    
    try {
      cacheFlushResource.flushActionCache(request4, mockSecurityContext);
      fail("Expected IllegalArgumentException for no backends selected");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("At least one backend must be selected"));
    }
  }

  @Test
  public void testAdapterExceptionHandling() {
    // Set up mock adapters that throw exceptions
    ActionCacheAdapter mockRedisAdapter = mock(RedisActionCacheAdapter.class);
    when(mockRedisAdapter.flushEntries(any(FlushCriteria.class)))
        .thenThrow(new RuntimeException("Redis connection error"));
    actionCacheAdapters.put("redis", mockRedisAdapter);
    
    // Create a request
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    
    // Call the API
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
    assertFalse(flushResponse.isSuccess());
    assertTrue(flushResponse.getMessage().contains("Error flushing Action Cache"));
    assertEquals(0, flushResponse.getEntriesRemoved());
  }

  private void fail(String message) {
    throw new AssertionError(message);
  }
}