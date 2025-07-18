package build.buildfarm.actioncache.standalone.config;

import static org.junit.Assert.assertNotNull;

import build.buildfarm.actioncache.standalone.ActionCache;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ActionCacheFactoryTest {

  @Test
  public void testCreateWithEmptyAdapters() throws Exception {
    ActionCacheConfig config = new ActionCacheConfig();
    ActionCache actionCache = ActionCacheFactory.create(config);
    assertNotNull(actionCache);
  }

  // Note: The following test is commented out because the adapter implementations
  // are not yet available. Uncomment and implement when the adapters are available.
  /*
  @Test
  public void testCreateWithAdapters() throws Exception {
    ActionCacheConfig config = new ActionCacheConfig();

    List<AdapterConfig> adapterConfigs = new ArrayList<>();

    AdapterConfig inMemoryConfig = new AdapterConfig();
    inMemoryConfig.setType(AdapterType.IN_MEMORY);
    adapterConfigs.add(inMemoryConfig);

    config.setAdapters(adapterConfigs);

    ActionCache actionCache = ActionCacheFactory.create(config);
    assertNotNull(actionCache);
  }
  */

  @Test(expected = ActionCacheConfigLoader.ConfigurationException.class)
  public void testCreateWithInvalidAdapterType() throws Exception {
    ActionCacheConfig config = new ActionCacheConfig();

    List<AdapterConfig> adapterConfigs = new ArrayList<>();

    AdapterConfig redisConfig = new AdapterConfig();
    redisConfig.setType(AdapterType.REDIS);
    adapterConfigs.add(redisConfig);

    config.setAdapters(adapterConfigs);

    // This should throw an exception because the Redis adapter is not yet implemented
    ActionCacheFactory.create(config);
  }
}
