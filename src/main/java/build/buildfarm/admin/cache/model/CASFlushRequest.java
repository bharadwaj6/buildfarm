package build.buildfarm.admin.cache.model;

/**
 * Request to flush Content Addressable Storage entries.
 */
public class CASFlushRequest {
  private FlushScope scope;
  private String instanceName;
  private String digestPrefix;
  private boolean flushFilesystem;
  private boolean flushInMemoryLRU;
  private boolean flushRedisWorkerMap;

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
  public String getInstanceName() {
    return instanceName;
  }

  /**
   * Sets the instance name.
   *
   * @param instanceName the instance name
   */
  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }

  /**
   * Gets the digest prefix.
   *
   * @return the digest prefix
   */
  public String getDigestPrefix() {
    return digestPrefix;
  }

  /**
   * Sets the digest prefix.
   *
   * @param digestPrefix the digest prefix
   */
  public void setDigestPrefix(String digestPrefix) {
    this.digestPrefix = digestPrefix;
  }

  /**
   * Gets whether to flush filesystem-backed entries.
   *
   * @return true if filesystem-backed entries should be flushed, false otherwise
   */
  public boolean isFlushFilesystem() {
    return flushFilesystem;
  }

  /**
   * Sets whether to flush filesystem-backed entries.
   *
   * @param flushFilesystem true if filesystem-backed entries should be flushed, false otherwise
   */
  public void setFlushFilesystem(boolean flushFilesystem) {
    this.flushFilesystem = flushFilesystem;
  }

  /**
   * Gets whether to flush in-memory LRU cache entries.
   *
   * @return true if in-memory LRU cache entries should be flushed, false otherwise
   */
  public boolean isFlushInMemoryLRU() {
    return flushInMemoryLRU;
  }

  /**
   * Sets whether to flush in-memory LRU cache entries.
   *
   * @param flushInMemoryLRU true if in-memory LRU cache entries should be flushed, false otherwise
   */
  public void setFlushInMemoryLRU(boolean flushInMemoryLRU) {
    this.flushInMemoryLRU = flushInMemoryLRU;
  }

  /**
   * Gets whether to flush Redis-backed CAS worker map entries.
   *
   * @return true if Redis-backed CAS worker map entries should be flushed, false otherwise
   */
  public boolean isFlushRedisWorkerMap() {
    return flushRedisWorkerMap;
  }

  /**
   * Sets whether to flush Redis-backed CAS worker map entries.
   *
   * @param flushRedisWorkerMap true if Redis-backed CAS worker map entries should be flushed, false otherwise
   */
  public void setFlushRedisWorkerMap(boolean flushRedisWorkerMap) {
    this.flushRedisWorkerMap = flushRedisWorkerMap;
  }
}