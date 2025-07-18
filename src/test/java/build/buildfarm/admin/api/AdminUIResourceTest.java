package build.buildfarm.admin.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link AdminUIResource}.
 */
@RunWith(JUnit4.class)
public class AdminUIResourceTest {

  @Test
  public void getAdminDashboard_returnsHtmlResponse() {
    // Arrange
    AdminUIResource resource = new AdminUIResource();
    
    // Act
    Response response = resource.getAdminDashboard();
    
    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
  }

  @Test
  public void getCacheFlushUI_returnsHtmlResponse() {
    // Arrange
    AdminUIResource resource = new AdminUIResource();
    
    // Act
    Response response = resource.getCacheFlushUI();
    
    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
  }
}