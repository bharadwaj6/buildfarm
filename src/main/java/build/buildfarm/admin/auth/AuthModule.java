package build.buildfarm.admin.auth;

import javax.inject.Singleton;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * Guice module for authentication and authorization.
 */
public class AuthModule extends AbstractModule {
  
  @Override
  protected void configure() {
    bind(AdminAuthFilter.class);
    bind(AdminRoleFilter.class);
  }
  
  @Provides
  @Singleton
  public AuthConfig provideAuthConfig() {
    // In a real implementation, this would load from a configuration file or environment
    AuthConfig config = new AuthConfig();
    // Default configuration
    config.setAuthEnabled(true);
    config.setAdminTokenHeader("X-Admin-Token");
    config.setAdminUsernameHeader("X-Admin-Username");
    config.setAdminToken("admin-token"); // In production, this should be loaded securely
    return config;
  }
}