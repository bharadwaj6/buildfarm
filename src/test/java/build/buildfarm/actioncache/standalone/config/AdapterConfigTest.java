package build.buildfarm.actioncache.standalone.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AdapterConfigTest {

  @Test
  public void testDefaultValues() {
    AdapterConfig config = new AdapterConfig();
    assertEquals(0, config.getProperties().size());
    assertEquals("Adapter type must be specified", config.validate());
  }

  @Test
  public void testSetters() {
    AdapterConfig config = new AdapterConfig();

    config.setType(AdapterType.REDIS);
    assertEquals(AdapterType.REDIS, config.getType());

    Map<String, String> properties = new HashMap<>();
    properties.put("host", "localhost");
    properties.put("port", "6379");
    config.setProperties(properties);
    assertEquals(2, config.getProperties().size());
    assertEquals("localhost", config.getProperties().get("host"));
    assertEquals("6379", config.getProperties().get("port"));
  }

  @Test
  public void testValidateRedisAdapter() {
    AdapterConfig config = new AdapterConfig();
    config.setType(AdapterType.REDIS);

    // Missing host and port
    assertEquals("Redis adapter requires 'host' property", config.validate());

    // Missing port
    Map<String, String> properties = new HashMap<>();
    properties.put("host", "localhost");
    config.setProperties(properties);
    assertEquals("Redis adapter requires 'port' property", config.validate());

    // Invalid port
    properties.put("port", "invalid");
    config.setProperties(properties);
    assertEquals("Redis adapter 'port' property must be a valid integer", config.validate());

    // Valid configuration
    properties.put("port", "6379");
    config.setProperties(properties);
    assertNull(config.validate());
  }

  @Test
  public void testValidateInMemoryAdapter() {
    AdapterConfig config = new AdapterConfig();
    config.setType(AdapterType.IN_MEMORY);

    // In-memory adapter doesn't require any properties
    assertNull(config.validate());
  }

  @Test
  public void testValidateFileSystemAdapter() {
    AdapterConfig config = new AdapterConfig();
    config.setType(AdapterType.FILE_SYSTEM);

    // Missing path
    assertEquals("File system adapter requires 'path' property", config.validate());

    // Valid configuration
    Map<String, String> properties = new HashMap<>();
    properties.put("path", "/tmp/cache");
    config.setProperties(properties);
    assertNull(config.validate());
  }
}
