package build.buildfarm.admin.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for the Admin Dashboard. */
@RunWith(JUnit4.class)
public class AdminDashboardIntegrationTest {

  private AdminUIResource adminUIResource;
  private StaticResourceProvider staticResourceProvider;

  @Before
  public void setUp() {
    adminUIResource = new AdminUIResource();
    staticResourceProvider = new StaticResourceProvider();
  }

  @Test
  public void testAdminDashboardResourcesAvailable() {
    // Test main dashboard HTML page
    Response dashboardResponse = adminUIResource.getAdminDashboard();
    assertEquals(Response.Status.OK.getStatusCode(), dashboardResponse.getStatus());
    assertTrue(dashboardResponse.getEntity() instanceof InputStream);

    // Test dashboard CSS resource
    Response cssResponse = staticResourceProvider.getCssResource("admin-dashboard.css");
    assertEquals(Response.Status.OK.getStatusCode(), cssResponse.getStatus());
    assertTrue(cssResponse.getEntity() instanceof InputStream);

    // Test dashboard JS resource
    Response jsResponse = staticResourceProvider.getJsResource("admin-dashboard.js");
    assertEquals(Response.Status.OK.getStatusCode(), jsResponse.getStatus());
    assertTrue(jsResponse.getEntity() instanceof InputStream);
  }

  @Test
  public void testCacheFlushIntegration() {
    // Test cache flush HTML page
    Response cacheFlushResponse = adminUIResource.getCacheFlushUI();
    assertEquals(Response.Status.OK.getStatusCode(), cacheFlushResponse.getStatus());
    assertTrue(cacheFlushResponse.getEntity() instanceof InputStream);

    // Test cache flush CSS resource
    Response cssResponse = staticResourceProvider.getCssResource("cache-flush.css");
    assertEquals(Response.Status.OK.getStatusCode(), cssResponse.getStatus());
    assertTrue(cssResponse.getEntity() instanceof InputStream);

    // Test cache flush JS resource
    Response jsResponse = staticResourceProvider.getJsResource("cache-flush.js");
    assertEquals(Response.Status.OK.getStatusCode(), jsResponse.getStatus());
    assertTrue(jsResponse.getEntity() instanceof InputStream);
  }
}
