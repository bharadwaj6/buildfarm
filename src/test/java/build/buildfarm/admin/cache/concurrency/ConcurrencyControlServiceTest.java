package build.buildfarm.admin.cache.concurrency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ConcurrencyControlService}. */
@RunWith(JUnit4.class)
public class ConcurrencyControlServiceTest {

  private ConcurrencyControlService concurrencyControlService;

  @Before
  public void setUp() {
    // Create a concurrency control service with a custom configuration
    ConcurrencyConfig config = new ConcurrencyConfig(3, 2, 1000, true);
    concurrencyControlService = new ConcurrencyControlService(config);
  }

  @Test
  public void testAcquireActionCacheFlushPermit_underLimit() {
    // First permit should be acquired
    assertTrue(concurrencyControlService.acquireActionCacheFlushPermit());
    assertEquals(1, concurrencyControlService.getActiveActionCacheFlushes());
    assertEquals(2, concurrencyControlService.getAvailableActionCacheFlushPermits());

    // Second permit should be acquired
    assertTrue(concurrencyControlService.acquireActionCacheFlushPermit());
    assertEquals(2, concurrencyControlService.getActiveActionCacheFlushes());
    assertEquals(1, concurrencyControlService.getAvailableActionCacheFlushPermits());

    // Third permit should be acquired
    assertTrue(concurrencyControlService.acquireActionCacheFlushPermit());
    assertEquals(3, concurrencyControlService.getActiveActionCacheFlushes());
    assertEquals(0, concurrencyControlService.getAvailableActionCacheFlushPermits());

    // Release permits
    concurrencyControlService.releaseActionCacheFlushPermit();
    concurrencyControlService.releaseActionCacheFlushPermit();
    concurrencyControlService.releaseActionCacheFlushPermit();

    assertEquals(0, concurrencyControlService.getActiveActionCacheFlushes());
    assertEquals(3, concurrencyControlService.getAvailableActionCacheFlushPermits());
  }

  @Test
  public void testAcquireCASFlushPermit_underLimit() {
    // First permit should be acquired
    assertTrue(concurrencyControlService.acquireCASFlushPermit());
    assertEquals(1, concurrencyControlService.getActiveCASFlushes());
    assertEquals(1, concurrencyControlService.getAvailableCASFlushPermits());

    // Second permit should be acquired
    assertTrue(concurrencyControlService.acquireCASFlushPermit());
    assertEquals(2, concurrencyControlService.getActiveCASFlushes());
    assertEquals(0, concurrencyControlService.getAvailableCASFlushPermits());

    // Release permits
    concurrencyControlService.releaseCASFlushPermit();
    concurrencyControlService.releaseCASFlushPermit();

    assertEquals(0, concurrencyControlService.getActiveCASFlushes());
    assertEquals(2, concurrencyControlService.getAvailableCASFlushPermits());
  }

  @Test
  public void testAcquireActionCacheFlushPermit_atLimit() throws Exception {
    // Acquire all permits
    assertTrue(concurrencyControlService.acquireActionCacheFlushPermit());
    assertTrue(concurrencyControlService.acquireActionCacheFlushPermit());
    assertTrue(concurrencyControlService.acquireActionCacheFlushPermit());

    // Try to acquire another permit - should fail
    boolean acquired = concurrencyControlService.acquireActionCacheFlushPermit();
    
    // Should not be able to acquire another permit
    assertEquals(false, acquired);
  }

  @Test
  public void testAcquireCASFlushPermit_atLimit() throws Exception {
    // Acquire all permits
    assertTrue(concurrencyControlService.acquireCASFlushPermit());
    assertTrue(concurrencyControlService.acquireCASFlushPermit());

    // Try to acquire another permit - should fail
    boolean acquired = concurrencyControlService.acquireCASFlushPermit();
    
    // Should not be able to acquire another permit
    assertEquals(false, acquired);
  }

  @Test
  public void testConcurrentAcquireActionCacheFlushPermit() throws Exception {
    // Create multiple threads to acquire permits concurrently
    int numThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completeLatch = new CountDownLatch(numThreads);
    List<Boolean> results = new ArrayList<>();

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await(); // Wait for all threads to be ready
              boolean acquired = concurrencyControlService.acquireActionCacheFlushPermit();
              synchronized (results) {
                results.add(acquired);
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
    completeLatch.await(1000, TimeUnit.MILLISECONDS);
    executor.shutdown();

    // Count the number of successful acquisitions
    int successCount = 0;
    for (boolean result : results) {
      if (result) {
        successCount++;
      }
    }

    // Only 3 threads should have acquired permits
    assertEquals(3, successCount);
    assertEquals(3, concurrencyControlService.getActiveActionCacheFlushes());
    assertEquals(0, concurrencyControlService.getAvailableActionCacheFlushPermits());

    // Release all permits
    for (int i = 0; i < successCount; i++) {
      concurrencyControlService.releaseActionCacheFlushPermit();
    }

    assertEquals(0, concurrencyControlService.getActiveActionCacheFlushes());
    assertEquals(3, concurrencyControlService.getAvailableActionCacheFlushPermits());
  }

  @Test
  public void testDisabledConcurrencyControls() {
    // Create a concurrency control service with concurrency controls disabled
    ConcurrencyConfig config = ConcurrencyConfig.disabled();
    ConcurrencyControlService disabledService = new ConcurrencyControlService(config);

    // All permit acquisitions should succeed
    for (int i = 0; i < 10; i++) {
      assertTrue(disabledService.acquireActionCacheFlushPermit());
      assertTrue(disabledService.acquireCASFlushPermit());
    }

    // Release permits
    for (int i = 0; i < 10; i++) {
      disabledService.releaseActionCacheFlushPermit();
      disabledService.releaseCASFlushPermit();
    }

    assertEquals(0, disabledService.getActiveActionCacheFlushes());
    assertEquals(0, disabledService.getActiveCASFlushes());
  }
}
