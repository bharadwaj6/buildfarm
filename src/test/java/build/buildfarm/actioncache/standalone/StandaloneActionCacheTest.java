package build.buildfarm.actioncache.standalone;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;
import build.buildfarm.admin.cache.adapter.ac.ActionCacheAdapter;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class StandaloneActionCacheTest {

  @Mock private ActionCacheAdapter mockAdapter1;
  @Mock private ActionCacheAdapter mockAdapter2;

  private StandaloneActionCache actionCache;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    actionCache = new StandaloneActionCache(Arrays.asList(mockAdapter1, mockAdapter2));
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

    FlushResult adapterResult1 = new FlushResult(true, "Adapter 1 flushed", 5, 0);
    FlushResult adapterResult2 = new FlushResult(true, "Adapter 2 flushed", 10, 0);

    when(mockAdapter1.flushEntries(criteria)).thenReturn(adapterResult1);
    when(mockAdapter2.flushEntries(criteria)).thenReturn(adapterResult2);

    FlushResult result = actionCache.flush(criteria);

    verify(mockAdapter1).flushEntries(criteria);
    verify(mockAdapter2).flushEntries(criteria);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isGreaterThan(15); // 5 + 10 + in-memory entries
    assertThat(actionCache.get(actionKey)).isNull();
  }

  @Test
  public void testFlushByDigestPrefix() {
    Digest actionKey1 = Digest.newBuilder().setHash("abc123").setSizeBytes(10).build();

    Digest actionKey2 = Digest.newBuilder().setHash("def456").setSizeBytes(10).build();

    ActionResult actionResult = ActionResult.newBuilder().setExitCode(0).build();

    actionCache.put(actionKey1, actionResult);
    actionCache.put(actionKey2, actionResult);

    FlushCriteria criteria = new FlushCriteria(FlushScope.DIGEST_PREFIX, null, "abc");

    FlushResult adapterResult1 = new FlushResult(true, "Adapter 1 flushed", 3, 0);
    FlushResult adapterResult2 = new FlushResult(true, "Adapter 2 flushed", 2, 0);

    when(mockAdapter1.flushEntries(criteria)).thenReturn(adapterResult1);
    when(mockAdapter2.flushEntries(criteria)).thenReturn(adapterResult2);

    FlushResult result = actionCache.flush(criteria);

    verify(mockAdapter1).flushEntries(criteria);
    verify(mockAdapter2).flushEntries(criteria);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isGreaterThan(5); // 3 + 2 + in-memory entries
    assertThat(actionCache.get(actionKey1)).isNull();
    assertThat(actionCache.get(actionKey2)).isEqualTo(actionResult);
  }
}
