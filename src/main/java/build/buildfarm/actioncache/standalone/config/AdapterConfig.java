package build.buildfarm.actioncache.standalone.config;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Configuration for an Action Cache adapter.
 */
public class AdapterConfig {
  private AdapterType type;
  private Map<String, String> properties = new HashMap<>();
  
  /**
   * Returns the adapter type.
   *
   * @return the adapter type
   */
  public AdapterType getType() {
    return type;
  }
  
  /**
   * Sets the adapter type.
   *
   * @param type the adapter type
   */
  public void setType(AdapterType type) {
    this.type = type;
  }
  
  /**
   * Returns the adapter properties.
   *
   * @return the adapter properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }
  
  /**
   * Sets the adapter properties.
   *
   * @param properties the adapter properties
   */
  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
  
  /**
   * Validates the adapter configuration.
   *
   * @return null if the configuration is valid, an error message otherwise
   */
  @Nullable
  public String validate() {
    if (type == null) {
      return "Adapter type must be specified";
    }
    
    switch (type) {
      case REDIS:
        return validateRedisConfig();
      case IN_MEMORY:
        return validateInMemoryConfig();
      case FILE_SYSTEM:
        return validateFileSystemConfig();
      default:
        return "Unknown adapter type: " + type;
    }
  }
  
  private String validateRedisConfig() {
    if (!properties.containsKey("host")) {
      return "Redis adapter requires 'host' property";
    }
    if (!properties.containsKey("port")) {
      return "Redis adapter requires 'port' property";
    }
    try {
      Integer.parseInt(properties.get("port"));
    } catch (NumberFormatException e) {
      return "Redis adapter 'port' property must be a valid integer";
    }
    return null;
  }
  
  private String validateInMemoryConfig() {
    // No specific validation for in-memory adapter
    return null;
  }
  
  private String validateFileSystemConfig() {
    if (!properties.containsKey("path")) {
      return "File system adapter requires 'path' property";
    }
    return null;
  }
}