package build.buildfarm.actioncache.standalone.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ActionCacheConfigLoaderTest {

  @Test
  public void testLoadFromStream() throws Exception {
    String yamlConfig = 
        "enableInMemoryCache: true\n" +
        "inMemoryCacheMaxSize: 5000\n" +
        "evictionPolicy: LRU\n" +
        "adapters:\n" +
        "  - type: REDIS\n" +
        "    properties:\n" +
        "      host: localhost\n" +
        "      port: 6379\n" +
        "  - type: IN_MEMORY\n" +
        "    properties: {}\n";
    
    InputStream inputStream = new ByteArrayInputStream(yamlConfig.getBytes(StandardCharsets.UTF_8));
    ActionCacheConfig config = ActionCacheConfigLoader.loadFromStream(inputStream);
    
    assertTrue(config.isEnableInMemoryCache());
    assertEquals(5000, config.getInMemoryCacheMaxSize());
    assertEquals(CacheEvictionPolicy.LRU, config.getEvictionPolicy());
    assertEquals(2, config.getAdapters().size());
    
    AdapterConfig redisAdapter = config.getAdapters().get(0);
    assertEquals(AdapterType.REDIS, redisAdapter.getType());
    assertEquals("localhost", redisAdapter.getProperties().get("host"));
    assertEquals("6379", redisAdapter.getProperties().get("port"));
    
    AdapterConfig inMemoryAdapter = config.getAdapters().get(1);
    assertEquals(AdapterType.IN_MEMORY, inMemoryAdapter.getType());
    assertEquals(0, inMemoryAdapter.getProperties().size());
  }
  
  @Test(expected = ActionCacheConfigLoader.ConfigurationException.class)
  public void testLoadFromStreamInvalidYaml() throws Exception {
    String yamlConfig = "invalid: yaml: content: - missing colon";
    InputStream inputStream = new ByteArrayInputStream(yamlConfig.getBytes(StandardCharsets.UTF_8));
    ActionCacheConfigLoader.loadFromStream(inputStream);
  }
  
  @Test(expected = ActionCacheConfigLoader.ConfigurationException.class)
  public void testLoadFromStreamInvalidConfig() throws Exception {
    String yamlConfig = 
        "enableInMemoryCache: true\n" +
        "inMemoryCacheMaxSize: 0\n"; // Invalid max cache size
    
    InputStream inputStream = new ByteArrayInputStream(yamlConfig.getBytes(StandardCharsets.UTF_8));
    ActionCacheConfigLoader.loadFromStream(inputStream);
  }
  
  @Test
  public void testLoadFromMap() throws Exception {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("enableInMemoryCache", false);
    configMap.put("inMemoryCacheMaxSize", 2000);
    configMap.put("evictionPolicy", "FIFO");
    
    ActionCacheConfig config = ActionCacheConfigLoader.loadFromMap(configMap);
    
    assertFalse(config.isEnableInMemoryCache());
    assertEquals(2000, config.getInMemoryCacheMaxSize());
    assertEquals(CacheEvictionPolicy.FIFO, config.getEvictionPolicy());
  }
  
  @Test(expected = ActionCacheConfigLoader.ConfigurationException.class)
  public void testLoadFromMapInvalidConfig() throws Exception {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("inMemoryCacheMaxSize", 0); // Invalid max cache size
    
    ActionCacheConfigLoader.loadFromMap(configMap);
  }
}