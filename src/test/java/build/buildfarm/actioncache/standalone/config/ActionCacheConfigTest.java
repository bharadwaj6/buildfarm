package build.buildfarm.actioncache.standalone.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ActionCacheConfigTest {

  @Test
  public void testDefaultValues() {
    ActionCacheConfig config = new ActionCacheConfig();
    assertTrue(config.isEnableInMemoryCache());
    assertEquals(10000, config.getInMemoryCacheMaxSize());
    assertEquals(0, config.getAdapters().size());
    assertEquals(CacheEvictionPolicy.LRU, config.getEvictionPolicy());
    assertNull(config.validate());
  }
  
  @Test
  public void testSetters() {
    ActionCacheConfig config = new ActionCacheConfig();
    
    config.setEnableInMemoryCache(false);
    assertFalse(config.isEnableInMemoryCache());
    
    config.setInMemoryCacheMaxSize(5000);
    assertEquals(5000, config.getInMemoryCacheMaxSize());
    
    List<AdapterConfig> adapters = new ArrayList<>();
    AdapterConfig adapter = new AdapterConfig();
    adapter.setType(AdapterType.IN_MEMORY);
    adapters.add(adapter);
    config.setAdapters(adapters);
    assertEquals(1, config.getAdapters().size());
    assertEquals(AdapterType.IN_MEMORY, config.getAdapters().get(0).getType());
    
    config.setEvictionPolicy(CacheEvictionPolicy.FIFO);
    assertEquals(CacheEvictionPolicy.FIFO, config.getEvictionPolicy());
  }
  
  @Test
  public void testValidation() {
    ActionCacheConfig config = new ActionCacheConfig();
    
    // Valid configuration
    assertNull(config.validate());
    
    // Invalid max cache size
    config.setInMemoryCacheMaxSize(0);
    assertEquals("In-memory cache max size must be greater than 0", config.validate());
    
    // Reset to valid value
    config.setInMemoryCacheMaxSize(1000);
    assertNull(config.validate());
    
    // Invalid adapter
    List<AdapterConfig> adapters = new ArrayList<>();
    AdapterConfig adapter = new AdapterConfig();
    adapter.setType(AdapterType.REDIS);
    adapters.add(adapter);
    config.setAdapters(adapters);
    assertEquals("Redis adapter requires 'host' property", config.validate());
  }
}