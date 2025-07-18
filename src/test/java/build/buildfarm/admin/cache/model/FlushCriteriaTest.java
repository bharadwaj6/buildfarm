package build.buildfarm.admin.cache.model;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FlushCriteriaTest {

  @Test
  public void testConstructorWithAllScope() {
    FlushCriteria criteria = new FlushCriteria(FlushScope.ALL, null, null);

    assertThat(criteria.getScope()).isEqualTo(FlushScope.ALL);
    assertThat(criteria.getInstanceName()).isNull();
    assertThat(criteria.getDigestPrefix()).isNull();
  }

  @Test
  public void testConstructorWithInstanceScope() {
    FlushCriteria criteria = new FlushCriteria(FlushScope.INSTANCE, "test-instance", null);

    assertThat(criteria.getScope()).isEqualTo(FlushScope.INSTANCE);
    assertThat(criteria.getInstanceName()).isEqualTo("test-instance");
    assertThat(criteria.getDigestPrefix()).isNull();
  }

  @Test
  public void testConstructorWithDigestPrefixScope() {
    FlushCriteria criteria = new FlushCriteria(FlushScope.DIGEST_PREFIX, null, "abc123");

    assertThat(criteria.getScope()).isEqualTo(FlushScope.DIGEST_PREFIX);
    assertThat(criteria.getInstanceName()).isNull();
    assertThat(criteria.getDigestPrefix()).isEqualTo("abc123");
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithNullScope() {
    new FlushCriteria(null, null, null);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithInstanceScopeAndNullInstanceName() {
    new FlushCriteria(FlushScope.INSTANCE, null, null);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorWithDigestPrefixScopeAndNullDigestPrefix() {
    new FlushCriteria(FlushScope.DIGEST_PREFIX, null, null);
  }
}
