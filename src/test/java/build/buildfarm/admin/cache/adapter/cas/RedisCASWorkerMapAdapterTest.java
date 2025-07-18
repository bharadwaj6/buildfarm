package build.buildfarm.admin.cache.adapter.cas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/** Tests for {@link RedisCASWorkerMapAdapter}. */
@RunWith(JUnit4.class)
public class RedisCASWorkerMapAdapterTest {

  private UnifiedJedis mockJedis;
  private RedisCASWorkerMapAdapter adapter;
  private static final String MAP_NAME = "test-cas-worker-map";

  @Before
  public void setUp() {
    mockJedis = mock(UnifiedJedis.class);
    adapter = new RedisCASWorkerMapAdapter(mockJedis, MAP_NAME);
  }

  @Test
  public void flushAllEntries_Success() {
    // Setup mock scan results
    ScanResult<String> scanResult1 =
        mockScanResult("1", Arrays.asList(MAP_NAME + ":abc123", MAP_NAME + ":def456"));
    ScanResult<String> scanResult2 = mockScanResult("0", Arrays.asList(MAP_NAME + ":ghi789"));

    when(mockJedis.scan(eq("0"), any(ScanParams.class))).thenReturn(scanResult1);
    when(mockJedis.scan(eq("1"), any(ScanParams.class))).thenReturn(scanResult2);
    when(mockJedis.del(any(String[].class))).thenReturn(3L);

    FlushCriteria criteria = new FlushCriteria(FlushScope.ALL, null, null);
    FlushResult result = adapter.flushEntries(criteria);

    assertTrue(result.isSuccess());
    assertEquals(3, result.getEntriesRemoved());
    assertTrue(result.getBytesReclaimed() > 0);

    // Verify that del was called with the correct keys
    ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
    verify(mockJedis).del(keysCaptor.capture());
    String[] capturedKeys = keysCaptor.getValue();
    assertEquals(3, capturedKeys.length);
    List<String> keysList = Arrays.asList(capturedKeys);
    assertTrue(keysList.contains(MAP_NAME + ":abc123"));
    assertTrue(keysList.contains(MAP_NAME + ":def456"));
    assertTrue(keysList.contains(MAP_NAME + ":ghi789"));
  }

  @Test
  public void flushAllEntries_NoEntries() {
    // Setup mock scan results with no entries
    ScanResult<String> emptyScanResult = mockScanResult("0", Collections.emptyList());
    when(mockJedis.scan(anyString(), any(ScanParams.class))).thenReturn(emptyScanResult);

    FlushCriteria criteria = new FlushCriteria(FlushScope.ALL, null, null);
    FlushResult result = adapter.flushEntries(criteria);

    assertTrue(result.isSuccess());
    assertEquals(0, result.getEntriesRemoved());
    assertEquals(0, result.getBytesReclaimed());

    // Verify that del was not called
    verify(mockJedis, times(0)).del(any(String[].class));
  }

  @Test
  public void flushDigestPrefixEntries_Success() {
    // Setup mock scan results
    ScanResult<String> scanResult1 =
        mockScanResult("1", Arrays.asList(MAP_NAME + ":abc123", MAP_NAME + ":def456"));
    ScanResult<String> scanResult2 = mockScanResult("0", Arrays.asList(MAP_NAME + ":abc789"));

    when(mockJedis.scan(eq("0"), any(ScanParams.class))).thenReturn(scanResult1);
    when(mockJedis.scan(eq("1"), any(ScanParams.class))).thenReturn(scanResult2);
    when(mockJedis.del(any(String[].class))).thenReturn(2L);

    FlushCriteria criteria = new FlushCriteria(FlushScope.DIGEST_PREFIX, null, "abc");
    FlushResult result = adapter.flushEntries(criteria);

    assertTrue(result.isSuccess());
    assertEquals(2, result.getEntriesRemoved());
    assertTrue(result.getBytesReclaimed() > 0);

    // Verify that del was called with the correct keys
    ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
    verify(mockJedis).del(keysCaptor.capture());
    String[] capturedKeys = keysCaptor.getValue();
    assertEquals(2, capturedKeys.length);
    List<String> keysList = Arrays.asList(capturedKeys);
    assertTrue(keysList.contains(MAP_NAME + ":abc123"));
    assertTrue(keysList.contains(MAP_NAME + ":abc789"));
    assertFalse(keysList.contains(MAP_NAME + ":def456"));
  }

  @Test
  public void flushInstanceEntries_NotSupported() {
    FlushCriteria criteria = new FlushCriteria(FlushScope.INSTANCE, "test-instance", null);
    FlushResult result = adapter.flushEntries(criteria);

    assertTrue(result.isSuccess());
    assertEquals(0, result.getEntriesRemoved());
    assertEquals(0, result.getBytesReclaimed());
    assertTrue(result.getMessage().contains("not supported"));

    // Verify that no Redis operations were performed
    verify(mockJedis, times(0)).scan(anyString(), any(ScanParams.class));
    verify(mockJedis, times(0)).del(any(String[].class));
  }

  @Test
  public void flushEntries_UnknownScope() {
    // Create a mock FlushCriteria with an unknown scope
    FlushCriteria criteria = mock(FlushCriteria.class);
    when(criteria.getScope()).thenReturn(null);

    FlushResult result = adapter.flushEntries(criteria);

    assertFalse(result.isSuccess());
    assertEquals(0, result.getEntriesRemoved());
    assertEquals(0, result.getBytesReclaimed());
    assertTrue(result.getMessage().contains("Unknown flush scope"));
  }

  @Test
  public void flushEntries_ExceptionHandling() {
    // Setup mock to throw an exception
    when(mockJedis.scan(anyString(), any(ScanParams.class)))
        .thenThrow(new RuntimeException("Test exception"));

    FlushCriteria criteria = new FlushCriteria(FlushScope.ALL, null, null);
    FlushResult result = adapter.flushEntries(criteria);

    assertFalse(result.isSuccess());
    assertEquals(0, result.getEntriesRemoved());
    assertEquals(0, result.getBytesReclaimed());
    assertTrue(result.getMessage().contains("Failed to flush"));
  }

  /**
   * Helper method to create a mock ScanResult.
   *
   * @param cursor the cursor value
   * @param results the list of results
   * @return a mock ScanResult
   */
  @SuppressWarnings("unchecked")
  private ScanResult<String> mockScanResult(String cursor, List<String> results) {
    ScanResult<String> scanResult = mock(ScanResult.class);
    when(scanResult.getCursor()).thenReturn(cursor);
    when(scanResult.getResult()).thenReturn(results);
    return scanResult;
  }
}
