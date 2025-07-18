package build.buildfarm.admin.cache.concurrency;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Service for controlling concurrency of flush operations.
 */
public class ConcurrencyControlService {
  private static final Logger logger = Logger.getLogger(ConcurrencyControlService.class.getName());
  
  private final Semaphore actionCacheSemaphore;
  private final Semaphore casSemaphore;
  private final AtomicInteger activeActionCacheFlushes = new AtomicInteger(0);
  private final AtomicInteger activeCASFlushes = new AtomicInteger(0);
  private final ConcurrencyConfig config;
  
  /**
   * Creates a new ConcurrencyControlService with the default configuration.
   */
  public ConcurrencyControlService() {
    this(ConcurrencyConfig.getDefault());
  }
  
  /**
   * Creates a new ConcurrencyControlService with the specified configuration.
   *
   * @param config the concurrency configuration
   */
  public ConcurrencyControlService(ConcurrencyConfig config) {
    this.config = config;
    this.actionCacheSemaphore = new Semaphore(config.getMaxConcurrentActionCacheFlushes(), true);
    this.casSemaphore = new Semaphore(config.getMaxConcurrentCASFlushes(), true);
  }
  
  /**
   * Acquires a permit to perform an Action Cache flush operation.
   *
   * @return true if a permit was acquired, false otherwise
   */
  public boolean acquireActionCacheFlushPermit() {
    if (!config.isEnabled()) {
      activeActionCacheFlushes.incrementAndGet();
      return true;
    }
    
    try {
      boolean acquired = actionCacheSemaphore.tryAcquire(
          config.getFlushOperationTimeoutMs(), TimeUnit.MILLISECONDS);
      if (acquired) {
        activeActionCacheFlushes.incrementAndGet();
      }
      return acquired;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.warning("Interrupted while waiting for Action Cache flush permit");
      return false;
    }
  }
  
  /**
   * Releases a permit for an Action Cache flush operation.
   */
  public void releaseActionCacheFlushPermit() {
    if (!config.isEnabled()) {
      activeActionCacheFlushes.decrementAndGet();
      return;
    }
    
    activeActionCacheFlushes.decrementAndGet();
    actionCacheSemaphore.release();
  }
  
  /**
   * Acquires a permit to perform a CAS flush operation.
   *
   * @return true if a permit was acquired, false otherwise
   */
  public boolean acquireCASFlushPermit() {
    if (!config.isEnabled()) {
      activeCASFlushes.incrementAndGet();
      return true;
    }
    
    try {
      boolean acquired = casSemaphore.tryAcquire(
          config.getFlushOperationTimeoutMs(), TimeUnit.MILLISECONDS);
      if (acquired) {
        activeCASFlushes.incrementAndGet();
      }
      return acquired;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.warning("Interrupted while waiting for CAS flush permit");
      return false;
    }
  }
  
  /**
   * Releases a permit for a CAS flush operation.
   */
  public void releaseCASFlushPermit() {
    if (!config.isEnabled()) {
      activeCASFlushes.decrementAndGet();
      return;
    }
    
    activeCASFlushes.decrementAndGet();
    casSemaphore.release();
  }
  
  /**
   * Gets the number of active Action Cache flush operations.
   *
   * @return the number of active Action Cache flush operations
   */
  public int getActiveActionCacheFlushes() {
    return activeActionCacheFlushes.get();
  }
  
  /**
   * Gets the number of active CAS flush operations.
   *
   * @return the number of active CAS flush operations
   */
  public int getActiveCASFlushes() {
    return activeCASFlushes.get();
  }
  
  /**
   * Gets the number of available Action Cache flush permits.
   *
   * @return the number of available Action Cache flush permits
   */
  public int getAvailableActionCacheFlushPermits() {
    return actionCacheSemaphore.availablePermits();
  }
  
  /**
   * Gets the number of available CAS flush permits.
   *
   * @return the number of available CAS flush permits
   */
  public int getAvailableCASFlushPermits() {
    return casSemaphore.availablePermits();
  }
  
  /**
   * Gets the concurrency configuration.
   *
   * @return the configuration
   */
  public ConcurrencyConfig getConfig() {
    return config;
  }
}