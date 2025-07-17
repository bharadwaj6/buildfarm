package build.buildfarm.actioncache.standalone.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Loader for Action Cache configuration.
 */
public class ActionCacheConfigLoader {
  
  /**
   * Loads the Action Cache configuration from a YAML file.
   *
   * @param configFile the configuration file
   * @return the loaded configuration
   * @throws IOException if an I/O error occurs
   * @throws ConfigurationException if the configuration is invalid
   */
  public static ActionCacheConfig loadFromFile(File configFile) throws IOException, ConfigurationException {
    try (InputStream inputStream = new FileInputStream(configFile)) {
      return loadFromStream(inputStream);
    }
  }
  
  /**
   * Loads the Action Cache configuration from an input stream.
   *
   * @param inputStream the input stream
   * @return the loaded configuration
   * @throws ConfigurationException if the configuration is invalid
   */
  public static ActionCacheConfig loadFromStream(InputStream inputStream) throws ConfigurationException {
    try {
      Yaml yaml = new Yaml(new Constructor(ActionCacheConfig.class));
      ActionCacheConfig config = yaml.load(inputStream);
      validateConfig(config);
      return config;
    } catch (YAMLException e) {
      throw new ConfigurationException("Failed to parse configuration: " + e.getMessage(), e);
    }
  }
  
  /**
   * Loads the Action Cache configuration from a map.
   *
   * @param configMap the configuration map
   * @return the loaded configuration
   * @throws ConfigurationException if the configuration is invalid
   */
  @VisibleForTesting
  public static ActionCacheConfig loadFromMap(Map<String, Object> configMap) throws ConfigurationException {
    try {
      Yaml yaml = new Yaml(new Constructor(ActionCacheConfig.class));
      String yamlString = yaml.dump(configMap);
      ActionCacheConfig config = yaml.load(yamlString);
      validateConfig(config);
      return config;
    } catch (YAMLException e) {
      throw new ConfigurationException("Failed to parse configuration: " + e.getMessage(), e);
    }
  }
  
  private static void validateConfig(ActionCacheConfig config) throws ConfigurationException {
    Preconditions.checkNotNull(config, "Configuration cannot be null");
    
    String validationError = config.validate();
    if (validationError != null) {
      throw new ConfigurationException(validationError);
    }
  }
  
  /**
   * Exception thrown when a configuration error occurs.
   */
  public static class ConfigurationException extends Exception {
    public ConfigurationException(String message) {
      super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}