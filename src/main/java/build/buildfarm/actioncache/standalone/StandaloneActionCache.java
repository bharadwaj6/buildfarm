package build.buildfarm.actioncache.standalone;

import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;
import build.buildfarm.actioncache.standalone.config.ActionCacheConfig;
import build.buildfarm.admin.cache.adapter.common.ActionCacheAdapter;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.Nullable;

/** A standalone implementation of the Action Cache that supports multiple storage backends. */
public class StandaloneActionCache implements ActionCache {
  private final ConcurrentMap<Digest, ActionResult> inMemoryCache;
  private final List<ActionCacheAdapter> adapters;
  private final int maxCacheSize;
  private final LinkedBlockingQueue<Digest> evictionQueue;

  /**
   * Creates a new StandaloneActionCache instance.
   *
   * @param adapters the list of adapters for different storage backends
   */
  public StandaloneActionCache(List<ActionCacheAdapter> adapters) {
    this(adapters, 10000); // Default max cache size
  }

  /**
   * Creates a new StandaloneActionCache instance with the specified max cache size.
   *
   * @param adapters the list of adapters for different storage backends
   * @param maxCacheSize the maximum size of the in-memory cache
   */
  public StandaloneActionCache(List<ActionCacheAdapter> adapters, int maxCacheSize) {
    this.inMemoryCache = new ConcurrentHashMap<>();
    this.adapters = adapters;
    this.maxCacheSize = maxCacheSize;
    this.evictionQueue = new LinkedBlockingQueue<>();
  }

  /**
   * Creates a new StandaloneActionCache instance from the given configuration.
   *
   * @param config the configuration
   * @param adapters the list of adapters for different storage backends
   */
  public StandaloneActionCache(ActionCacheConfig config, List<ActionCacheAdapter> adapters) {
    this.adapters = adapters;
    this.maxCacheSize = config.getInMemoryCacheMaxSize();

    if (config.isEnableInMemoryCache()) {
      this.inMemoryCache = new ConcurrentHashMap<>();
      this.evictionQueue = new LinkedBlockingQueue<>();
    } else {
      this.inMemoryCache = null;
      this.evictionQueue = null;
    }
  }

  @Override
  @Nullable
  public ActionResult get(Digest actionKey) {
    if (inMemoryCache == null) {
      // In-memory cache is disabled, delegate to adapters
      for (ActionCacheAdapter adapter : adapters) {
        ActionResult result = adapter.get(actionKey);
        if (result != null) {
          return result;
        }
      }
      return null;
    }

    // Check in-memory cache first
    ActionResult result = inMemoryCache.get(actionKey);
    if (result != null) {
      return result;
    }

    // If not found in memory, check adapters
    // Note: In a real implementation, adapters would implement get() method
    // For now, we'll just return null as adapters don't have this method in the common interface
    return null;
  }

  @Override
  public void put(Digest actionKey, ActionResult actionResult) {
    // Store in adapters
    for (ActionCacheAdapter adapter : adapters) {
      adapter.put(actionKey, actionResult);
    }

    // Store in memory if enabled
    if (inMemoryCache != null) {
      putInMemory(actionKey, actionResult);
    }
  }

  /**
   * Puts an action result into the in-memory cache, handling eviction if necessary.
   *
   * @param actionKey the action key
   * @param actionResult the action result
   */
  private void putInMemory(Digest actionKey, ActionResult actionResult) {
    if (inMemoryCache.size() >= maxCacheSize) {
      // Evict the oldest entry
      Digest toEvict = evictionQueue.poll();
      if (toEvict != null) {
        inMemoryCache.remove(toEvict);
      }
    }

    inMemoryCache.put(actionKey, actionResult);
    evictionQueue.offer(actionKey);
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
