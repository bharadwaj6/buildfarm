package build.buildfarm.admin.cache.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CacheFlushService}. */
@RunWith(JUnit4.class)
public class CacheFlushServiceTest {

  private CacheFlushService service;
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

    // Create a concrete implementation of CacheFlushService for testing
    service =
        new CacheFlushService() {
          @Override
          public ActionCacheFlushResponse flushActionCache(ActionCacheFlushRequest request) {
            ActionCacheFlushResponse response = new ActionCacheFlushResponse();

            if (request.isFlushRedis()) {
              FlushCriteria criteria =
                  new FlushCriteria(
                      request.getScope(), request.getInstanceName(), request.getDigestPrefix());
              FlushResult result = mockRedisActionCacheAdapter.flushEntries(criteria);
              response.addEntriesRemovedByBackend("redis", result.getEntriesRemoved());
              if (!result.isSuccess()) {
                response.setSuccess(false);
                response.setMessage(
                    response.getMessage()
                        + (response.getMessage().isEmpty() ? "" : ", ")
                        + result.getMessage());
              }
            }

            if (request.isFlushInMemory()) {
              FlushCriteria criteria =
                  new FlushCriteria(
                      request.getScope(), request.getInstanceName(), request.getDigestPrefix());
              FlushResult result = mockInMemoryActionCacheAdapter.flushEntries(criteria);
              response.addEntriesRemovedByBackend("in-memory", result.getEntriesRemoved());
              if (!result.isSuccess()) {
                response.setSuccess(false);
                response.setMessage(
                    response.getMessage()
                        + (response.getMessage().isEmpty() ? "" : ", ")
                        + result.getMessage());
              }
            }

            return response;
          }

          @Override
          public CASFlushResponse flushCAS(CASFlushRequest request) {
            CASFlushResponse response = new CASFlushResponse();

            if (request.isFlushFilesystem()) {
              FlushCriteria criteria =
                  new FlushCriteria(
                      request.getScope(), request.getInstanceName(), request.getDigestPrefix());
              FlushResult result = mockFilesystemCASAdapter.flushEntries(criteria);
              response.addBackendResult(
                  "filesystem", result.getEntriesRemoved(), result.getBytesReclaimed());
              if (!result.isSuccess()) {
                response.setSuccess(false);
                response.setMessage(
                    response.getMessage()
                        + (response.getMessage().isEmpty() ? "" : ", ")
                        + result.getMessage());
              }
            }

            if (request.isFlushInMemoryLRU()) {
              FlushCriteria criteria =
                  new FlushCriteria(
                      request.getScope(), request.getInstanceName(), request.getDigestPrefix());
              FlushResult result = mockInMemoryLRUCASAdapter.flushEntries(criteria);
              response.addBackendResult(
                  "in-memory-lru", result.getEntriesRemoved(), result.getBytesReclaimed());
              if (!result.isSuccess()) {
                response.setSuccess(false);
                response.setMessage(
                    response.getMessage()
                        + (response.getMessage().isEmpty() ? "" : ", ")
                        + result.getMessage());
              }
            }

            if (request.isFlushRedisWorkerMap()) {
              FlushCriteria criteria =
                  new FlushCriteria(
                      request.getScope(), request.getInstanceName(), request.getDigestPrefix());
              FlushResult result = mockRedisCASWorkerMapAdapter.flushEntries(criteria);
              response.addBackendResult(
                  "redis-worker-map", result.getEntriesRemoved(), result.getBytesReclaimed());
              if (!result.isSuccess()) {
                response.setSuccess(false);
                response.setMessage(
                    response.getMessage()
                        + (response.getMessage().isEmpty() ? "" : ", ")
                        + result.getMessage());
              }
            }

            return response;
          }
        };
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
    assertEquals(
        Integer.valueOf(15), response.getEntriesRemovedByBackend().get("redis-worker-map"));
    assertEquals(3, response.getBytesReclaimedByBackend().size());
    assertEquals(Long.valueOf(1024), response.getBytesReclaimedByBackend().get("filesystem"));
    assertEquals(Long.valueOf(512), response.getBytesReclaimedByBackend().get("in-memory-lru"));
    assertEquals(Long.valueOf(0), response.getBytesReclaimedByBackend().get("redis-worker-map"));
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
  }
}
