package build.buildfarm.admin.cache.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.adapter.ac.ActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.cas.CASAdapter;
import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

/**
 * Tests for {@link CacheFlushServiceImpl}.
 */
@RunWith(JUnit4.class)
public class CacheFlushServiceImplTest {
  
  private CacheFlushServiceImpl service;
  private ActionCacheAdapter mockRedisActionCacheAdapter;
  private ActionCacheAdapter mockInMemoryActionCacheAdapter;
  private CASAdapter mockFilesystemCASAdapter;
  private CASAdapter mockInMemoryLRUCASAdapter;
  private CASAdapter mockRedisCASWorkerMapAdapter;
  
  @Before
  public void setUp() {
    // Create mock adapters
    mockRedisActionCacheAdapter = mock(ActionCacheAdapter.class);
    mockInMemoryActionCacheAdapter = mock(ActionCacheAdapter.class);
    mockFilesystemCASAdapter = mock(CASAdapter.class);
    mockInMemoryLRUCASAdapter = mock(CASAdapter.class);
    mockRedisCASWorkerMapAdapter = mock(CASAdapter.class);
    
    // Set up mock responses
    when(mockRedisActionCacheAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Redis AC flushed", 10, 0));
    when(mockInMemoryActionCacheAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "In-memory AC flushed", 5, 0));
    when(mockFilesystemCASAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Filesystem CAS flushed", 20, 1024));
    when(mockInMemoryLRUCASAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "In-memory LRU CAS flushed", 8, 512));
    when(mockRedisCASWorkerMapAdapter.flushEntries(any(FlushCriteria.class)))
        .thenReturn(new FlushResult(true, "Redis CAS worker map flushed", 15, 0));
    
    // Create adapter maps
    Map<String, ActionCacheAdapter> actionCacheAdapters = new HashMap<>();
    actionCacheAdapters.put("redis", mockRedisActionCacheAdapter);
    actionCacheAdapters.put("in-memory", mockInMemoryActionCacheAdapter);
    
    Map<String, CASAdapter> casAdapters = new HashMap<>();
    casAdapters.put("filesystem", mockFilesystemCASAdapter);
    casAdapters.put("in-memory-lru", mockInMemoryLRUCASAdapter);
    casAdapters.put("redis-worker-map", mockRedisCASWorkerMapAdapter);
    
    // Create the service
    service = new CacheFlushServiceImpl(actionCacheAdapters, casAdapters);
  }
  
  @Test
  public void flushActionCache_flushAll_success() {
    // Create a request to flush all Action Cache entries
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // Flush the Action Cache
    ActionCacheFlushResponse response = service.flushActionCache(request);
    
    // Verify the response
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(15, response.getEntriesRemoved());
    assertEquals(2, response.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(10), response.getEntriesRemovedByBackend().get("redis"));
    assertEquals(Integer.valueOf(5), response.getEntriesRemovedByBackend().get("in-memory"));
    
    // Verify the adapters were called with the correct criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockRedisActionCacheAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockInMemoryActionCacheAdapter).flushEntries(criteriaCaptor.capture());
    
    // Verify the criteria
    for (FlushCriteria criteria : criteriaCaptor.getAllValues()) {
      assertEquals(FlushScope.ALL, criteria.getScope());
      assertEquals(null, criteria.getInstanceName());
      assertEquals(null, criteria.getDigestPrefix());
    }
  }
  
  @Test
  public void flushActionCache_flushRedisOnly_success() {
    // Create a request to flush only Redis Action Cache entries
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    request.setFlushInMemory(false);
    
    // Flush the Action Cache
    ActionCacheFlushResponse response = service.flushActionCache(request);
    
    // Verify the response
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(10, response.getEntriesRemoved());
    assertEquals(1, response.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(10), response.getEntriesRemovedByBackend().get("redis"));
    
    // Verify the adapters were called with the correct criteria
    verify(mockRedisActionCacheAdapter).flushEntries(any(FlushCriteria.class));
    verify(mockInMemoryActionCacheAdapter, never()).flushEntries(any(FlushCriteria.class));
  }
  
  @Test
  public void flushActionCache_flushInMemoryOnly_success() {
    // Create a request to flush only in-memory Action Cache entries
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(false);
    request.setFlushInMemory(true);
    
    // Flush the Action Cache
    ActionCacheFlushResponse response = service.flushActionCache(request);
    
    // Verify the response
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(5, response.getEntriesRemoved());
    assertEquals(1, response.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(5), response.getEntriesRemovedByBackend().get("in-memory"));
    
    // Verify the adapters were called with the correct criteria
    verify(mockRedisActionCacheAdapter, never()).flushEntries(any(FlushCriteria.class));
    verify(mockInMemoryActionCacheAdapter).flushEntries(any(FlushCriteria.class));
  }
  
  @Test
  public void flushActionCache_instanceScope_success() {
    // Create a request to flush Action Cache entries for a specific instance
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.INSTANCE);
    request.setInstanceName("test-instance");
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // Flush the Action Cache
    ActionCacheFlushResponse response = service.flushActionCache(request);
    
    // Verify the response
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(15, response.getEntriesRemoved());
    
    // Verify the adapters were called with the correct criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockRedisActionCacheAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockInMemoryActionCacheAdapter).flushEntries(criteriaCaptor.capture());
    
    // Verify the criteria
    for (FlushCriteria criteria : criteriaCaptor.getAllValues()) {
      assertEquals(FlushScope.INSTANCE, criteria.getScope());
      assertEquals("test-instance", criteria.getInstanceName());
      assertEquals(null, criteria.getDigestPrefix());
    }
  }
  
  @Test
  public void flushActionCache_digestPrefixScope_success() {
    // Create a request to flush Action Cache entries with a specific digest prefix
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.DIGEST_PREFIX);
    request.setDigestPrefix("abc123");
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // Flush the Action Cache
    ActionCacheFlushResponse response = service.flushActionCache(request);
    
    // Verify the response
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(15, response.getEntriesRemoved());
    
    // Verify the adapters were called with the correct criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockRedisActionCacheAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockInMemoryActionCacheAdapter).flushEntries(criteriaCaptor.capture());
    
    // Verify the criteria
    for (FlushCriteria criteria : criteriaCaptor.getAllValues()) {
      assertEquals(FlushScope.DIGEST_PREFIX, criteria.getScope());
      assertEquals(null, criteria.getInstanceName());
      assertEquals("abc123", criteria.getDigestPrefix());
    }
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void flushActionCache_missingScope_throwsException() {
    // Create a request with a missing scope
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // This should throw an IllegalArgumentException
    service.flushActionCache(request);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void flushActionCache_instanceScopeWithoutInstanceName_throwsException() {
    // Create a request with INSTANCE scope but no instance name
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.INSTANCE);
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // This should throw an IllegalArgumentException
    service.flushActionCache(request);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void flushActionCache_digestPrefixScopeWithoutDigestPrefix_throwsException() {
    // Create a request with DIGEST_PREFIX scope but no digest prefix
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.DIGEST_PREFIX);
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // This should throw an IllegalArgumentException
    service.flushActionCache(request);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void flushActionCache_noBackendsSelected_throwsException() {
    // Create a request with no backends selected
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(false);
    request.setFlushInMemory(false);
    
    // This should throw an IllegalArgumentException
    service.flushActionCache(request);
  }
  
  @Test
  public void flushCAS_flushAll_success() {
    // Create a request to flush all CAS entries
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    request.setFlushRedisWorkerMap(true);
    
    // Flush the CAS
    CASFlushResponse response = service.flushCAS(request);
    
    // Verify the response
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(43, response.getEntriesRemoved());
    assertEquals(1536, response.getBytesReclaimed());
    assertEquals(3, response.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(20), response.getEntriesRemovedByBackend().get("filesystem"));
    assertEquals(Integer.valueOf(8), response.getEntriesRemovedByBackend().get("in-memory-lru"));
    assertEquals(Integer.valueOf(15), response.getEntriesRemovedByBackend().get("redis-worker-map"));
    assertEquals(3, response.getBytesReclaimedByBackend().size());
    assertEquals(Long.valueOf(1024), response.getBytesReclaimedByBackend().get("filesystem"));
    assertEquals(Long.valueOf(512), response.getBytesReclaimedByBackend().get("in-memory-lru"));
    assertEquals(Long.valueOf(0), response.getBytesReclaimedByBackend().get("redis-worker-map"));
    
    // Verify the adapters were called with the correct criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockFilesystemCASAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockInMemoryLRUCASAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockRedisCASWorkerMapAdapter).flushEntries(criteriaCaptor.capture());
    
    // Verify the criteria
    for (FlushCriteria criteria : criteriaCaptor.getAllValues()) {
      assertEquals(FlushScope.ALL, criteria.getScope());
      assertEquals(null, criteria.getInstanceName());
      assertEquals(null, criteria.getDigestPrefix());
    }
  }
  
  @Test
  public void flushCAS_flushFilesystemOnly_success() {
    // Create a request to flush only filesystem CAS entries
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(false);
    request.setFlushRedisWorkerMap(false);
    
    // Flush the CAS
    CASFlushResponse response = service.flushCAS(request);
    
    // Verify the response
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(20, response.getEntriesRemoved());
    assertEquals(1024, response.getBytesReclaimed());
    assertEquals(1, response.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(20), response.getEntriesRemovedByBackend().get("filesystem"));
    assertEquals(1, response.getBytesReclaimedByBackend().size());
    assertEquals(Long.valueOf(1024), response.getBytesReclaimedByBackend().get("filesystem"));
    
    // Verify the adapters were called with the correct criteria
    verify(mockFilesystemCASAdapter).flushEntries(any(FlushCriteria.class));
    verify(mockInMemoryLRUCASAdapter, never()).flushEntries(any(FlushCriteria.class));
    verify(mockRedisCASWorkerMapAdapter, never()).flushEntries(any(FlushCriteria.class));
  }
  
  @Test
  public void flushCAS_instanceScope_success() {
    // Create a request to flush CAS entries for a specific instance
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.INSTANCE);
    request.setInstanceName("test-instance");
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    request.setFlushRedisWorkerMap(true);
    
    // Flush the CAS
    CASFlushResponse response = service.flushCAS(request);
    
    // Verify the response
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(43, response.getEntriesRemoved());
    assertEquals(1536, response.getBytesReclaimed());
    
    // Verify the adapters were called with the correct criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockFilesystemCASAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockInMemoryLRUCASAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockRedisCASWorkerMapAdapter).flushEntries(criteriaCaptor.capture());
    
    // Verify the criteria
    for (FlushCriteria criteria : criteriaCaptor.getAllValues()) {
      assertEquals(FlushScope.INSTANCE, criteria.getScope());
      assertEquals("test-instance", criteria.getInstanceName());
      assertEquals(null, criteria.getDigestPrefix());
    }
  }
  
  @Test
  public void flushCAS_digestPrefixScope_success() {
    // Create a request to flush CAS entries with a specific digest prefix
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.DIGEST_PREFIX);
    request.setDigestPrefix("abc123");
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    request.setFlushRedisWorkerMap(true);
    
    // Flush the CAS
    CASFlushResponse response = service.flushCAS(request);
    
    // Verify the response
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(43, response.getEntriesRemoved());
    assertEquals(1536, response.getBytesReclaimed());
    
    // Verify the adapters were called with the correct criteria
    ArgumentCaptor<FlushCriteria> criteriaCaptor = ArgumentCaptor.forClass(FlushCriteria.class);
    verify(mockFilesystemCASAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockInMemoryLRUCASAdapter).flushEntries(criteriaCaptor.capture());
    verify(mockRedisCASWorkerMapAdapter).flushEntries(criteriaCaptor.capture());
    
    // Verify the criteria
    for (FlushCriteria criteria : criteriaCaptor.getAllValues()) {
      assertEquals(FlushScope.DIGEST_PREFIX, criteria.getScope());
      assertEquals(null, criteria.getInstanceName());
      assertEquals("abc123", criteria.getDigestPrefix());
    }
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void flushCAS_missingScope_throwsException() {
    // Create a request with a missing scope
    CASFlushRequest request = new CASFlushRequest();
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    request.setFlushRedisWorkerMap(true);
    
    // This should throw an IllegalArgumentException
    service.flushCAS(request);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void flushCAS_instanceScopeWithoutInstanceName_throwsException() {
    // Create a request with INSTANCE scope but no instance name
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.INSTANCE);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    request.setFlushRedisWorkerMap(true);
    
    // This should throw an IllegalArgumentException
    service.flushCAS(request);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void flushCAS_digestPrefixScopeWithoutDigestPrefix_throwsException() {
    // Create a request with DIGEST_PREFIX scope but no digest prefix
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.DIGEST_PREFIX);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    request.setFlushRedisWorkerMap(true);
    
    // This should throw an IllegalArgumentException
    service.flushCAS(request);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void flushCAS_noBackendsSelected_throwsException() {
    // Create a request with no backends selected
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(false);
    request.setFlushInMemoryLRU(false);
    request.setFlushRedisWorkerMap(false);
    
    // This should throw an IllegalArgumentException
    service.flushCAS(request);
  }
  
  @Test
  public void flushActionCache_adapterThrowsException_partialSuccess() {
    // Set up the Redis adapter to throw an exception
    when(mockRedisActionCacheAdapter.flushEntries(any(FlushCriteria.class)))
        .thenThrow(new RuntimeException("Redis connection error"));
    
    // Create a request to flush all Action Cache entries
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // Flush the Action Cache
    ActionCacheFlushResponse response = service.flushActionCache(request);
    
    // Verify the response
    assertNotNull(response);
    assertFalse(response.isSuccess());
    assertTrue(response.getMessage().contains("Error flushing Action Cache"));
    assertEquals(5, response.getEntriesRemoved());
    assertEquals(1, response.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(5), response.getEntriesRemovedByBackend().get("in-memory"));
  }
  
  @Test
  public void flushCAS_adapterThrowsException_partialSuccess() {
    // Set up the filesystem adapter to throw an exception
    when(mockFilesystemCASAdapter.flushEntries(any(FlushCriteria.class)))
        .thenThrow(new RuntimeException("Filesystem error"));
    
    // Create a request to flush all CAS entries
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    request.setFlushRedisWorkerMap(true);
    
    // Flush the CAS
    CASFlushResponse response = service.flushCAS(request);
    
    // Verify the response
    assertNotNull(response);
    assertFalse(response.isSuccess());
    assertTrue(response.getMessage().contains("Error flushing CAS"));
    assertEquals(23, response.getEntriesRemoved());
    assertEquals(512, response.getBytesReclaimed());
    assertEquals(2, response.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(8), response.getEntriesRemovedByBackend().get("in-memory-lru"));
    assertEquals(Integer.valueOf(15), response.getEntriesRemovedByBackend().get("redis-worker-map"));
    assertEquals(2, response.getBytesReclaimedByBackend().size());
    assertEquals(Long.valueOf(512), response.getBytesReclaimedByBackend().get("in-memory-lru"));
    assertEquals(Long.valueOf(0), response.getBytesReclaimedByBackend().get("redis-worker-map"));
  }
  
  @Test
  public void flushActionCache_multipleRequests_activeOperationsTracked() {
    // Create a request to flush all Action Cache entries
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    request.setFlushInMemory(true);
    
    // Flush the Action Cache multiple times
    service.flushActionCache(request);
    service.flushActionCache(request);
    service.flushActionCache(request);
    
    // Verify the active operations count
    assertEquals(0, service.getActiveFlushOperations("action-cache"));
  }
  
  @Test
  public void flushCAS_multipleRequests_activeOperationsTracked() {
    // Create a request to flush all CAS entries
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    request.setFlushRedisWorkerMap(true);
    
    // Flush the CAS multiple times
    service.flushCAS(request);
    service.flushCAS(request);
    service.flushCAS(request);
    
    // Verify the active operations count
    assertEquals(0, service.getActiveFlushOperations("cas"));
  }
}