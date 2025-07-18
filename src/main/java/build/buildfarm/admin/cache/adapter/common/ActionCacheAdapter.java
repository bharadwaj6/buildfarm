package build.buildfarm.admin.cache.adapter.common;

import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;

/**
 * Interface for Action Cache adapters that handle flushing operations.
 */
public interface ActionCacheAdapter {
  /**
   * Flush Action Cache entries based on specified criteria.
   *
   * @param criteria The criteria for flushing entries
   * @return Result of the flush operation
   */
  FlushResult flushEntries(FlushCriteria criteria);
  
  /**
   * Gets an action result from the cache.
   *
   * @param actionKey the action key
   * @return the action result, or null if not found
   */
  default ActionResult get(Digest actionKey) {
    // Default implementation returns null
    return null;
  }

  /**
   * Puts an action result into the cache.
   *
   * @param actionKey the action key
   * @param actionResult the action result
   */
  default void put(Digest actionKey, ActionResult actionResult) {
    // Default implementation does nothing
  }
}