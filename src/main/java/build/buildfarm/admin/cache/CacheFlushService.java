package build.buildfarm.admin.cache;

import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;

/** Service for flushing caches. */
public interface CacheFlushService {
  /**
   * Flushes Action Cache entries based on specified criteria.
   *
   * @param request the flush request
   * @return the flush response
   */
  ActionCacheFlushResponse flushActionCache(ActionCacheFlushRequest request);

  /**
   * Flushes Content Addressable Storage entries based on specified criteria.
   *
   * @param request the flush request
   * @return the flush response
   */
  CASFlushResponse flushCAS(CASFlushRequest request);
}
