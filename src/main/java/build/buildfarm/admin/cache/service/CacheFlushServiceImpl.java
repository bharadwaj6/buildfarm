package build.buildfarm.admin.cache.service;

import build.buildfarm.admin.cache.adapter.ac.ActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.cas.CASAdapter;
import build.buildfarm.admin.cache.concurrency.ConcurrencyConfig;
import build.buildfarm.admin.cache.concurrency.ConcurrencyControlService;
import build.buildfarm.admin.cache.metrics.CacheFlushMetrics;
import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import build.buildfarm.admin.cache.model.ConcurrencyLimitExceededResponse;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the {@link CacheFlushService} interface.
 * This service coordinates flush operations across different storage backends.
 */
public class CacheFlushServiceImpl implements CacheFlushService {
  private static final Logger logger = Logger.getLogger(CacheFlushServiceImpl.class.getName());
  
  private final Map<String, ActionCacheAdapter> actionCacheAdapters;
  private final Map<String, CASAdapter> casAdapters;
  private final ReadWriteLock actionCacheLock = new ReentrantReadWriteLock();
  private final ReadWriteLock casLock = new ReentrantReadWriteLock();
  private final Map<String, Integer> activeFlushOperations = new ConcurrentHashMap<>();
  private final ConcurrencyControlService concurrencyControlService;
  
  /**
   * Creates a new CacheFlushServiceImpl instance.
   *
   * @param actionCacheAdapters map of Action Cache adapters by name
   * @param casAdapters map of CAS adapters by name
   */
  public CacheFlushServiceImpl(
      Map<String, ActionCacheAdapter> actionCacheAdapters,
      Map<String, CASAdapter> casAdapters) {
    this(actionCacheAdapters, casAdapters, new ConcurrencyControlService(ConcurrencyConfig.getDefault()));
  }
  
  /**
   * Creates a new CacheFlushServiceImpl instance with the specified concurrency control service.
   *
   * @param actionCacheAdapters map of Action Cache adapters by name
   * @param casAdapters map of CAS adapters by name
   * @param concurrencyControlService the concurrency control service
   */
  public CacheFlushServiceImpl(
      Map<String, ActionCacheAdapter> actionCacheAdapters,
      Map<String, CASAdapter> casAdapters,
      ConcurrencyControlService concurrencyControlService) {
    this.actionCacheAdapters = ImmutableMap.copyOf(
        Preconditions.checkNotNull(actionCacheAdapters, "actionCacheAdapters"));
    this.casAdapters = ImmutableMap.copyOf(
        Preconditions.checkNotNull(casAdapters, "casAdapters"));
    this.concurrencyControlService = Preconditions.checkNotNull(
        concurrencyControlService, "concurrencyControlService");
  }
  
  @Override
  public ActionCacheFlushResponse flushActionCache(ActionCacheFlushRequest request) {
    validateActionCacheFlushRequest(request);
    
    // Try to acquire a permit for the Action Cache flush operation
    if (!concurrencyControlService.acquireActionCacheFlushPermit()) {
      // Could not acquire permit, return error response
      logger.warning("Could not acquire Action Cache flush permit: concurrency limit reached");
      return new ActionCacheFlushResponse(
          false, 
          "Concurrency limit reached for Action Cache flush operations", 
          0);
    }
    
    ActionCacheFlushResponse response = new ActionCacheFlushResponse();
    FlushCriteria criteria = new FlushCriteria(
        request.getScope(), request.getInstanceName(), request.getDigestPrefix());
    
    // Acquire a read lock to allow concurrent flush operations but prevent modifications
    // to the adapters while flushing
    actionCacheLock.readLock().lock();
    try {
      // Track the active flush operation
      incrementActiveFlushOperations("action-cache");
      
      try {
        // Flush Redis Action Cache if requested
        if (request.isFlushRedis() && actionCacheAdapters.containsKey("redis")) {
          FlushResult result = flushActionCacheBackend("redis", criteria);
          response.addEntriesRemovedByBackend("redis", result.getEntriesRemoved());
          handleFlushResult(response, result);
        }
        
        // Flush in-memory Action Cache if requested
        if (request.isFlushInMemory() && actionCacheAdapters.containsKey("in-memory")) {
          FlushResult result = flushActionCacheBackend("in-memory", criteria);
          response.addEntriesRemovedByBackend("in-memory", result.getEntriesRemoved());
          handleFlushResult(response, result);
        }
        
        // If no backends were flushed, add a message to the response
        if (response.getEntriesRemovedByBackend().isEmpty()) {
          response.setMessage("No Action Cache backends were flushed");
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Error flushing Action Cache", e);
        response.setSuccess(false);
        response.setMessage("Error flushing Action Cache: " + e.getMessage());
      } finally {
        // Decrement the active flush operation count
        decrementActiveFlushOperations("action-cache");
        // Release the permit
        concurrencyControlService.releaseActionCacheFlushPermit();
      }
    } finally {
      actionCacheLock.readLock().unlock();
    }
    
    return response;
  }
  
  @Override
  public CASFlushResponse flushCAS(CASFlushRequest request) {
    validateCASFlushRequest(request);
    
    // Try to acquire a permit for the CAS flush operation
    if (!concurrencyControlService.acquireCASFlushPermit()) {
      // Could not acquire permit, return error response
      logger.warning("Could not acquire CAS flush permit: concurrency limit reached");
      return new CASFlushResponse(
          false, 
          "Concurrency limit reached for CAS flush operations", 
          0, 
          0);
    }
    
    CASFlushResponse response = new CASFlushResponse();
    FlushCriteria criteria = new FlushCriteria(
        request.getScope(), request.getInstanceName(), request.getDigestPrefix());
    
    // Acquire a read lock to allow concurrent flush operations but prevent modifications
    // to the adapters while flushing
    casLock.readLock().lock();
    try {
      // Track the active flush operation
      incrementActiveFlushOperations("cas");
      
      try {
        // Flush filesystem CAS if requested
        if (request.isFlushFilesystem() && casAdapters.containsKey("filesystem")) {
          FlushResult result = flushCASBackend("filesystem", criteria);
          response.addBackendResult(
              "filesystem", result.getEntriesRemoved(), result.getBytesReclaimed());
          handleFlushResult(response, result);
        }
        
        // Flush in-memory LRU CAS if requested
        if (request.isFlushInMemoryLRU() && casAdapters.containsKey("in-memory-lru")) {
          FlushResult result = flushCASBackend("in-memory-lru", criteria);
          response.addBackendResult(
              "in-memory-lru", result.getEntriesRemoved(), result.getBytesReclaimed());
          handleFlushResult(response, result);
        }
        
        // Flush Redis CAS worker map if requested
        if (request.isFlushRedisWorkerMap() && casAdapters.containsKey("redis-worker-map")) {
          FlushResult result = flushCASBackend("redis-worker-map", criteria);
          response.addBackendResult(
              "redis-worker-map", result.getEntriesRemoved(), result.getBytesReclaimed());
          handleFlushResult(response, result);
        }
        
        // If no backends were flushed, add a message to the response
        if (response.getEntriesRemovedByBackend().isEmpty()) {
          response.setMessage("No CAS backends were flushed");
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Error flushing CAS", e);
        response.setSuccess(false);
        response.setMessage("Error flushing CAS: " + e.getMessage());
      } finally {
        // Decrement the active flush operation count
        decrementActiveFlushOperations("cas");
        // Release the permit
        concurrencyControlService.releaseCASFlushPermit();
      }
    } finally {
      casLock.readLock().unlock();
    }
    
    return response;
  }
  
  /**
   * Validates an Action Cache flush request.
   *
   * @param request the request to validate
   * @throws IllegalArgumentException if the request is invalid
   */
  private void validateActionCacheFlushRequest(ActionCacheFlushRequest request) {
    Preconditions.checkNotNull(request, "request");
    Preconditions.checkNotNull(request.getScope(), "scope");
    
    if (request.getScope() == null) {
      throw new IllegalArgumentException("Scope must be specified");
    }
    
    if (request.getScope() == build.buildfarm.admin.cache.model.FlushScope.INSTANCE
        && (request.getInstanceName() == null || request.getInstanceName().isEmpty())) {
      throw new IllegalArgumentException(
          "Instance name must be specified when scope is INSTANCE");
    }
    
    if (request.getScope() == build.buildfarm.admin.cache.model.FlushScope.DIGEST_PREFIX
        && (request.getDigestPrefix() == null || request.getDigestPrefix().isEmpty())) {
      throw new IllegalArgumentException(
          "Digest prefix must be specified when scope is DIGEST_PREFIX");
    }
    
    if (!request.isFlushRedis() && !request.isFlushInMemory()) {
      throw new IllegalArgumentException(
          "At least one backend must be selected for flushing");
    }
  }
  
  /**
   * Validates a CAS flush request.
   *
   * @param request the request to validate
   * @throws IllegalArgumentException if the request is invalid
   */
  private void validateCASFlushRequest(CASFlushRequest request) {
    Preconditions.checkNotNull(request, "request");
    Preconditions.checkNotNull(request.getScope(), "scope");
    
    if (request.getScope() == null) {
      throw new IllegalArgumentException("Scope must be specified");
    }
    
    if (request.getScope() == build.buildfarm.admin.cache.model.FlushScope.INSTANCE
        && (request.getInstanceName() == null || request.getInstanceName().isEmpty())) {
      throw new IllegalArgumentException(
          "Instance name must be specified when scope is INSTANCE");
    }
    
    if (request.getScope() == build.buildfarm.admin.cache.model.FlushScope.DIGEST_PREFIX
        && (request.getDigestPrefix() == null || request.getDigestPrefix().isEmpty())) {
      throw new IllegalArgumentException(
          "Digest prefix must be specified when scope is DIGEST_PREFIX");
    }
    
    if (!request.isFlushFilesystem() 
        && !request.isFlushInMemoryLRU() 
        && !request.isFlushRedisWorkerMap()) {
      throw new IllegalArgumentException(
          "At least one backend must be selected for flushing");
    }
  }
  
  /**
   * Flushes an Action Cache backend.
   *
   * @param backendName the name of the backend to flush
   * @param criteria the criteria for the flush operation
   * @return the result of the flush operation
   */
  private FlushResult flushActionCacheBackend(String backendName, FlushCriteria criteria) {
    ActionCacheAdapter adapter = actionCacheAdapters.get(backendName);
    if (adapter == null) {
      return new FlushResult(false, "Backend not found: " + backendName, 0, 0);
    }
    
    try {
      logger.info("Flushing Action Cache backend: " + backendName);
      FlushResult result = adapter.flushEntries(criteria);
      logger.info(String.format(
          "Flushed %d entries from Action Cache backend: %s", 
          result.getEntriesRemoved(), backendName));
      
      // Record metrics for the flush operation
      CacheFlushMetrics.recordFlushOperation(
          "action-cache", backendName, criteria.getScope().name(), result.isSuccess());
      CacheFlushMetrics.recordEntriesRemoved(
          "action-cache", backendName, result.getEntriesRemoved());
      
      return result;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error flushing Action Cache backend: " + backendName, e);
      // Record failed operation in metrics
      CacheFlushMetrics.recordFlushOperation(
          "action-cache", backendName, criteria.getScope().name(), false);
      return new FlushResult(
          false, "Error flushing Action Cache backend: " + backendName + ": " + e.getMessage(), 0, 0);
    }
  }
  
  /**
   * Flushes a CAS backend.
   *
   * @param backendName the name of the backend to flush
   * @param criteria the criteria for the flush operation
   * @return the result of the flush operation
   */
  private FlushResult flushCASBackend(String backendName, FlushCriteria criteria) {
    CASAdapter adapter = casAdapters.get(backendName);
    if (adapter == null) {
      return new FlushResult(false, "Backend not found: " + backendName, 0, 0);
    }
    
    try {
      logger.info("Flushing CAS backend: " + backendName);
      FlushResult result = adapter.flushEntries(criteria);
      logger.info(String.format(
          "Flushed %d entries (%d bytes) from CAS backend: %s", 
          result.getEntriesRemoved(), result.getBytesReclaimed(), backendName));
      
      // Record metrics for the flush operation
      CacheFlushMetrics.recordFlushOperation(
          "cas", backendName, criteria.getScope().name(), result.isSuccess());
      CacheFlushMetrics.recordEntriesRemoved(
          "cas", backendName, result.getEntriesRemoved());
      CacheFlushMetrics.recordBytesReclaimed(
          "cas", backendName, result.getBytesReclaimed());
      
      return result;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error flushing CAS backend: " + backendName, e);
      // Record failed operation in metrics
      CacheFlushMetrics.recordFlushOperation(
          "cas", backendName, criteria.getScope().name(), false);
      return new FlushResult(
          false, "Error flushing CAS backend: " + backendName + ": " + e.getMessage(), 0, 0);
    }
  }
  
  /**
   * Updates the response based on the result of a flush operation.
   *
   * @param response the response to update
   * @param result the result of the flush operation
   */
  private void handleFlushResult(ActionCacheFlushResponse response, FlushResult result) {
    if (!result.isSuccess()) {
      response.setSuccess(false);
      response.setMessage(response.getMessage() + (response.getMessage().isEmpty() ? "" : ", ") 
          + result.getMessage());
    }
  }
  
  /**
   * Updates the response based on the result of a flush operation.
   *
   * @param response the response to update
   * @param result the result of the flush operation
   */
  private void handleFlushResult(CASFlushResponse response, FlushResult result) {
    if (!result.isSuccess()) {
      response.setSuccess(false);
      response.setMessage(response.getMessage() + (response.getMessage().isEmpty() ? "" : ", ") 
          + result.getMessage());
    }
  }
  
  /**
   * Increments the count of active flush operations for a cache type.
   *
   * @param cacheType the cache type (action-cache or cas)
   */
  private void incrementActiveFlushOperations(String cacheType) {
    activeFlushOperations.compute(cacheType, (key, count) -> count == null ? 1 : count + 1);
    logger.info("Active flush operations for " + cacheType + ": " 
        + activeFlushOperations.getOrDefault(cacheType, 0));
  }
  
  /**
   * Decrements the count of active flush operations for a cache type.
   *
   * @param cacheType the cache type (action-cache or cas)
   */
  private void decrementActiveFlushOperations(String cacheType) {
    activeFlushOperations.compute(cacheType, (key, count) -> count == null ? 0 : count - 1);
    logger.info("Active flush operations for " + cacheType + ": " 
        + activeFlushOperations.getOrDefault(cacheType, 0));
  }
  
  /**
   * Gets the number of active flush operations for a cache type.
   *
   * @param cacheType the cache type (action-cache or cas)
   * @return the number of active flush operations
   */
  public int getActiveFlushOperations(String cacheType) {
    return activeFlushOperations.getOrDefault(cacheType, 0);
  }
}