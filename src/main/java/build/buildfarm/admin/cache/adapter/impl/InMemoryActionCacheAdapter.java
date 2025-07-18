package build.buildfarm.admin.cache.adapter.impl;

import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;
import build.buildfarm.admin.cache.adapter.common.ActionCacheAdapter;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import com.google.common.base.Preconditions;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Implementation of {@link ActionCacheAdapter} for in-memory Action Cache. */
public class InMemoryActionCacheAdapter implements ActionCacheAdapter {

  private final ConcurrentMap<ActionCacheKey, ActionResult> cache;

  /**
   * Creates a new InMemoryActionCacheAdapter instance.
   *
   * @param cache the in-memory cache
   */
  public InMemoryActionCacheAdapter(ConcurrentMap<ActionCacheKey, ActionResult> cache) {
    this.cache = Preconditions.checkNotNull(cache, "cache");
  }

  /** Creates a new InMemoryActionCacheAdapter instance with a new empty cache. */
  public InMemoryActionCacheAdapter() {
    this(new ConcurrentHashMap<>());
  }

  @Override
  public ActionResult get(Digest actionKey) {
    // Implementation would go here
    return null;
  }

  @Override
  public void put(Digest actionKey, ActionResult actionResult) {
    // Implementation would go here
  }

  @Override
  public FlushResult flushEntries(FlushCriteria criteria) {
    FlushResult result = new FlushResult();
    int entriesRemoved = 0;

    if (criteria.getScope() == FlushScope.ALL) {
      // Flush all entries
      entriesRemoved = cache.size();
      cache.clear();
    } else if (criteria.getScope() == FlushScope.INSTANCE) {
      // Flush entries for a specific instance
      String instanceName = criteria.getInstanceName();
      entriesRemoved = (int) cache.keySet().stream()
          .filter(key -> key.getInstanceName().equals(instanceName))
          .peek(key -> cache.remove(key))
          .count();
    } else if (criteria.getScope() == FlushScope.DIGEST_PREFIX) {
      // Flush entries with a specific digest prefix
      String digestPrefix = criteria.getDigestPrefix();
      entriesRemoved = (int) cache.keySet().stream()
          .filter(key -> key.getDigest().getHash().startsWith(digestPrefix))
          .peek(key -> cache.remove(key))
          .count();
    }

    result.setSuccess(true);
    result.setEntriesRemoved(entriesRemoved);
    result.setMessage("Successfully flushed in-memory Action Cache");
    return result;
  }

  /** Key for the in-memory action cache. */
  public static class ActionCacheKey {
    private final String instanceName;
    private final Digest digest;

    public ActionCacheKey(String instanceName, Digest digest) {
      this.instanceName = instanceName;
      this.digest = digest;
    }

    public String getInstanceName() {
      return instanceName;
    }

    public Digest getDigest() {
      return digest;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ActionCacheKey that = (ActionCacheKey) o;
      return instanceName.equals(that.instanceName) && digest.equals(that.digest);
    }

    @Override
    public int hashCode() {
      int result = instanceName.hashCode();
      result = 31 * result + digest.hashCode();
      return result;
    }
  }
}