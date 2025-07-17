package build.buildfarm.admin.auth;

import java.security.Principal;

/**
 * Principal implementation for admin users.
 */
public class AdminPrincipal implements Principal {
  private final String name;
  private final boolean isAdmin;

  public AdminPrincipal(String name, boolean isAdmin) {
    this.name = name;
    this.isAdmin = isAdmin;
  }

  @Override
  public String getName() {
    return name;
  }

  public boolean isAdmin() {
    return isAdmin;
  }
}