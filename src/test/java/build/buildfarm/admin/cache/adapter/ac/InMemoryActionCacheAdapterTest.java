package build.buildfarm.admin.cache.adapter.ac;

import static com.google.common.truth.Truth.assertThat;

import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;
import build.buildfarm.admin.cache.adapter.ac.InMemoryActionCacheAdapter.ActionCacheKey;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import com.google.protobuf.ByteString;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link InMemoryActionCacheAdapter}.
 */
@RunWith(JUnit4.class)
public class InMemoryActionCacheAdapterTest {
  
  private ConcurrentMap<ActionCacheKey, ActionResult> cache;
  private InMemoryActionCacheAdapter adapter;
  
  @Before
  public void setUp() {
    cache = new ConcurrentHashMap<>();
    adapter = new InMemoryActionCacheAdapter(cache);
    
    // Add some test entries
    cache.put(
        new ActionCacheKey("instance1", createDigest("abc123")),
        ActionResult.getDefaultInstance());
    cache.put(
        new ActionCacheKey("instance1", createDigest("def456")),
        ActionResult.getDefaultInstance());
    cache.put(
        new ActionCacheKey("instance2", createDigest("abc789")),
        ActionResult.getDefaultInstance());
  }
  
  @Test
  public void testFlushAllEntries() {
    // Execute
    FlushCriteria criteria = new FlushCriteria(FlushScope.ALL, null, null);
    FlushResult result = adapter.flushEntries(criteria);
    
    // Verify
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(3);
    assertThat(cache).isEmpty();
  }
  
  @Test
  public void testFlushInstanceEntries() {
    // Execute
    FlushCriteria criteria = new FlushCriteria(FlushScope.INSTANCE, "instance1", null);
    FlushResult result = adapter.flushEntries(criteria);
    
    // Verify
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(2);
    assertThat(cache).hasSize(1);
    assertThat(cache.keySet().iterator().next().getInstanceName()).isEqualTo("instance2");
  }
  
  @Test
  public void testFlushDigestPrefixEntries() {
    // Execute
    FlushCriteria criteria = new FlushCriteria(FlushScope.DIGEST_PREFIX, null, "abc");
    FlushResult result = adapter.flushEntries(criteria);
    
    // Verify
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(2);
    assertThat(cache).hasSize(1);
    assertThat(cache.keySet().iterator().next().getDigest().getHash()).isEqualTo("def456");
  }
  
  @Test
  public void testFlushEntriesWithNoMatches() {
    // Execute
    FlushCriteria criteria = new FlushCriteria(FlushScope.INSTANCE, "instance3", null);
    FlushResult result = adapter.flushEntries(criteria);
    
    // Verify
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(0);
    assertThat(cache).hasSize(3);
  }
  
  @Test
  public void testActionCacheKeyEquality() {
    // Create two keys with the same values
    ActionCacheKey key1 = new ActionCacheKey("instance", createDigest("abc123"));
    ActionCacheKey key2 = new ActionCacheKey("instance", createDigest("abc123"));
    
    // Create a key with different instance
    ActionCacheKey key3 = new ActionCacheKey("other", createDigest("abc123"));
    
    // Create a key with different digest
    ActionCacheKey key4 = new ActionCacheKey("instance", createDigest("def456"));
    
    // Verify
    assertThat(key1).isEqualTo(key2);
    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    
    assertThat(key1).isNotEqualTo(key3);
    assertThat(key1).isNotEqualTo(key4);
  }
  
  private Digest createDigest(String hash) {
    return Digest.newBuilder()
        .setHash(hash)
        .setSizeBytes(123)
        .build();
  }
}