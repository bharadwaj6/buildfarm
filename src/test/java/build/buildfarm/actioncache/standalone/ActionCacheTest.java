package build.buildfarm.actioncache.standalone;

import static com.google.common.truth.Truth.assertThat;

import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ActionCacheTest {

  private TestActionCache actionCache;

  @Before
  public void setUp() {
    actionCache = new TestActionCache();
  }

  @Test
  public void testGetReturnsNullForNonExistentKey() {
    Digest actionKey = Digest.newBuilder().setHash("non-existent").setSizeBytes(0).build();

    ActionResult result = actionCache.get(actionKey);

    assertThat(result).isNull();
  }

  @Test
  public void testPutAndGet() {
    Digest actionKey = Digest.newBuilder().setHash("test-hash").setSizeBytes(10).build();

    ActionResult actionResult = ActionResult.newBuilder().setExitCode(0).build();

    actionCache.put(actionKey, actionResult);
    ActionResult retrievedResult = actionCache.get(actionKey);

    assertThat(retrievedResult).isEqualTo(actionResult);
  }

  @Test
  public void testFlushAll() {
    Digest actionKey = Digest.newBuilder().setHash("test-hash").setSizeBytes(10).build();

    ActionResult actionResult = ActionResult.newBuilder().setExitCode(0).build();

    actionCache.put(actionKey, actionResult);

    FlushCriteria criteria = new FlushCriteria(FlushScope.ALL, null, null);
    FlushResult result = actionCache.flush(criteria);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(1);
    assertThat(actionCache.get(actionKey)).isNull();
  }

  @Test
  public void testFlushByInstance() {
    Digest actionKey1 = Digest.newBuilder().setHash("test-hash-1").setSizeBytes(10).build();

    Digest actionKey2 = Digest.newBuilder().setHash("test-hash-2").setSizeBytes(10).build();

    ActionResult actionResult = ActionResult.newBuilder().setExitCode(0).build();

    actionCache.put(actionKey1, actionResult);
    actionCache.put(actionKey2, actionResult);
    actionCache.setInstanceForKey(actionKey1, "instance1");
    actionCache.setInstanceForKey(actionKey2, "instance2");

    FlushCriteria criteria = new FlushCriteria(FlushScope.INSTANCE, "instance1", null);
    FlushResult result = actionCache.flush(criteria);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(1);
    assertThat(actionCache.get(actionKey1)).isNull();
    assertThat(actionCache.get(actionKey2)).isEqualTo(actionResult);
  }

  @Test
  public void testFlushByDigestPrefix() {
    Digest actionKey1 = Digest.newBuilder().setHash("abc123").setSizeBytes(10).build();

    Digest actionKey2 = Digest.newBuilder().setHash("def456").setSizeBytes(10).build();

    ActionResult actionResult = ActionResult.newBuilder().setExitCode(0).build();

    actionCache.put(actionKey1, actionResult);
    actionCache.put(actionKey2, actionResult);

    FlushCriteria criteria = new FlushCriteria(FlushScope.DIGEST_PREFIX, null, "abc");
    FlushResult result = actionCache.flush(criteria);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(1);
    assertThat(actionCache.get(actionKey1)).isNull();
    assertThat(actionCache.get(actionKey2)).isEqualTo(actionResult);
  }

  /** Test implementation of ActionCache for testing the interface contract. */
  private static class TestActionCache implements ActionCache {
    private final java.util.Map<Digest, ActionResult> cache = new java.util.HashMap<>();
    private final java.util.Map<Digest, String> instanceMap = new java.util.HashMap<>();

    @Override
    public ActionResult get(Digest actionKey) {
      return cache.get(actionKey);
    }

    @Override
    public void put(Digest actionKey, ActionResult actionResult) {
      cache.put(actionKey, actionResult);
    }

    @Override
    public FlushResult flush(FlushCriteria criteria) {
      int entriesRemoved = 0;

      if (criteria.getScope() == FlushScope.ALL) {
        entriesRemoved = cache.size();
        cache.clear();
        instanceMap.clear();
      } else if (criteria.getScope() == FlushScope.INSTANCE) {
        String instanceName = criteria.getInstanceName();
        java.util.Iterator<java.util.Map.Entry<Digest, String>> it =
            instanceMap.entrySet().iterator();
        while (it.hasNext()) {
          java.util.Map.Entry<Digest, String> entry = it.next();
          if (instanceName.equals(entry.getValue())) {
            cache.remove(entry.getKey());
            it.remove();
            entriesRemoved++;
          }
        }
      } else if (criteria.getScope() == FlushScope.DIGEST_PREFIX) {
        String prefix = criteria.getDigestPrefix();
        java.util.Iterator<Digest> it = cache.keySet().iterator();
        while (it.hasNext()) {
          Digest key = it.next();
          if (key.getHash().startsWith(prefix)) {
            it.remove();
            instanceMap.remove(key);
            entriesRemoved++;
          }
        }
      }

      return new FlushResult(true, "Flushed " + entriesRemoved + " entries", entriesRemoved, 0);
    }

    public void setInstanceForKey(Digest key, String instance) {
      instanceMap.put(key, instance);
    }
  }
}
