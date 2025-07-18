package build.buildfarm.actioncache.standalone.config;

import build.buildfarm.actioncache.standalone.ActionCache;
import build.buildfarm.actioncache.standalone.StandaloneActionCache;
import build.buildfarm.admin.cache.adapter.common.ActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.impl.InMemoryActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.impl.RedisActionCacheAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Factory for creating Action Cache instances from configuration. */
public class ActionCacheFactory {

  /**
   * Creates a new Action Cache instance from the given configuration.
   *
   * @param config the configuration
   * @return the created Action Cache instance
   * @throws ActionCacheConfigLoader.ConfigurationException if the configuration is invalid
   */
  public static ActionCache create(ActionCacheConfig config)
      throws ActionCacheConfigLoader.ConfigurationException {
    List<ActionCacheAdapter> adapters = createAdapters(config);
    return new StandaloneActionCache(adapters);
  }

  private static List<ActionCacheAdapter> createAdapters(ActionCacheConfig config)
      throws ActionCacheConfigLoader.ConfigurationException {
    List<ActionCacheAdapter> adapters = new ArrayList<>();

    for (AdapterConfig adapterConfig : config.getAdapters()) {
      ActionCacheAdapter adapter = createAdapter(adapterConfig);
      adapters.add(adapter);
    }

    return adapters;
  }

  private static ActionCacheAdapter createAdapter(AdapterConfig config)
      throws ActionCacheConfigLoader.ConfigurationException {
    switch (config.getType()) {
      case REDIS:
        return createRedisAdapter(config.getProperties());
      case IN_MEMORY:
        return createInMemoryAdapter(config.getProperties());
      case FILE_SYSTEM:
        return createFileSystemAdapter(config.getProperties());
      default:
        throw new ActionCacheConfigLoader.ConfigurationException(
            "Unknown adapter type: " + config.getType());
    }
  }

  private static ActionCacheAdapter createRedisAdapter(Map<String, String> properties)
      throws ActionCacheConfigLoader.ConfigurationException {
    // This would be implemented when the Redis adapter is implemented
    // For now, we'll throw an exception
    throw new ActionCacheConfigLoader.ConfigurationException("Redis adapter not yet implemented");
  }

  private static ActionCacheAdapter createInMemoryAdapter(Map<String, String> properties)
      throws ActionCacheConfigLoader.ConfigurationException {
    // This would be implemented when the in-memory adapter is implemented
    // For now, we'll throw an exception
    throw new ActionCacheConfigLoader.ConfigurationException(
        "In-memory adapter not yet implemented");
  }

  private static ActionCacheAdapter createFileSystemAdapter(Map<String, String> properties)
      throws ActionCacheConfigLoader.ConfigurationException {
    // This would be implemented when the file system adapter is implemented
    // For now, we'll throw an exception
    throw new ActionCacheConfigLoader.ConfigurationException(
        "File system adapter not yet implemented");
  }
}
