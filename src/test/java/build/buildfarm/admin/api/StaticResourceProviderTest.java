package build.buildfarm.admin.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link StaticResourceProvider}. */
@RunWith(JUnit4.class)
public class StaticResourceProviderTest {

  @Test
  public void getCssResource_returnsValidResponse() {
    // Arrange
    StaticResourceProvider provider = new StaticResourceProvider();

    // Act
    Response response = provider.getCssResource("cache-flush.css");

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
  }

  @Test
  public void getJsResource_returnsValidResponse() {
    // Arrange
    StaticResourceProvider provider = new StaticResourceProvider();

    // Act
    Response response = provider.getJsResource("cache-flush.js");

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
  }

  @Test
  public void getHtmlResource_returnsValidResponse() {
    // Arrange
    StaticResourceProvider provider = new StaticResourceProvider();

    // Act
    Response response = provider.getHtmlResource("cache-flush.html");

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
  }

  @Test
  public void getNonExistentResource_returns404() {
    // Arrange
    StaticResourceProvider provider = new StaticResourceProvider();

    // Act
    Response response = provider.getCssResource("non-existent.css");

    // Assert
    assertNotNull(response);
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
}
