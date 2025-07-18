package build.buildfarm.admin.cache.model;

import com.google.common.base.Preconditions;

/** Criteria for a cache flush operation. */
public class FlushCriteria {
  private final FlushScope scope;
  private final String instanceName;
  private final String digestPrefix;

  /**
   * Creates a new FlushCriteria instance.
   *
   * @param scope the scope of the flush operation
   * @param instanceName the instance name (only used when scope is INSTANCE)
   * @param digestPrefix the digest prefix (only used when scope is DIGEST_PREFIX)
   */
  public FlushCriteria(FlushScope scope, String instanceName, String digestPrefix) {
    this.scope = Preconditions.checkNotNull(scope, "scope");

    if (scope == FlushScope.INSTANCE) {
      Preconditions.checkNotNull(
          instanceName, "instanceName must be provided when scope is INSTANCE");
    }

    if (scope == FlushScope.DIGEST_PREFIX) {
      Preconditions.checkNotNull(
          digestPrefix, "digestPrefix must be provided when scope is DIGEST_PREFIX");
    }

    this.instanceName = instanceName;
    this.digestPrefix = digestPrefix;
  }

  /**
   * Gets the scope of the flush operation.
   *
   * @return the scope
   */
  public FlushScope getScope() {
    return scope;
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
   * Gets the digest prefix.
   *
   * @return the digest prefix
   */
  public String getDigestPrefix() {
    return digestPrefix;
  }
}
