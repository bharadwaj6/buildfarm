package build.buildfarm.actioncache.standalone.config;

/**
 * Enum representing the cache eviction policies.
 */
public enum CacheEvictionPolicy {
  LRU,
  LFU,
  FIFO
}