package build.buildfarm.admin.cache.service;

import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;

/**
 * Service for flushing Action Cache and Content Addressable Storage entries.
 * This service coordinates flush operations across different storage backends.
 */
public interface CacheFlushService {
  /**
   * Flushes Action Cache entries based on the specified request.
   * 
   * <p>This method coordinates the flush operation across different Action Cache backends
   * (Redis, in-memory) based on the parameters in the request.
   *
   * @param request the request containing flush parameters
   * @return the response with details about the flush operation
   * @throws IllegalArgumentException if the request contains invalid parameters
   */
  ActionCacheFlushResponse flushActionCache(ActionCacheFlushRequest request);
  
  /**
   * Flushes Content Addressable Storage entries based on the specified request.
   * 
   * <p>This method coordinates the flush operation across different CAS backends
   * (filesystem, in-memory LRU, Redis worker map) based on the parameters in the request.
   *
   * @param request the request containing flush parameters
   * @return the response with details about the flush operation
   * @throws IllegalArgumentException if the request contains invalid parameters
   */
  CASFlushResponse flushCAS(CASFlushRequest request);
}