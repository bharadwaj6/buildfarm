package build.buildfarm.admin.cache.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for concurrency controls in {@link CacheFlushServiceImpl}. */
@RunWith(JUnit4.class)
public class CacheFlushServiceConcurrencyTest {

  private CacheFlushServiceImpl cacheFlushService;
  private ConcurrencyControlService concurrencyControlService;
  private Map<String, ActionCacheAdapter> actionCacheAdapters;
  private Map<String, CASAdapter> casAdapters;

  @Before
  public void setUp() {
    // Set up mock adapters
    actionCacheAdapters = new HashMap<>();
    ActionCacheAdapter mockRedisAdapter = mock(ActionCacheAdapter.class);
    when(mockRedisAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Success", 5, 0));
    actionCacheAdapters.put("redis", mockRedisAdapter);

    casAdapters = new HashMap<>();
    CASAdapter mockFilesystemAdapter = mock(CASAdapter.class);
    when(mockFilesystemAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Success", 5, 1024));
    casAdapters.put("filesystem", mockFilesystemAdapter);

    // Set up concurrency control service with limited permits
    ConcurrencyConfig config = new ConcurrencyConfig(2, 1, 1000, true);
    concurrencyControlService = new ConcurrencyControlService(config);

    // Create the service
    cacheFlushService =
        new CacheFlushServiceImpl(actionCacheAdapters, casAdapters, concurrencyControlService);
  }

  @Test
  public void testFlushActionCache_concurrencyLimitReached() throws Exception {
    // Create requests
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);

    // Acquire all permits
    assertTrue(concurrencyControlService.acquireActionCacheFlushPermit());
    assertTrue(concurrencyControlService.acquireActionCacheFlushPermit());

    // Call the service
    ActionCacheFlushResponse response = cacheFlushService.flushActionCache(request);

    // Verify the response
    assertFalse(response.isSuccess());
    assertTrue(response.getMessage().contains("Concurrency limit reached"));
    assertEquals(0, response.getEntriesRemoved());

    // Release permits
    concurrencyControlService.releaseActionCacheFlushPermit();
    concurrencyControlService.releaseActionCacheFlushPermit();
  }

  @Test
  public void testFlushCAS_concurrencyLimitReached() throws Exception {
    // Create requests
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);

    // Acquire all permits
    assertTrue(concurrencyControlService.acquireCASFlushPermit());

    // Call the service
    CASFlushResponse response = cacheFlushService.flushCAS(request);

    // Verify the response
    assertFalse(response.isSuccess());
    assertTrue(response.getMessage().contains("Concurrency limit reached"));
    assertEquals(0, response.getEntriesRemoved());
    assertEquals(0, response.getBytesReclaimed());

    // Release permits
    concurrencyControlService.releaseCASFlushPermit();
  }

  @Test
  public void testConcurrentFlushActionCache() throws Exception {
    // Create multiple threads to flush Action Cache concurrently
    int numThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completeLatch = new CountDownLatch(numThreads);
    AtomicInteger successCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await(); // Wait for all threads to be ready

              ActionCacheFlushRequest request = new ActionCacheFlushRequest();
              request.setScope(FlushScope.ALL);
              request.setFlushRedis(true);

              ActionCacheFlushResponse response = cacheFlushService.flushActionCache(request);
              if (response.isSuccess()) {
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

    // Only 2 threads should have succeeded (concurrency limit is 2)
    assertEquals(2, successCount.get());
  }

  @Test
  public void testConcurrentFlushCAS() throws Exception {
    // Create multiple threads to flush CAS concurrently
    int numThreads = 3;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completeLatch = new CountDownLatch(numThreads);
    AtomicInteger successCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await(); // Wait for all threads to be ready

              CASFlushRequest request = new CASFlushRequest();
              request.setScope(FlushScope.ALL);
              request.setFlushFilesystem(true);

              CASFlushResponse response = cacheFlushService.flushCAS(request);
              if (response.isSuccess()) {
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

    // Only 1 thread should have succeeded (concurrency limit is 1)
    assertEquals(1, successCount.get());
  }
}
