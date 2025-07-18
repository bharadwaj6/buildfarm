package build.buildfarm.admin.cache.adapter.ac;

import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import javax.inject.Inject;

/** Implementation of {@link ActionCacheAdapter} for in-memory Action Cache. */
public class InMemoryActionCacheAdapter implements ActionCacheAdapter {

  private final ConcurrentMap<ActionCacheKey, ActionResult> cache;

  /**
   * Creates a new InMemoryActionCacheAdapter instance.
   *
   * @param cache the in-memory cache
   */
  @Inject
  public InMemoryActionCacheAdapter(ConcurrentMap<ActionCacheKey, ActionResult> cache) {
    this.cache = Preconditions.checkNotNull(cache, "cache");
  }

  /** Creates a new InMemoryActionCacheAdapter instance with a new empty cache. */
  public InMemoryActionCacheAdapter() {
    this(new ConcurrentHashMap<>());
  }

  @Override
  public FlushResult flushEntries(FlushCriteria criteria) {
    Preconditions.checkNotNull(criteria, "criteria");

    switch (criteria.getScope()) {
      case ALL:
        return flushAllEntries();
      case INSTANCE:
        return flushInstanceEntries(criteria.getInstanceName());
      case DIGEST_PREFIX:
        return flushDigestPrefixEntries(criteria.getDigestPrefix());
      default:
        return new FlushResult(false, "Unknown flush scope: " + criteria.getScope(), 0, 0);
    }
  }

  /**
   * Flushes all Action Cache entries from memory.
   *
   * @return the result of the flush operation
   */
  private FlushResult flushAllEntries() {
    int entriesRemoved = cache.size();
    cache.clear();
    return new FlushResult(true, "Flushed all Action Cache entries from memory", entriesRemoved, 0);
  }

  /**
   * Flushes Action Cache entries for a specific instance from memory.
   *
   * @param instanceName the instance name
   * @return the result of the flush operation
   */
  private FlushResult flushInstanceEntries(String instanceName) {
    return flushEntriesWithPredicate(
        key -> key.getInstanceName().equals(instanceName),
        "Flushed Action Cache entries for instance " + instanceName);
  }

  /**
   * Flushes Action Cache entries with a specific digest prefix from memory.
   *
   * @param digestPrefix the digest prefix
   * @return the result of the flush operation
   */
  private FlushResult flushDigestPrefixEntries(String digestPrefix) {
    return flushEntriesWithPredicate(
        key -> key.getDigest().getHash().startsWith(digestPrefix),
        "Flushed Action Cache entries with digest prefix " + digestPrefix);
  }

  /**
   * Flushes Action Cache entries that match the given predicate.
   *
   * @param predicate the predicate to match
   * @param message the message to include in the result
   * @return the result of the flush operation
   */
  private FlushResult flushEntriesWithPredicate(
      Predicate<ActionCacheKey> predicate, String message) {
    int entriesRemoved = 0;

    for (Map.Entry<ActionCacheKey, ActionResult> entry : cache.entrySet()) {
      if (predicate.test(entry.getKey())) {
        cache.remove(entry.getKey());
        entriesRemoved++;
      }
    }

    return new FlushResult(true, message, entriesRemoved, 0);
  }

  /** Key for the in-memory Action Cache. */
  public static class ActionCacheKey {
    private final String instanceName;
    private final Digest digest;

    /**
     * Creates a new ActionCacheKey instance.
     *
     * @param instanceName the instance name
     * @param digest the action digest
     */
    public ActionCacheKey(String instanceName, Digest digest) {
      this.instanceName = Preconditions.checkNotNull(instanceName, "instanceName");
      this.digest = Preconditions.checkNotNull(digest, "digest");
    }

    /**
     * Gets the instance name.
     *
     * @return the instance name
     */
    public String getInstanceName() {
      return instanceName;
    }

    /**
     * Gets the action digest.
     *
     * @return the action digest
     */
    public Digest getDigest() {
      return digest;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ActionCacheKey)) {
        return false;
      }
      ActionCacheKey other = (ActionCacheKey) obj;
      return instanceName.equals(other.instanceName) && digest.equals(other.digest);
    }

    @Override
    public int hashCode() {
      return 31 * instanceName.hashCode() + digest.hashCode();
    }
  }
}
