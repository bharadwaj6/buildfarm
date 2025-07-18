package build.buildfarm.admin.cache.model;

/** Defines the scope of a cache flush operation. */
public enum FlushScope {
  /** Flush all entries in the cache. */
  ALL,

  /** Flush entries for a specific instance. */
  INSTANCE,

  /** Flush entries with a specific digest prefix. */
  DIGEST_PREFIX
}
