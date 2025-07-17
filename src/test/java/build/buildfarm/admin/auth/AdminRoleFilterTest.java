package build.buildfarm.admin.auth;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.security.Principal;
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
public class AdminRoleFilterTest {

  @Mock private ContainerRequestContext requestContext;
  @Mock private UriInfo uriInfo;
  @Mock private SecurityContext securityContext;
  @Mock private Principal principal;
  
  private AuthConfig authConfig;
  private AdminRoleFilter filter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    
    authConfig = new AuthConfig();
    authConfig.setAuthEnabled(true);
    
    filter = new AdminRoleFilter(authConfig);
    
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getSecurityContext()).thenReturn(securityContext);
    when(securityContext.getUserPrincipal()).thenReturn(principal);
  }

  @Test
  public void testFilterSkipsNonAdminPaths() throws IOException {
    // Setup
    when(uriInfo.getPath()).thenReturn("api/v1/some/path");
    
    // Execute
    filter.filter(requestContext);
    
    // Verify
    verify(requestContext, never()).abortWith(any(Response.class));
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
  }

  @Test
  public void testFilterRejectsUnauthenticatedRequests() throws IOException {
    // Setup
    when(uriInfo.getPath()).thenReturn("admin/v1/cache/action/flush");
    when(securityContext.getUserPrincipal()).thenReturn(null);
    
    // Execute
    filter.filter(requestContext);
    
    // Verify
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(responseCaptor.capture());
    
    Response response = responseCaptor.getValue();
    assert response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode();
  }

  @Test
  public void testFilterRejectsNonAdminUsers() throws IOException {
    // Setup
    when(uriInfo.getPath()).thenReturn("admin/v1/cache/action/flush");
    when(securityContext.isUserInRole("admin")).thenReturn(false);
    
    // Execute
    filter.filter(requestContext);
    
    // Verify
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(responseCaptor.capture());
    
    Response response = responseCaptor.getValue();
    assert response.getStatus() == Response.Status.FORBIDDEN.getStatusCode();
  }

  @Test
  public void testFilterAllowsAdminUsers() throws IOException {
    // Setup
    when(uriInfo.getPath()).thenReturn("admin/v1/cache/action/flush");
    when(securityContext.isUserInRole("admin")).thenReturn(true);
    
    // Execute
    filter.filter(requestContext);
    
    // Verify
    verify(requestContext, never()).abortWith(any(Response.class));
  }
}