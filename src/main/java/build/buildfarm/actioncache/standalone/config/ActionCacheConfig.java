package build.buildfarm.actioncache.standalone.config;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** Configuration for the standalone Action Cache. */
public class ActionCacheConfig {
  private boolean enableInMemoryCache = true;
  private int inMemoryCacheMaxSize = 10000;
  private List<AdapterConfig> adapters = new ArrayList<>();
  private CacheEvictionPolicy evictionPolicy = CacheEvictionPolicy.LRU;

  /**
   * Returns whether the in-memory cache is enabled.
   *
   * @return true if the in-memory cache is enabled, false otherwise
   */
  public boolean isEnableInMemoryCache() {
    return enableInMemoryCache;
  }

  /**
   * Sets whether the in-memory cache is enabled.
   *
   * @param enableInMemoryCache true to enable the in-memory cache, false otherwise
   */
  public void setEnableInMemoryCache(boolean enableInMemoryCache) {
    this.enableInMemoryCache = enableInMemoryCache;
  }

  /**
   * Returns the maximum size of the in-memory cache.
   *
   * @return the maximum size of the in-memory cache
   */
  public int getInMemoryCacheMaxSize() {
    return inMemoryCacheMaxSize;
  }

  /**
   * Sets the maximum size of the in-memory cache.
   *
   * @param inMemoryCacheMaxSize the maximum size of the in-memory cache
   */
  public void setInMemoryCacheMaxSize(int inMemoryCacheMaxSize) {
    this.inMemoryCacheMaxSize = inMemoryCacheMaxSize;
  }

  /**
   * Returns the list of adapter configurations.
   *
   * @return the list of adapter configurations
   */
  public List<AdapterConfig> getAdapters() {
    return adapters;
  }

  /**
   * Sets the list of adapter configurations.
   *
   * @param adapters the list of adapter configurations
   */
  public void setAdapters(List<AdapterConfig> adapters) {
    this.adapters = adapters;
  }

  /**
   * Returns the cache eviction policy.
   *
   * @return the cache eviction policy
   */
  public CacheEvictionPolicy getEvictionPolicy() {
    return evictionPolicy;
  }

  /**
   * Sets the cache eviction policy.
   *
   * @param evictionPolicy the cache eviction policy
   */
  public void setEvictionPolicy(CacheEvictionPolicy evictionPolicy) {
    this.evictionPolicy = evictionPolicy;
  }

  /**
   * Validates the configuration.
   *
   * @return null if the configuration is valid, an error message otherwise
   */
  @Nullable
  public String validate() {
    if (inMemoryCacheMaxSize <= 0) {
      return "In-memory cache max size must be greater than 0";
    }

    for (AdapterConfig adapter : adapters) {
      String validationError = adapter.validate();
      if (validationError != null) {
        return validationError;
      }
    }

    return null;
  }
}
