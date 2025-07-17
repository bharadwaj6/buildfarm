package build.buildfarm.actioncache.standalone;

import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;
import build.buildfarm.admin.cache.adapter.ac.ActionCacheAdapter;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;

/**
 * A standalone implementation of the Action Cache that supports multiple storage backends.
 */
public class StandaloneActionCache implements ActionCache {
  private final ConcurrentMap<Digest, ActionResult> inMemoryCache;
  private final List<ActionCacheAdapter> adapters;

  /**
   * Creates a new StandaloneActionCache instance.
   *
   * @param adapters the list of adapters for different storage backends
   */
  public StandaloneActionCache(List<ActionCacheAdapter> adapters) {
    this.inMemoryCache = new ConcurrentHashMap<>();
    this.adapters = adapters;
  }

  @Override
  @Nullable
  public ActionResult get(Digest actionKey) {
    return inMemoryCache.get(actionKey);
  }

  @Override
  public void put(Digest actionKey, ActionResult actionResult) {
    inMemoryCache.put(actionKey, actionResult);
  }

  @Override
  public FlushResult flush(FlushCriteria criteria) {
    FlushResult result = new FlushResult();
    
    // First, flush in-memory cache
    int entriesRemoved = 0;
    
    if (criteria.getScope() == FlushScope.ALL) {
      entriesRemoved = inMemoryCache.size();
      inMemoryCache.clear();
    } else if (criteria.getScope() == FlushScope.INSTANCE) {
      // In-memory cache doesn't track instances, so we can't flush by instance
      // This would be implemented in a real implementation
    } else if (criteria.getScope() == FlushScope.DIGEST_PREFIX) {
      String prefix = criteria.getDigestPrefix();
      inMemoryCache.keySet().removeIf(key -> key.getHash().startsWith(prefix));
      // Count removed entries
      // In a real implementation, we would track the number of entries removed
    }
    
    result.setEntriesRemoved(entriesRemoved);
    
    // Then, flush all adapters
    for (ActionCacheAdapter adapter : adapters) {
      FlushResult adapterResult = adapter.flushEntries(criteria);
      result.merge(adapterResult);
    }
    
    return result;
  }
}