package build.buildfarm.admin.cache.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.api.CacheFlushResource;
import build.buildfarm.admin.cache.adapter.ac.ActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.cas.CASAdapter;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Tests for interactions between components during cache flush operations.
 * These tests focus on how the different components work together, especially during concurrent operations.
 */
@RunWith(JUnit4.class)
public class CacheFlushComponentInteractionTest {

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
    ConcurrencyConfig concurrencyConfig = new ConcurrencyConfig(2, 1, 1000, true);
    concurrencyControlService = new ConcurrencyControlService(concurrencyConfig);
    
    // Create real rate limit service with a test configuration
    RateLimitConfig rateLimitConfig = new RateLimitConfig(3, 60000, true);
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
  public void testConcurrentActionCacheAndCASFlush() throws Exception {
    // Set up mock adapters with delayed execution to simulate long-running operations
    ActionCacheAdapter mockActionCacheAdapter = mock(ActionCacheAdapter.class);
    doAnswer(invocation -> {
      Thread.sleep(500); // Simulate a long-running operation
      return new FlushResult(true, "Action Cache flush successful", 10, 0);
    }).when(mockActionCacheAdapter).flushEntries(any(FlushCriteria.class));
    actionCacheAdapters.put("in-memory", mockActionCacheAdapter);
    
    CASAdapter mockCASAdapter = mock(CASAdapter.class);
    doAnswer(invocation -> {
      Thread.sleep(500); // Simulate a long-running operation
      return new FlushResult(true, "CAS flush successful", 20, 1024 * 1024);
    }).when(mockCASAdapter).flushEntries(any(FlushCriteria.class));
    casAdapters.put("filesystem", mockCASAdapter);
    
    // Create requests
    ActionCacheFlushRequest acRequest = new ActionCacheFlushRequest();
    acRequest.setScope(FlushScope.ALL);
    acRequest.setFlushInMemory(true);
    
    CASFlushRequest casRequest = new CASFlushRequest();
    casRequest.setScope(FlushScope.ALL);
    casRequest.setFlushFilesystem(true);
    
    // Create threads to call the API concurrently
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completeLatch = new CountDownLatch(2);
    List<Response> responses = new ArrayList<>();
    
    // Thread for Action Cache flush
    executor.submit(() -> {
      try {
        startLatch.await(); // Wait for both threads to be ready
        Response response = cacheFlushResource.flushActionCache(acRequest, mockSecurityContext);
        synchronized (responses) {
          responses.add(response);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        completeLatch.countDown();
      }
    });
    
    // Thread for CAS flush
    executor.submit(() -> {
      try {
        startLatch.await(); // Wait for both threads to be ready
        Response response = cacheFlushResource.flushCAS(casRequest, mockSecurityContext);
        synchronized (responses) {
          responses.add(response);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        completeLatch.countDown();
      }
    });
    
    // Start both threads simultaneously
    startLatch.countDown();
    
    // Wait for both threads to complete
    completeLatch.await(2000, TimeUnit.MILLISECONDS);
    executor.shutdown();
    
    // Both operations should have succeeded because they use different semaphores
    assertEquals(2, responses.size());
    
    int okCount = 0;
    for (Response response : responses) {
      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        okCount++;
      }
    }
    
    assertEquals(2, okCount);
    
    // Verify that both adapters were called
    verify(mockActionCacheAdapter).flushEntries(any(FlushCriteria.class));
    verify(mockCASAdapter).flushEntries(any(FlushCriteria.class));
  }

  @Test
  public void testConcurrentActionCacheFlushWithRateLimiting() throws Exception {
    // Set up mock adapter
    ActionCacheAdapter mockActionCacheAdapter = mock(ActionCacheAdapter.class);
    when(mockActionCacheAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Action Cache flush successful", 10, 0));
    actionCacheAdapters.put("in-memory", mockActionCacheAdapter);
    
    // Create request
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushInMemory(true);
    
    // Create multiple threads to call the API concurrently
    int numThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completeLatch = new CountDownLatch(numThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger rateLimitCount = new AtomicInteger(0);
    
    for (int i = 0; i < numThreads; i++) {
      executor.submit(() -> {
        try {
          startLatch.await(); // Wait for all threads to be ready
          
          Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
          if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            successCount.incrementAndGet();
          } else if (response.getStatus() == 429) { // 429 Too Many Requests
            rateLimitCount.incrementAndGet();
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
    
    // Only 3 threads should have succeeded (rate limit is 3)
    // The other 2 should have been rate limited
    assertEquals(3, successCount.get());
    assertEquals(2, rateLimitCount.get());
    
    // Verify that the adapter was called exactly 3 times
    verify(mockActionCacheAdapter, times(3)).flushEntries(any(FlushCriteria.class));
  }

  @Test
  public void testConcurrentCASFlushWithConcurrencyControl() throws Exception {
    // Set up mock adapter with delayed execution to simulate a long-running operation
    CASAdapter mockCASAdapter = mock(CASAdapter.class);
    doAnswer(invocation -> {
      Thread.sleep(500); // Simulate a long-running operation
      return new FlushResult(true, "CAS flush successful", 20, 1024 * 1024);
    }).when(mockCASAdapter).flushEntries(any(FlushCriteria.class));
    casAdapters.put("filesystem", mockCASAdapter);
    
    // Create request
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    
    // Create multiple threads to call the API concurrently
    int numThreads = 3;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completeLatch = new CountDownLatch(numThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger concurrencyLimitCount = new AtomicInteger(0);
    
    for (int i = 0; i < numThreads; i++) {
      executor.submit(() -> {
        try {
          startLatch.await(); // Wait for all threads to be ready
          
          Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);
          if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            CASFlushResponse flushResponse = (CASFlushResponse) response.getEntity();
            if (flushResponse.isSuccess()) {
              successCount.incrementAndGet();
            } else if (flushResponse.getMessage().contains("Concurrency limit reached")) {
              concurrencyLimitCount.incrementAndGet();
            }
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
    
    // Only 1 thread should have succeeded (concurrency limit is 1)
    // The other 2 should have hit the concurrency limit
    assertEquals(1, successCount.get());
    assertEquals(2, concurrencyLimitCount.get());
    
    // Verify that the adapter was called exactly once
    verify(mockCASAdapter, times(1)).flushEntries(any(FlushCriteria.class));
  }

  @Test
  public void testRateLimitingAndConcurrencyControlInteraction() throws Exception {
    // Set up mock adapter with delayed execution to simulate a long-running operation
    ActionCacheAdapter mockActionCacheAdapter = mock(ActionCacheAdapter.class);
    doAnswer(invocation -> {
      Thread.sleep(1000); // Simulate a long-running operation
      return new FlushResult(true, "Action Cache flush successful", 10, 0);
    }).when(mockActionCacheAdapter).flushEntries(any(FlushCriteria.class));
    actionCacheAdapters.put("in-memory", mockActionCacheAdapter);
    
    // Create request
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushInMemory(true);
    
    // First, exhaust the concurrency limit
    ExecutorService executor1 = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch1 = new CountDownLatch(1);
    CountDownLatch completeLatch1 = new CountDownLatch(2);
    List<Response> responses1 = new ArrayList<>();
    
    for (int i = 0; i < 2; i++) {
      executor1.submit(() -> {
        try {
          startLatch1.await(); // Wait for both threads to be ready
          Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
          synchronized (responses1) {
            responses1.add(response);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          completeLatch1.countDown();
        }
      });
    }
    
    // Start both threads simultaneously
    startLatch1.countDown();
    
    // Wait a bit for the threads to start processing
    Thread.sleep(100);
    
    // Now, try to send more requests that should be rate limited
    ExecutorService executor2 = Executors.newFixedThreadPool(3);
    CountDownLatch startLatch2 = new CountDownLatch(1);
    CountDownLatch completeLatch2 = new CountDownLatch(3);
    List<Response> responses2 = new ArrayList<>();
    
    for (int i = 0; i < 3; i++) {
      executor2.submit(() -> {
        try {
          startLatch2.await(); // Wait for all threads to be ready
          Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
          synchronized (responses2) {
            responses2.add(response);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          completeLatch2.countDown();
        }
      });
    }
    
    // Start all threads simultaneously
    startLatch2.countDown();
    
    // Wait for all threads to complete
    completeLatch1.await(2000, TimeUnit.MILLISECONDS);
    completeLatch2.await(2000, TimeUnit.MILLISECONDS);
    executor1.shutdown();
    executor2.shutdown();
    
    // Count the number of successful operations and rate limited operations
    int successCount = 0;
    int concurrencyLimitCount = 0;
    int rateLimitCount = 0;
    
    for (Response response : responses1) {
      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
        if (flushResponse.isSuccess()) {
          successCount++;
        } else if (flushResponse.getMessage().contains("Concurrency limit reached")) {
          concurrencyLimitCount++;
        }
      } else if (response.getStatus() == 429) {
        rateLimitCount++;
      }
    }
    
    for (Response response : responses2) {
      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
        if (flushResponse.isSuccess()) {
          successCount++;
        } else if (flushResponse.getMessage().contains("Concurrency limit reached")) {
          concurrencyLimitCount++;
        }
      } else if (response.getStatus() == 429) {
        rateLimitCount++;
      }
    }
    
    // We expect:
    // - 2 successful operations (concurrency limit is 2)
    // - 1 operation that hit the concurrency limit
    // - 2 operations that were rate limited (rate limit is 3, and we've already used 2 + 1 = 3)
    assertEquals(2, successCount);
    assertTrue(concurrencyLimitCount > 0);
    assertTrue(rateLimitCount > 0);
    assertEquals(5, successCount + concurrencyLimitCount + rateLimitCount);
    
    // Verify that the adapter was called exactly twice
    verify(mockActionCacheAdapter, times(2)).flushEntries(any(FlushCriteria.class));
  }

  @Test
  public void testMultipleBackendInteraction() {
    // Set up mock adapters
    ActionCacheAdapter mockRedisAdapter = mock(ActionCacheAdapter.class);
    when(mockRedisAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Redis flush successful", 30, 0));
    actionCacheAdapters.put("redis", mockRedisAdapter);
    
    ActionCacheAdapter mockInMemoryAdapter = mock(ActionCacheAdapter.class);
    when(mockInMemoryAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "In-memory flush successful", 20, 0));
    actionCacheAdapters.put("in-memory", mockInMemoryAdapter);
    
    // Create a request to flush both backends
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // Call the API
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    assertEquals(50, flushResponse.getEntriesRemoved());
    assertEquals(2, flushResponse.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(30), flushResponse.getEntriesRemovedByBackend().get("redis"));
    assertEquals(Integer.valueOf(20), flushResponse.getEntriesRemovedByBackend().get("in-memory"));
    
    // Verify that both adapters were called with the same criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockRedisAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockInMemoryAdapter).flushEntries(criteriaCaptor.capture());
    
    List<FlushCriteria> capturedCriteria = criteriaCaptor.getAllValues();
    assertEquals(2, capturedCriteria.size());
    assertEquals(FlushScope.ALL, capturedCriteria.get(0).getScope());
    assertEquals(FlushScope.ALL, capturedCriteria.get(1).getScope());
  }

  @Test
  public void testPartialBackendFailure() {
    // Set up mock adapters
    ActionCacheAdapter mockRedisAdapter = mock(ActionCacheAdapter.class);
    when(mockRedisAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Redis flush successful", 30, 0));
    actionCacheAdapters.put("redis", mockRedisAdapter);
    
    ActionCacheAdapter mockInMemoryAdapter = mock(ActionCacheAdapter.class);
    when(mockInMemoryAdapter.flushEntries(any(FlushCriteria.class)))
        .thenThrow(new RuntimeException("In-memory flush failed"));
    actionCacheAdapters.put("in-memory", mockInMemoryAdapter);
    
    // Create a request to flush both backends
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
    assertTrue(flushResponse.getMessage().contains("Error flushing Action Cache"));
    assertEquals(30, flushResponse.getEntriesRemoved());
    assertEquals(1, flushResponse.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(30), flushResponse.getEntriesRemovedByBackend().get("redis"));
    
    // Verify that both adapters were called
    verify(mockRedisAdapter).flushEntries(any(FlushCriteria.class));
    verify(mockInMemoryAdapter).flushEntries(any(FlushCriteria.class));
  }
}