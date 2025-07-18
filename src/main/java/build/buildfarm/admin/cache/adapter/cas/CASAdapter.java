package build.buildfarm.admin.cache.adapter.cas;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;

/** Interface for Content Addressable Storage (CAS) adapters that can flush entries. */
public interface CASAdapter {
  /**
   * Flushes CAS entries based on the specified criteria.
   *
   * @param criteria the criteria for the flush operation
   * @return the result of the flush operation
   */
  FlushResult flushEntries(FlushCriteria criteria);
}
