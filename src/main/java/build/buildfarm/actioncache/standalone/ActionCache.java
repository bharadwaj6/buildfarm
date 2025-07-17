package build.buildfarm.actioncache.standalone;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import com.google.protobuf.ByteString;
import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;

/**
 * Interface for a standalone Action Cache.
 */
public interface ActionCache {
  /**
   * Gets an action result from the cache.
   *
   * @param actionKey the action key
   * @return the action result, or null if not found
   */
  ActionResult get(Digest actionKey);
  
  /**
   * Puts an action result into the cache.
   *
   * @param actionKey the action key
   * @param actionResult the action result
   */
  void put(Digest actionKey, ActionResult actionResult);
  
  /**
   * Flushes entries from the cache based on specified criteria.
   *
   * @param criteria the criteria for the flush operation
   * @return the result of the flush operation
   */
  FlushResult flush(FlushCriteria criteria);
}