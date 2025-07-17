package build.buildfarm.admin.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import build.buildfarm.admin.cache.model.ErrorResponse;
import build.buildfarm.admin.cache.model.FlushScope;
import build.buildfarm.admin.cache.service.CacheFlushService;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link CacheFlushResource}.
 */
@RunWith(JUnit4.class)
public class CacheFlushResourceTest {
  private CacheFlushService mockCacheFlushService;
  private CacheFlushResource cacheFlushResource;

  @Before
  public void setUp() {
    mockCacheFlushService = mock(CacheFlushService.class);
    cacheFlushResource = new CacheFlushResource(mockCacheFlushService);
  }

  @Test
  public void flushActionCache_validRequest_returnsOk() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    
    ActionCacheFlushResponse expectedResponse = new ActionCacheFlushResponse();
    expectedResponse.setSuccess(true);
    expectedResponse.setEntriesRemoved(10);
    Map<String, Integer> entriesRemovedByBackend = new HashMap<>();
    entriesRemovedByBackend.put("redis", 10);
    expectedResponse.setEntriesRemovedByBackend(entriesRemovedByBackend);
    
    when(mockCacheFlushService.flushActionCache(any(ActionCacheFlushRequest.class)))
        .thenReturn(expectedResponse);
    
    // Act
    Response response = cacheFlushResource.flushActionCache(request);
    
    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    ActionCacheFlushResponse actualResponse = (ActionCacheFlushResponse) response.getEntity();
    assertNotNull(actualResponse);
    assertEquals(expectedResponse.isSuccess(), actualResponse.isSuccess());
    assertEquals(expectedResponse.getEntriesRemoved(), actualResponse.getEntriesRemoved());
    assertEquals(
        expectedResponse.getEntriesRemovedByBackend(), actualResponse.getEntriesRemovedByBackend());
    verify(mockCacheFlushService).flushActionCache(request);
  }
  
  @Test
  public void flushActionCache_nullRequest_returnsBadRequest() {
    // Act
    Response response = cacheFlushResource.flushActionCache(null);
    
    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
    assertEquals("Request cannot be null", errorResponse.getMessage());
    verify(mockCacheFlushService, never()).flushActionCache(any());
  }
  
  @Test
  public void flushActionCache_nullScope_returnsBadRequest() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(null);
    request.setFlushRedis(true);
    
    // Act
    Response response = cacheFlushResource.flushActionCache(request);
    
    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
    assertEquals("Scope must be specified", errorResponse.getMessage());
    verify(mockCacheFlushService, never()).flushActionCache(any());
  }
  
  @Test
  public void flushActionCache_instanceScopeWithoutInstanceName_returnsBadRequest() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.INSTANCE);
    request.setInstanceName(null);
    request.setFlushRedis(true);
    
    // Act
    Response response = cacheFlushResource.flushActionCache(request);
    
    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
    assertEquals("Instance name must be specified when scope is INSTANCE", errorResponse.getMessage());
    verify(mockCacheFlushService, never()).flushActionCache(any());
  }
  
  @Test
  public void flushActionCache_digestPrefixScopeWithoutDigestPrefix_returnsBadRequest() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.DIGEST_PREFIX);
    request.setDigestPrefix(null);
    request.setFlushRedis(true);
    
    // Act
    Response response = cacheFlushResource.flushActionCache(request);
    
    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
    assertEquals("Digest prefix must be specified when scope is DIGEST_PREFIX", errorResponse.getMessage());
    verify(mockCacheFlushService, never()).flushActionCache(any());
  }
  
  @Test
  public void flushActionCache_noBackendsSelected_returnsBadRequest() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(false);
    request.setFlushInMemory(false);
    
    // Act
    Response response = cacheFlushResource.flushActionCache(request);
    
    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
    assertEquals("At least one backend must be selected for flushing", errorResponse.getMessage());
    verify(mockCacheFlushService, never()).flushActionCache(any());
  }
  
  @Test
  public void flushActionCache_serviceThrowsException_returnsInternalServerError() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    
    when(mockCacheFlushService.flushActionCache(any(ActionCacheFlushRequest.class)))
        .thenThrow(new RuntimeException("Test exception"));
    
    // Act
    Response response = cacheFlushResource.flushActionCache(request);
    
    // Assert
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INTERNAL_ERROR", errorResponse.getErrorCode());
    assertTrue(errorResponse.getMessage().contains("Test exception"));
    verify(mockCacheFlushService).flushActionCache(request);
  }
  
  @Test
  public void flushActionCache_serviceReturnsFailure_returnsInternalServerError() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    
    ActionCacheFlushResponse failureResponse = new ActionCacheFlushResponse();
    failureResponse.setSuccess(false);
    failureResponse.setMessage("Failed to flush Action Cache");
    
    when(mockCacheFlushService.flushActionCache(any(ActionCacheFlushRequest.class)))
        .thenReturn(failureResponse);
    
    // Act
    Response response = cacheFlushResource.flushActionCache(request);
    
    // Assert
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    ActionCacheFlushResponse actualResponse = (ActionCacheFlushResponse) response.getEntity();
    assertNotNull(actualResponse);
    assertEquals(failureResponse.isSuccess(), actualResponse.isSuccess());
    assertEquals(failureResponse.getMessage(), actualResponse.getMessage());
    verify(mockCacheFlushService).flushActionCache(request);
  }
  
  @Test
  public void flushCAS_validRequest_returnsOk() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    
    CASFlushResponse expectedResponse = new CASFlushResponse();
    expectedResponse.setSuccess(true);
    expectedResponse.setEntriesRemoved(10);
    expectedResponse.setBytesReclaimed(1024);
    Map<String, Integer> entriesRemovedByBackend = new HashMap<>();
    entriesRemovedByBackend.put("filesystem", 10);
    expectedResponse.setEntriesRemovedByBackend(entriesRemovedByBackend);
    Map<String, Long> bytesReclaimedByBackend = new HashMap<>();
    bytesReclaimedByBackend.put("filesystem", 1024L);
    expectedResponse.setBytesReclaimedByBackend(bytesReclaimedByBackend);
    
    when(mockCacheFlushService.flushCAS(any(CASFlushRequest.class)))
        .thenReturn(expectedResponse);
    
    // Act
    Response response = cacheFlushResource.flushCAS(request);
    
    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    CASFlushResponse actualResponse = (CASFlushResponse) response.getEntity();
    assertNotNull(actualResponse);
    assertEquals(expectedResponse.isSuccess(), actualResponse.isSuccess());
    assertEquals(expectedResponse.getEntriesRemoved(), actualResponse.getEntriesRemoved());
    assertEquals(expectedResponse.getBytesReclaimed(), actualResponse.getBytesReclaimed());
    assertEquals(
        expectedResponse.getEntriesRemovedByBackend(), actualResponse.getEntriesRemovedByBackend());
    assertEquals(
        expectedResponse.getBytesReclaimedByBackend(), actualResponse.getBytesReclaimedByBackend());
    verify(mockCacheFlushService).flushCAS(request);
  }
  
  @Test
  public void flushCAS_nullRequest_returnsBadRequest() {
    // Act
    Response response = cacheFlushResource.flushCAS(null);
    
    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
    assertEquals("Request cannot be null", errorResponse.getMessage());
    verify(mockCacheFlushService, never()).flushCAS(any());
  }
  
  @Test
  public void flushCAS_nullScope_returnsBadRequest() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(null);
    request.setFlushFilesystem(true);
    
    // Act
    Response response = cacheFlushResource.flushCAS(request);
    
    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
    assertEquals("Scope must be specified", errorResponse.getMessage());
    verify(mockCacheFlushService, never()).flushCAS(any());
  }
  
  @Test
  public void flushCAS_instanceScopeWithoutInstanceName_returnsBadRequest() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.INSTANCE);
    request.setInstanceName(null);
    request.setFlushFilesystem(true);
    
    // Act
    Response response = cacheFlushResource.flushCAS(request);
    
    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
    assertEquals("Instance name must be specified when scope is INSTANCE", errorResponse.getMessage());
    verify(mockCacheFlushService, never()).flushCAS(any());
  }
  
  @Test
  public void flushCAS_digestPrefixScopeWithoutDigestPrefix_returnsBadRequest() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.DIGEST_PREFIX);
    request.setDigestPrefix(null);
    request.setFlushFilesystem(true);
    
    // Act
    Response response = cacheFlushResource.flushCAS(request);
    
    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
    assertEquals("Digest prefix must be specified when scope is DIGEST_PREFIX", errorResponse.getMessage());
    verify(mockCacheFlushService, never()).flushCAS(any());
  }
  
  @Test
  public void flushCAS_noBackendsSelected_returnsBadRequest() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(false);
    request.setFlushInMemoryLRU(false);
    request.setFlushRedisWorkerMap(false);
    
    // Act
    Response response = cacheFlushResource.flushCAS(request);
    
    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INVALID_ARGUMENT", errorResponse.getErrorCode());
    assertEquals("At least one backend must be selected for flushing", errorResponse.getMessage());
    verify(mockCacheFlushService, never()).flushCAS(any());
  }
  
  @Test
  public void flushCAS_serviceThrowsException_returnsInternalServerError() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    
    when(mockCacheFlushService.flushCAS(any(CASFlushRequest.class)))
        .thenThrow(new RuntimeException("Test exception"));
    
    // Act
    Response response = cacheFlushResource.flushCAS(request);
    
    // Assert
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
    assertNotNull(errorResponse);
    assertEquals("INTERNAL_ERROR", errorResponse.getErrorCode());
    assertTrue(errorResponse.getMessage().contains("Test exception"));
    verify(mockCacheFlushService).flushCAS(request);
  }
  
  @Test
  public void flushCAS_serviceReturnsFailure_returnsInternalServerError() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    
    CASFlushResponse failureResponse = new CASFlushResponse();
    failureResponse.setSuccess(false);
    failureResponse.setMessage("Failed to flush CAS");
    
    when(mockCacheFlushService.flushCAS(any(CASFlushRequest.class)))
        .thenReturn(failureResponse);
    
    // Act
    Response response = cacheFlushResource.flushCAS(request);
    
    // Assert
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    CASFlushResponse actualResponse = (CASFlushResponse) response.getEntity();
    assertNotNull(actualResponse);
    assertEquals(failureResponse.isSuccess(), actualResponse.isSuccess());
    assertEquals(failureResponse.getMessage(), actualResponse.getMessage());
    verify(mockCacheFlushService).flushCAS(request);
  }
}