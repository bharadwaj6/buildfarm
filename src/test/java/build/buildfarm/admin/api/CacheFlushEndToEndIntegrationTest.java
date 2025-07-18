package build.buildfarm.admin.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.adapter.ac.ActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.ac.InMemoryActionCacheAdapter;
import build.buildfarm.admin.cache.adapter.cas.CASAdapter;
import build.buildfarm.admin.cache.adapter.cas.InMemoryLRUCASAdapter;
import build.buildfarm.admin.cache.concurrency.ConcurrencyConfig;
import build.buildfarm.admin.cache.concurrency.ConcurrencyControlService;
import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import build.buildfarm.admin.cache.ratelimit.RateLimitConfig;
import build.buildfarm.admin.cache.ratelimit.RateLimitService;
import build.buildfarm.admin.cache.service.CacheFlushService;
import build.buildfarm.admin.cache.service.CacheFlushServiceImpl;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * End-to-end integration tests for the Cache Flush API.
 * These tests use real implementations of the adapters with in-memory storage.
 */
@RunWith(JUnit4.class)
public class CacheFlushEndToEndIntegrationTest {

  private CacheFlushResource cacheFlushResource;
  private CacheFlushService cacheFlushService;
  private Map<String, ActionCacheAdapter> actionCacheAdapters;
  private Map<String, CASAdapter> casAdapters;
  private InMemoryActionCacheAdapter inMemoryActionCacheAdapter;
  private InMemoryLRUCASAdapter inMemoryLRUCASAdapter;
  private SecurityContext mockSecurityContext;
  private Principal mockPrincipal;

  @Before
  public void setUp() {
    // Set up real adapters with in-memory storage
    actionCacheAdapters = new HashMap<>();
    casAdapters = new HashMap<>();
    
    // Create real in-memory adapters
    inMemoryActionCacheAdapter = new InMemoryActionCacheAdapter();
    actionCacheAdapters.put("in-memory", inMemoryActionCacheAdapter);
    
    inMemoryLRUCASAdapter = new InMemoryLRUCASAdapter();
    casAdapters.put("in-memory-lru", inMemoryLRUCASAdapter);
    
    // Create real concurrency control service with a test configuration
    ConcurrencyConfig concurrencyConfig = new ConcurrencyConfig(3, 2, 1000, true);
    ConcurrencyControlService concurrencyControlService = new ConcurrencyControlService(concurrencyConfig);
    
    // Create real rate limit service with a test configuration
    RateLimitConfig rateLimitConfig = new RateLimitConfig(5, 60000, true);
    RateLimitService rateLimitService = new RateLimitService(rateLimitConfig);
    
    // Set up security context
    mockPrincipal = mock(Principal.class);
    when(mockPrincipal.getName()).thenReturn("test-user");
    
    mockSecurityContext = mock(SecurityContext.class);
    when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
    
    // Create the service with the real adapters
    cacheFlushService = new CacheFlushServiceImpl(actionCacheAdapters, casAdapters, concurrencyControlService);
    
    // Create the resource with the service and rate limit service
    cacheFlushResource = new CacheFlushResource(cacheFlushService, rateLimitService);
    
    // Populate the in-memory adapters with test data
    populateTestData();
  }
  
  private void populateTestData() {
    // Populate the in-memory Action Cache adapter
    for (int i = 0; i < 50; i++) {
      String key = "action-key-" + i;
      String instanceName = i % 2 == 0 ? "instance-1" : "instance-2";
      String digestPrefix = i % 3 == 0 ? "abc" : (i % 3 == 1 ? "def" : "ghi");
      inMemoryActionCacheAdapter.put(key, instanceName, digestPrefix + UUID.randomUUID().toString(), "test-data");
    }
    
    // Populate the in-memory LRU CAS adapter
    for (int i = 0; i < 50; i++) {
      String key = "cas-key-" + i;
      String instanceName = i % 2 == 0 ? "instance-1" : "instance-2";
      String digestPrefix = i % 3 == 0 ? "abc" : (i % 3 == 1 ? "def" : "ghi");
      byte[] data = new byte[1024]; // 1KB of data
      inMemoryLRUCASAdapter.put(key, instanceName, digestPrefix + UUID.randomUUID().toString(), data);
    }
  }

  @Test
  public void testFlushAllActionCache() {
    // Verify initial state
    assertEquals(50, inMemoryActionCacheAdapter.size());
    
    // Create a request to flush all Action Cache entries
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushInMemory(true);
    
    // Call the API
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    assertEquals(50, flushResponse.getEntriesRemoved());
    assertEquals(1, flushResponse.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(50), flushResponse.getEntriesRemovedByBackend().get("in-memory"));
    
    // Verify that the cache was flushed
    assertEquals(0, inMemoryActionCacheAdapter.size());
  }

  @Test
  public void testFlushInstanceSpecificActionCache() {
    // Verify initial state
    assertEquals(50, inMemoryActionCacheAdapter.size());
    
    // Create a request to flush instance-specific Action Cache entries
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.INSTANCE);
    request.setInstanceName("instance-1");
    request.setFlushInMemory(true);
    
    // Call the API
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    assertEquals(25, flushResponse.getEntriesRemoved());
    assertEquals(1, flushResponse.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(25), flushResponse.getEntriesRemovedByBackend().get("in-memory"));
    
    // Verify that only instance-1 entries were flushed
    assertEquals(25, inMemoryActionCacheAdapter.size());
    for (String key : inMemoryActionCacheAdapter.getKeys()) {
      assertFalse(inMemoryActionCacheAdapter.getInstance(key).equals("instance-1"));
    }
  }

  @Test
  public void testFlushDigestPrefixSpecificActionCache() {
    // Verify initial state
    assertEquals(50, inMemoryActionCacheAdapter.size());
    
    // Create a request to flush digest-prefix-specific Action Cache entries
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.DIGEST_PREFIX);
    request.setDigestPrefix("abc");
    request.setFlushInMemory(true);
    
    // Call the API
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ActionCacheFlushResponse flushResponse = (ActionCacheFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    
    // Approximately 1/3 of entries should have been removed (those with 'abc' prefix)
    int expectedRemoved = 50 / 3;
    int actualRemoved = flushResponse.getEntriesRemoved();
    assertTrue(Math.abs(expectedRemoved - actualRemoved) <= 2); // Allow for small variation due to random distribution
    
    // Verify that only entries with 'abc' prefix were flushed
    for (String key : inMemoryActionCacheAdapter.getKeys()) {
      assertFalse(inMemoryActionCacheAdapter.getDigest(key).startsWith("abc"));
    }
  }

  @Test
  public void testFlushAllCAS() {
    // Verify initial state
    assertEquals(50, inMemoryLRUCASAdapter.size());
    
    // Create a request to flush all CAS entries
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushInMemoryLRU(true);
    
    // Call the API
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    CASFlushResponse flushResponse = (CASFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    assertEquals(50, flushResponse.getEntriesRemoved());
    assertEquals(50 * 1024, flushResponse.getBytesReclaimed());
    assertEquals(1, flushResponse.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(50), flushResponse.getEntriesRemovedByBackend().get("in-memory-lru"));
    
    // Verify that the cache was flushed
    assertEquals(0, inMemoryLRUCASAdapter.size());
  }

  @Test
  public void testFlushInstanceSpecificCAS() {
    // Verify initial state
    assertEquals(50, inMemoryLRUCASAdapter.size());
    
    // Create a request to flush instance-specific CAS entries
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.INSTANCE);
    request.setInstanceName("instance-1");
    request.setFlushInMemoryLRU(true);
    
    // Call the API
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    CASFlushResponse flushResponse = (CASFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    assertEquals(25, flushResponse.getEntriesRemoved());
    assertEquals(25 * 1024, flushResponse.getBytesReclaimed());
    assertEquals(1, flushResponse.getEntriesRemovedByBackend().size());
    assertEquals(Integer.valueOf(25), flushResponse.getEntriesRemovedByBackend().get("in-memory-lru"));
    
    // Verify that only instance-1 entries were flushed
    assertEquals(25, inMemoryLRUCASAdapter.size());
    for (String key : inMemoryLRUCASAdapter.getKeys()) {
      assertFalse(inMemoryLRUCASAdapter.getInstance(key).equals("instance-1"));
    }
  }

  @Test
  public void testFlushDigestPrefixSpecificCAS() {
    // Verify initial state
    assertEquals(50, inMemoryLRUCASAdapter.size());
    
    // Create a request to flush digest-prefix-specific CAS entries
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.DIGEST_PREFIX);
    request.setDigestPrefix("abc");
    request.setFlushInMemoryLRU(true);
    
    // Call the API
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);
    
    // Verify the response
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    CASFlushResponse flushResponse = (CASFlushResponse) response.getEntity();
    assertTrue(flushResponse.isSuccess());
    
    // Approximately 1/3 of entries should have been removed (those with 'abc' prefix)
    int expectedRemoved = 50 / 3;
    int actualRemoved = flushResponse.getEntriesRemoved();
    assertTrue(Math.abs(expectedRemoved - actualRemoved) <= 2); // Allow for small variation due to random distribution
    
    // Verify that only entries with 'abc' prefix were flushed
    for (String key : inMemoryLRUCASAdapter.getKeys()) {
      assertFalse(inMemoryLRUCASAdapter.getDigest(key).startsWith("abc"));
    }
  }

  @Test
  public void testMultipleFlushOperations() {
    // Verify initial state
    assertEquals(50, inMemoryActionCacheAdapter.size());
    assertEquals(50, inMemoryLRUCASAdapter.size());
    
    // First, flush instance-1 entries from Action Cache
    ActionCacheFlushRequest acRequest = new ActionCacheFlushRequest();
    acRequest.setScope(FlushScope.INSTANCE);
    acRequest.setInstanceName("instance-1");
    acRequest.setFlushInMemory(true);
    
    Response acResponse = cacheFlushResource.flushActionCache(acRequest, mockSecurityContext);
    assertEquals(Response.Status.OK.getStatusCode(), acResponse.getStatus());
    assertEquals(25, ((ActionCacheFlushResponse) acResponse.getEntity()).getEntriesRemoved());
    
    // Then, flush digest-prefix 'def' entries from CAS
    CASFlushRequest casRequest = new CASFlushRequest();
    casRequest.setScope(FlushScope.DIGEST_PREFIX);
    casRequest.setDigestPrefix("def");
    casRequest.setFlushInMemoryLRU(true);
    
    Response casResponse = cacheFlushResource.flushCAS(casRequest, mockSecurityContext);
    assertEquals(Response.Status.OK.getStatusCode(), casResponse.getStatus());
    
    // Approximately 1/3 of entries should have been removed (those with 'def' prefix)
    int expectedRemoved = 50 / 3;
    int actualRemoved = ((CASFlushResponse) casResponse.getEntity()).getEntriesRemoved();
    assertTrue(Math.abs(expectedRemoved - actualRemoved) <= 2);
    
    // Finally, flush all remaining entries
    ActionCacheFlushRequest acRequest2 = new ActionCacheFlushRequest();
    acRequest2.setScope(FlushScope.ALL);
    acRequest2.setFlushInMemory(true);
    
    Response acResponse2 = cacheFlushResource.flushActionCache(acRequest2, mockSecurityContext);
    assertEquals(Response.Status.OK.getStatusCode(), acResponse2.getStatus());
    assertEquals(25, ((ActionCacheFlushResponse) acResponse2.getEntity()).getEntriesRemoved());
    
    CASFlushRequest casRequest2 = new CASFlushRequest();
    casRequest2.setScope(FlushScope.ALL);
    casRequest2.setFlushInMemoryLRU(true);
    
    Response casResponse2 = cacheFlushResource.flushCAS(casRequest2, mockSecurityContext);
    assertEquals(Response.Status.OK.getStatusCode(), casResponse2.getStatus());
    
    // Verify that all caches are empty
    assertEquals(0, inMemoryActionCacheAdapter.size());
    assertEquals(0, inMemoryLRUCASAdapter.size());
  }
  
  /**
   * Mock implementation of InMemoryActionCacheAdapter for testing.
   */
  private static class InMemoryActionCacheAdapter implements ActionCacheAdapter {
    private final Map<String, String> instanceMap = new HashMap<>();
    private final Map<String, String> digestMap = new HashMap<>();
    private final Map<String, Object> dataMap = new HashMap<>();
    
    public void put(String key, String instance, String digest, Object data) {
      instanceMap.put(key, instance);
      digestMap.put(key, digest);
      dataMap.put(key, data);
    }
    
    public String getInstance(String key) {
      return instanceMap.get(key);
    }
    
    public String getDigest(String key) {
      return digestMap.get(key);
    }
    
    public int size() {
      return dataMap.size();
    }
    
    public Iterable<String> getKeys() {
      return dataMap.keySet();
    }
    
    @Override
    public FlushResult flushEntries(FlushCriteria criteria) {
      int entriesRemoved = 0;
      
      if (criteria.getScope() == FlushScope.ALL) {
        entriesRemoved = dataMap.size();
        instanceMap.clear();
        digestMap.clear();
        dataMap.clear();
      } else if (criteria.getScope() == FlushScope.INSTANCE) {
        String instanceName = criteria.getInstanceName();
        for (String key : new HashMap<>(instanceMap).keySet()) {
          if (instanceMap.get(key).equals(instanceName)) {
            instanceMap.remove(key);
            digestMap.remove(key);
            dataMap.remove(key);
            entriesRemoved++;
          }
        }
      } else if (criteria.getScope() == FlushScope.DIGEST_PREFIX) {
        String digestPrefix = criteria.getDigestPrefix();
        for (String key : new HashMap<>(digestMap).keySet()) {
          if (digestMap.get(key).startsWith(digestPrefix)) {
            instanceMap.remove(key);
            digestMap.remove(key);
            dataMap.remove(key);
            entriesRemoved++;
          }
        }
      }
      
      return new FlushResult(true, "Flushed " + entriesRemoved + " entries", entriesRemoved, 0);
    }
  }
  
  /**
   * Mock implementation of InMemoryLRUCASAdapter for testing.
   */
  private static class InMemoryLRUCASAdapter implements CASAdapter {
    private final Map<String, String> instanceMap = new HashMap<>();
    private final Map<String, String> digestMap = new HashMap<>();
    private final Map<String, byte[]> dataMap = new HashMap<>();
    
    public void put(String key, String instance, String digest, byte[] data) {
      instanceMap.put(key, instance);
      digestMap.put(key, digest);
      dataMap.put(key, data);
    }
    
    public String getInstance(String key) {
      return instanceMap.get(key);
    }
    
    public String getDigest(String key) {
      return digestMap.get(key);
    }
    
    public int size() {
      return dataMap.size();
    }
    
    public Iterable<String> getKeys() {
      return dataMap.keySet();
    }
    
    @Override
    public FlushResult flushEntries(FlushCriteria criteria) {
      int entriesRemoved = 0;
      long bytesReclaimed = 0;
      
      if (criteria.getScope() == FlushScope.ALL) {
        for (byte[] data : dataMap.values()) {
          bytesReclaimed += data.length;
        }
        entriesRemoved = dataMap.size();
        instanceMap.clear();
        digestMap.clear();
        dataMap.clear();
      } else if (criteria.getScope() == FlushScope.INSTANCE) {
        String instanceName = criteria.getInstanceName();
        for (String key : new HashMap<>(instanceMap).keySet()) {
          if (instanceMap.get(key).equals(instanceName)) {
            bytesReclaimed += dataMap.get(key).length;
            instanceMap.remove(key);
            digestMap.remove(key);
            dataMap.remove(key);
            entriesRemoved++;
          }
        }
      } else if (criteria.getScope() == FlushScope.DIGEST_PREFIX) {
        String digestPrefix = criteria.getDigestPrefix();
        for (String key : new HashMap<>(digestMap).keySet()) {
          if (digestMap.get(key).startsWith(digestPrefix)) {
            bytesReclaimed += dataMap.get(key).length;
            instanceMap.remove(key);
            digestMap.remove(key);
            dataMap.remove(key);
            entriesRemoved++;
          }
        }
      }
      
      return new FlushResult(
          true, "Flushed " + entriesRemoved + " entries (" + bytesReclaimed + " bytes)", 
          entriesRemoved, bytesReclaimed);
    }
  }
}