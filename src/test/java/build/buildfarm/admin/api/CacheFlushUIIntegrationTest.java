package build.buildfarm.admin.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import build.buildfarm.admin.cache.model.FlushScope;
import build.buildfarm.admin.cache.service.CacheFlushService;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for the Cache Flush UI components. */
@RunWith(JUnit4.class)
public class CacheFlushUIIntegrationTest {

  private AdminUIResource adminUIResource;
  private StaticResourceProvider staticResourceProvider;
  private CacheFlushResource cacheFlushResource;
  private CacheFlushService mockCacheFlushService;
  private SecurityContext mockSecurityContext;

  @Before
  public void setUp() {
    adminUIResource = new AdminUIResource();
    staticResourceProvider = new StaticResourceProvider();
    mockCacheFlushService = mock(CacheFlushService.class);
    cacheFlushResource = new CacheFlushResource(mockCacheFlushService);
    mockSecurityContext = mock(SecurityContext.class);
  }

  @Test
  public void testUIResourcesAvailable() {
    // Test HTML page
    Response htmlResponse = adminUIResource.getCacheFlushUI();
    assertEquals(Response.Status.OK.getStatusCode(), htmlResponse.getStatus());
    assertTrue(htmlResponse.getEntity() instanceof InputStream);

    // Test CSS resource
    Response cssResponse = staticResourceProvider.getCssResource("cache-flush.css");
    assertEquals(Response.Status.OK.getStatusCode(), cssResponse.getStatus());
    assertTrue(cssResponse.getEntity() instanceof InputStream);

    // Test JS resource
    Response jsResponse = staticResourceProvider.getJsResource("cache-flush.js");
    assertEquals(Response.Status.OK.getStatusCode(), jsResponse.getStatus());
    assertTrue(jsResponse.getEntity() instanceof InputStream);
  }

  @Test
  public void testActionCacheFlushEndpoint() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    request.setFlushInMemory(true);

    ActionCacheFlushResponse mockResponse = new ActionCacheFlushResponse();
    mockResponse.setSuccess(true);
    mockResponse.setEntriesRemoved(100);
    Map<String, Integer> entriesRemovedByBackend = new HashMap<>();
    entriesRemovedByBackend.put("Redis", 60);
    entriesRemovedByBackend.put("InMemory", 40);
    mockResponse.setEntriesRemovedByBackend(entriesRemovedByBackend);

    when(mockCacheFlushService.flushActionCache(request)).thenReturn(mockResponse);

    // Act
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof ActionCacheFlushResponse);
    ActionCacheFlushResponse actualResponse = (ActionCacheFlushResponse) response.getEntity();
    assertEquals(true, actualResponse.isSuccess());
    assertEquals(100, actualResponse.getEntriesRemoved());
    assertEquals(2, actualResponse.getEntriesRemovedByBackend().size());
  }

  @Test
  public void testCASFlushEndpoint() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(true);
    request.setFlushRedisWorkerMap(true);

    CASFlushResponse mockResponse = new CASFlushResponse();
    mockResponse.setSuccess(true);
    mockResponse.setEntriesRemoved(200);
    mockResponse.setBytesReclaimed(1024 * 1024 * 50); // 50 MB
    Map<String, Integer> entriesRemovedByBackend = new HashMap<>();
    entriesRemovedByBackend.put("Filesystem", 100);
    entriesRemovedByBackend.put("InMemoryLRU", 50);
    entriesRemovedByBackend.put("RedisWorkerMap", 50);
    mockResponse.setEntriesRemovedByBackend(entriesRemovedByBackend);
    Map<String, Long> bytesReclaimedByBackend = new HashMap<>();
    bytesReclaimedByBackend.put("Filesystem", 1024L * 1024L * 40L); // 40 MB
    bytesReclaimedByBackend.put("InMemoryLRU", 1024L * 1024L * 10L); // 10 MB
    mockResponse.setBytesReclaimedByBackend(bytesReclaimedByBackend);

    when(mockCacheFlushService.flushCAS(request)).thenReturn(mockResponse);

    // Act
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof CASFlushResponse);
    CASFlushResponse actualResponse = (CASFlushResponse) response.getEntity();
    assertEquals(true, actualResponse.isSuccess());
    assertEquals(200, actualResponse.getEntriesRemoved());
    assertEquals(1024 * 1024 * 50, actualResponse.getBytesReclaimed());
    assertEquals(3, actualResponse.getEntriesRemovedByBackend().size());
    assertEquals(2, actualResponse.getBytesReclaimedByBackend().size());
  }
}
