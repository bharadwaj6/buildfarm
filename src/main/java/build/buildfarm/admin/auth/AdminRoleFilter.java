package build.buildfarm.admin.auth;

import java.io.IOException;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

/**
 * Authorization filter for admin API endpoints.
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class AdminRoleFilter implements ContainerRequestFilter {
  private final AuthConfig authConfig;
  
  @Inject
  public AdminRoleFilter(AuthConfig authConfig) {
    this.authConfig = authConfig;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String path = requestContext.getUriInfo().getPath();
    
    // Only apply authorization to admin API endpoints
    if (!path.startsWith("admin/")) {
      return;
    }
    
    // Skip authorization if authentication is disabled
    if (!authConfig.isAuthEnabled()) {
      return;
    }
    
    SecurityContext securityContext = requestContext.getSecurityContext();
    if (securityContext == null || securityContext.getUserPrincipal() == null) {
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity("Authentication required")
              .build());
      return;
    }
    
    // Check if the user has the admin role
    if (!securityContext.isUserInRole("admin")) {
      requestContext.abortWith(
          Response.status(Response.Status.FORBIDDEN)
              .entity("User does not have admin privileges")
              .build());
    }
  }
}