package build.buildfarm.admin.cache.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request to flush Action Cache entries.
 */
public class ActionCacheFlushRequest {
  private FlushScope scope;
  private String instanceName;
  private String digestPrefix;
  private boolean flushRedis;
  private boolean flushInMemory;

  /**
   * Gets the scope of the flush operation.
   *
   * @return the scope
   */
  public FlushScope getScope() {
    return scope;
  }

  /**
   * Sets the scope of the flush operation.
   *
   * @param scope the scope
   */
  public void setScope(FlushScope scope) {
    this.scope = scope;
  }

  /**
   * Gets the instance name.
   *
   * @return the instance name
   */
  @JsonProperty("instanceName")
  public String getInstanceName() {
    return instanceName;
  }

  /**
   * Sets the instance name.
   *
   * @param instanceName the instance name
   */
  @JsonProperty("instanceName")
  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }

  /**
   * Gets the digest prefix.
   *
   * @return the digest prefix
   */
  @JsonProperty("digestPrefix")
  public String getDigestPrefix() {
    return digestPrefix;
  }

  /**
   * Sets the digest prefix.
   *
   * @param digestPrefix the digest prefix
   */
  @JsonProperty("digestPrefix")
  public void setDigestPrefix(String digestPrefix) {
    this.digestPrefix = digestPrefix;
  }

  /**
   * Gets whether to flush Redis-backed entries.
   *
   * @return true if Redis-backed entries should be flushed, false otherwise
   */
  @JsonProperty("flushRedis")
  public boolean isFlushRedis() {
    return flushRedis;
  }

  /**
   * Sets whether to flush Redis-backed entries.
   *
   * @param flushRedis true if Redis-backed entries should be flushed, false otherwise
   */
  @JsonProperty("flushRedis")
  public void setFlushRedis(boolean flushRedis) {
    this.flushRedis = flushRedis;
  }

  /**
   * Gets whether to flush in-memory entries.
   *
   * @return true if in-memory entries should be flushed, false otherwise
   */
  @JsonProperty("flushInMemory")
  public boolean isFlushInMemory() {
    return flushInMemory;
  }

  /**
   * Sets whether to flush in-memory entries.
   *
   * @param flushInMemory true if in-memory entries should be flushed, false otherwise
   */
  @JsonProperty("flushInMemory")
  public void setFlushInMemory(boolean flushInMemory) {
    this.flushInMemory = flushInMemory;
  }
}