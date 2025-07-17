package build.buildfarm.admin.auth;

import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

/**
 * Authentication filter for admin API endpoints.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AdminAuthFilter implements ContainerRequestFilter {
  private final AuthConfig authConfig;
  
  @Inject
  public AdminAuthFilter(AuthConfig authConfig) {
    this.authConfig = authConfig;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String path = requestContext.getUriInfo().getPath();
    
    // Only apply authentication to admin API endpoints
    if (!path.startsWith("admin/")) {
      return;
    }
    
    // Skip authentication if it's disabled
    if (!authConfig.isAuthEnabled()) {
      return;
    }
    
    // Get the authentication credentials from the request
    String token = requestContext.getHeaderString(authConfig.getAdminTokenHeader());
    String username = requestContext.getHeaderString(authConfig.getAdminUsernameHeader());
    
    // Validate the token and username
    if (!isValidToken(token) || username == null || username.isEmpty()) {
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity("Authentication failed")
              .build());
      return;
    }
    
    // Create a security context with the authenticated user
    final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
    requestContext.setSecurityContext(new SecurityContext() {
      @Override
      public Principal getUserPrincipal() {
        return new AdminPrincipal(username, true);
      }

      @Override
      public boolean isUserInRole(String role) {
        return "admin".equals(role);
      }

      @Override
      public boolean isSecure() {
        return currentSecurityContext.isSecure();
      }

      @Override
      public String getAuthenticationScheme() {
        return "AdminToken";
      }
    });
  }
  
  private boolean isValidToken(String token) {
    // In a real implementation, this would validate against a secure token store
    return token != null && token.equals(authConfig.getAdminToken());
  }
}