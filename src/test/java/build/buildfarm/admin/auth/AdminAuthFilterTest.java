package build.buildfarm.admin.auth;

import static org.mockito.Mockito.*;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class AdminAuthFilterTest {

  @Mock private ContainerRequestContext requestContext;
  @Mock private UriInfo uriInfo;
  @Mock private SecurityContext securityContext;
  
  private AuthConfig authConfig;
  private AdminAuthFilter filter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    
    authConfig = new AuthConfig();
    authConfig.setAuthEnabled(true);
    authConfig.setAdminTokenHeader("X-Admin-Token");
    authConfig.setAdminUsernameHeader("X-Admin-Username");
    authConfig.setAdminToken("test-admin-token");
    
    filter = new AdminAuthFilter(authConfig);
    
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getSecurityContext()).thenReturn(securityContext);
  }

  @Test
  public void testFilterSkipsNonAdminPaths() throws IOException {
    // Setup
    when(uriInfo.getPath()).thenReturn("api/v1/some/path");
    
    // Execute
    filter.filter(requestContext);
    
    // Verify
    verify(requestContext, never()).abortWith(any(Response.class));
    verify(requestContext, never()).setSecurityContext(any(SecurityContext.class));
  }

  @Test
  public void testFilterSkipsWhenAuthDisabled() throws IOException {
    // Setup
    when(uriInfo.getPath()).thenReturn("admin/v1/cache/action/flush");
    authConfig.setAuthEnabled(false);
    
    // Execute
    filter.filter(requestContext);
    
    // Verify
    verify(requestContext, never()).abortWith(any(Response.class));
    verify(requestContext, never()).setSecurityContext(any(SecurityContext.class));
  }

  @Test
  public void testFilterRejectsInvalidToken() throws IOException {
    // Setup
    when(uriInfo.getPath()).thenReturn("admin/v1/cache/action/flush");
    when(requestContext.getHeaderString("X-Admin-Token")).thenReturn("invalid-token");
    when(requestContext.getHeaderString("X-Admin-Username")).thenReturn("test-user");
    
    // Execute
    filter.filter(requestContext);
    
    // Verify
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(responseCaptor.capture());
    
    Response response = responseCaptor.getValue();
    assert response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode();
  }

  @Test
  public void testFilterRejectsMissingUsername() throws IOException {
    // Setup
    when(uriInfo.getPath()).thenReturn("admin/v1/cache/action/flush");
    when(requestContext.getHeaderString("X-Admin-Token")).thenReturn("test-admin-token");
    when(requestContext.getHeaderString("X-Admin-Username")).thenReturn(null);
    
    // Execute
    filter.filter(requestContext);
    
    // Verify
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(responseCaptor.capture());
    
    Response response = responseCaptor.getValue();
    assert response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode();
  }

  @Test
  public void testFilterAcceptsValidCredentials() throws IOException {
    // Setup
    when(uriInfo.getPath()).thenReturn("admin/v1/cache/action/flush");
    when(requestContext.getHeaderString("X-Admin-Token")).thenReturn("test-admin-token");
    when(requestContext.getHeaderString("X-Admin-Username")).thenReturn("test-user");
    
    // Execute
    filter.filter(requestContext);
    
    // Verify
    verify(requestContext, never()).abortWith(any(Response.class));
    verify(requestContext).setSecurityContext(any(SecurityContext.class));
  }
}