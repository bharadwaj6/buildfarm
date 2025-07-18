package build.buildfarm.admin.cache.adapter.ac;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;

/** Interface for Action Cache adapters that can flush entries. */
public interface ActionCacheAdapter {
  /**
   * Flushes Action Cache entries based on the specified criteria.
   *
   * @param criteria the criteria for the flush operation
   * @return the result of the flush operation
   */
  FlushResult flushEntries(FlushCriteria criteria);
}
