package build.buildfarm.admin.auth;

/**
 * Configuration for authentication settings.
 */
public class AuthConfig {
  private boolean authEnabled = true;
  private String adminTokenHeader = "X-Admin-Token";
  private String adminUsernameHeader = "X-Admin-Username";
  private String adminToken = "admin-token"; // In production, this should be loaded securely
  
  public boolean isAuthEnabled() {
    return authEnabled;
  }
  
  public void setAuthEnabled(boolean authEnabled) {
    this.authEnabled = authEnabled;
  }
  
  public String getAdminTokenHeader() {
    return adminTokenHeader;
  }
  
  public void setAdminTokenHeader(String adminTokenHeader) {
    this.adminTokenHeader = adminTokenHeader;
  }
  
  public String getAdminUsernameHeader() {
    return adminUsernameHeader;
  }
  
  public void setAdminUsernameHeader(String adminUsernameHeader) {
    this.adminUsernameHeader = adminUsernameHeader;
  }
  
  public String getAdminToken() {
    return adminToken;
  }
  
  public void setAdminToken(String adminToken) {
    this.adminToken = adminToken;
  }
}