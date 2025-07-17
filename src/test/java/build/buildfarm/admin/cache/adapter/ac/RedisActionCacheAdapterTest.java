package build.buildfarm.admin.cache.adapter.ac;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

/**
 * Tests for {@link RedisActionCacheAdapter}.
 */
@RunWith(JUnit4.class)
public class RedisActionCacheAdapterTest {
  
  private JedisPool mockJedisPool;
  private Jedis mockJedis;
  private RedisActionCacheAdapter adapter;
  
  @Before
  public void setUp() {
    mockJedisPool = mock(JedisPool.class);
    mockJedis = mock(Jedis.class);
    when(mockJedisPool.getResource()).thenReturn(mockJedis);
    
    adapter = new RedisActionCacheAdapter(mockJedisPool);
  }
  
  @Test
  public void testFlushAllEntries() {
    // Setup
    Set<String> keys = new HashSet<>(Arrays.asList("ac:key1", "ac:key2", "ac:key3"));
    mockScanResult(keys);
    
    // Execute
    FlushCriteria criteria = new FlushCriteria(FlushScope.ALL, null, null);
    FlushResult result = adapter.flushEntries(criteria);
    
    // Verify
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(3);
    
    ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
    verify(mockJedis).del(keysCaptor.capture());
    assertThat(keysCaptor.getValue()).asList().containsExactlyElementsIn(keys);
    
    // Verify scan pattern
    ArgumentCaptor<ScanParams> scanParamsCaptor = ArgumentCaptor.forClass(ScanParams.class);
    verify(mockJedis).scan(eq("0"), scanParamsCaptor.capture());
    assertThat(scanParamsCaptor.getValue().getMatch()).isEqualTo("ac:*");
  }
  
  @Test
  public void testFlushInstanceEntries() {
    // Setup
    Set<String> keys = new HashSet<>(Arrays.asList("ac:instance1:key1", "ac:instance1:key2"));
    mockScanResult(keys);
    
    // Execute
    FlushCriteria criteria = new FlushCriteria(FlushScope.INSTANCE, "instance1", null);
    FlushResult result = adapter.flushEntries(criteria);
    
    // Verify
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(2);
    
    ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
    verify(mockJedis).del(keysCaptor.capture());
    assertThat(keysCaptor.getValue()).asList().containsExactlyElementsIn(keys);
    
    // Verify scan pattern
    ArgumentCaptor<ScanParams> scanParamsCaptor = ArgumentCaptor.forClass(ScanParams.class);
    verify(mockJedis).scan(eq("0"), scanParamsCaptor.capture());
    assertThat(scanParamsCaptor.getValue().getMatch()).isEqualTo("ac:instance1:*");
  }
  
  @Test
  public void testFlushDigestPrefixEntries() {
    // Setup
    Set<String> keys = new HashSet<>(Arrays.asList("ac:instance1:abc123", "ac:instance2:abc456"));
    mockScanResult(keys);
    
    // Execute
    FlushCriteria criteria = new FlushCriteria(FlushScope.DIGEST_PREFIX, null, "abc");
    FlushResult result = adapter.flushEntries(criteria);
    
    // Verify
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(2);
    
    ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
    verify(mockJedis).del(keysCaptor.capture());
    assertThat(keysCaptor.getValue()).asList().containsExactlyElementsIn(keys);
    
    // Verify scan pattern
    ArgumentCaptor<ScanParams> scanParamsCaptor = ArgumentCaptor.forClass(ScanParams.class);
    verify(mockJedis).scan(eq("0"), scanParamsCaptor.capture());
    assertThat(scanParamsCaptor.getValue().getMatch()).isEqualTo("ac:*:abc*");
  }
  
  @Test
  public void testFlushEntriesWithNoKeys() {
    // Setup
    mockScanResult(new HashSet<>());
    
    // Execute
    FlushCriteria criteria = new FlushCriteria(FlushScope.ALL, null, null);
    FlushResult result = adapter.flushEntries(criteria);
    
    // Verify
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(0);
  }
  
  @Test
  public void testFlushEntriesWithException() {
    // Setup
    when(mockJedis.scan(anyString(), any(ScanParams.class))).thenThrow(new RuntimeException("Redis error"));
    
    // Execute
    FlushCriteria criteria = new FlushCriteria(FlushScope.ALL, null, null);
    FlushResult result = adapter.flushEntries(criteria);
    
    // Verify
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getMessage()).contains("Redis error");
    assertThat(result.getEntriesRemoved()).isEqualTo(0);
  }
  
  private void mockScanResult(Set<String> keys) {
    @SuppressWarnings("unchecked")
    ScanResult<String> scanResult = mock(ScanResult.class);
    when(scanResult.getResult()).thenReturn(keys);
    when(scanResult.getCursor()).thenReturn("0");
    when(mockJedis.scan(anyString(), any(ScanParams.class))).thenReturn(scanResult);
  }
}