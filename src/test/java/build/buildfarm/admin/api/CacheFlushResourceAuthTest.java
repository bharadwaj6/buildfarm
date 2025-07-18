package build.buildfarm.admin.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.auth.AdminPrincipal;
import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import build.buildfarm.admin.cache.model.FlushScope;
import build.buildfarm.admin.cache.service.CacheFlushService;
import java.security.Principal;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CacheFlushResource} with authentication. */
@RunWith(JUnit4.class)
public class CacheFlushResourceAuthTest {
  private CacheFlushService mockCacheFlushService;
  private CacheFlushResource cacheFlushResource;
  private SecurityContext mockSecurityContext;
  private Principal mockPrincipal;

  @Before
  public void setUp() {
    mockCacheFlushService = mock(CacheFlushService.class);
    cacheFlushResource = new CacheFlushResource(mockCacheFlushService);
    mockSecurityContext = mock(SecurityContext.class);
    mockPrincipal = mock(Principal.class);

    when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
    when(mockPrincipal.getName()).thenReturn("test-admin");
  }

  @Test
  public void flushActionCache_withSecurityContext_logsUserIdentity() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);

    ActionCacheFlushResponse expectedResponse = new ActionCacheFlushResponse();
    expectedResponse.setSuccess(true);
    expectedResponse.setEntriesRemoved(10);

    when(mockCacheFlushService.flushActionCache(any(ActionCacheFlushRequest.class)))
        .thenReturn(expectedResponse);

    // Act
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(mockSecurityContext).getUserPrincipal();
    verify(mockPrincipal).getName();
  }

  @Test
  public void flushActionCache_withAdminPrincipal_logsUserIdentity() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);

    ActionCacheFlushResponse expectedResponse = new ActionCacheFlushResponse();
    expectedResponse.setSuccess(true);
    expectedResponse.setEntriesRemoved(10);

    when(mockCacheFlushService.flushActionCache(any(ActionCacheFlushRequest.class)))
        .thenReturn(expectedResponse);

    // Replace the mock principal with an AdminPrincipal
    AdminPrincipal adminPrincipal = new AdminPrincipal("admin-user", true);
    when(mockSecurityContext.getUserPrincipal()).thenReturn(adminPrincipal);

    // Act
    Response response = cacheFlushResource.flushActionCache(request, mockSecurityContext);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(mockSecurityContext).getUserPrincipal();
  }

  @Test
  public void flushActionCache_withNullSecurityContext_usesUnknownUser() {
    // Arrange
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);

    ActionCacheFlushResponse expectedResponse = new ActionCacheFlushResponse();
    expectedResponse.setSuccess(true);
    expectedResponse.setEntriesRemoved(10);

    when(mockCacheFlushService.flushActionCache(any(ActionCacheFlushRequest.class)))
        .thenReturn(expectedResponse);

    // Act
    Response response = cacheFlushResource.flushActionCache(request, null);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void flushCAS_withSecurityContext_logsUserIdentity() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);

    CASFlushResponse expectedResponse = new CASFlushResponse();
    expectedResponse.setSuccess(true);
    expectedResponse.setEntriesRemoved(10);
    expectedResponse.setBytesReclaimed(1024);

    when(mockCacheFlushService.flushCAS(any(CASFlushRequest.class))).thenReturn(expectedResponse);

    // Act
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(mockSecurityContext).getUserPrincipal();
    verify(mockPrincipal).getName();
  }

  @Test
  public void flushCAS_withAdminPrincipal_logsUserIdentity() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);

    CASFlushResponse expectedResponse = new CASFlushResponse();
    expectedResponse.setSuccess(true);
    expectedResponse.setEntriesRemoved(10);
    expectedResponse.setBytesReclaimed(1024);

    when(mockCacheFlushService.flushCAS(any(CASFlushRequest.class))).thenReturn(expectedResponse);

    // Replace the mock principal with an AdminPrincipal
    AdminPrincipal adminPrincipal = new AdminPrincipal("admin-user", true);
    when(mockSecurityContext.getUserPrincipal()).thenReturn(adminPrincipal);

    // Act
    Response response = cacheFlushResource.flushCAS(request, mockSecurityContext);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(mockSecurityContext).getUserPrincipal();
  }

  @Test
  public void flushCAS_withNullSecurityContext_usesUnknownUser() {
    // Arrange
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);

    CASFlushResponse expectedResponse = new CASFlushResponse();
    expectedResponse.setSuccess(true);
    expectedResponse.setEntriesRemoved(10);
    expectedResponse.setBytesReclaimed(1024);

    when(mockCacheFlushService.flushCAS(any(CASFlushRequest.class))).thenReturn(expectedResponse);

    // Act
    Response response = cacheFlushResource.flushCAS(request, null);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }
}
