package build.buildfarm.admin.cache.adapter.cas;

import static com.google.common.truth.Truth.assertThat;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link CASAdapter}.
 */
@RunWith(JUnit4.class)
public class CASAdapterTest {

  /**
   * Test implementation of CASAdapter for testing the contract.
   */
  private static class TestCASAdapter implements CASAdapter {
    private boolean flushAllCalled = false;
    private boolean flushInstanceCalled = false;
    private boolean flushDigestPrefixCalled = false;
    private String lastInstanceName = null;
    private String lastDigestPrefix = null;

    @Override
    public FlushResult flushEntries(FlushCriteria criteria) {
      switch (criteria.getScope()) {
        case ALL:
          flushAllCalled = true;
          return new FlushResult(true, "All CAS entries flushed", 20, 2048);
        case INSTANCE:
          flushInstanceCalled = true;
          lastInstanceName = criteria.getInstanceName();
          return new FlushResult(true, "Instance CAS entries flushed", 10, 1024);
        case DIGEST_PREFIX:
          flushDigestPrefixCalled = true;
          lastDigestPrefix = criteria.getDigestPrefix();
          return new FlushResult(true, "Digest prefix CAS entries flushed", 5, 512);
        default:
          return new FlushResult(false, "Unknown scope", 0, 0);
      }
    }
  }

  @Test
  public void testFlushAll() {
    TestCASAdapter adapter = new TestCASAdapter();
    FlushCriteria criteria = new FlushCriteria(FlushScope.ALL, null, null);
    
    FlushResult result = adapter.flushEntries(criteria);
    
    assertThat(adapter.flushAllCalled).isTrue();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getMessage()).isEqualTo("All CAS entries flushed");
    assertThat(result.getEntriesRemoved()).isEqualTo(20);
    assertThat(result.getBytesReclaimed()).isEqualTo(2048);
  }

  @Test
  public void testFlushInstance() {
    TestCASAdapter adapter = new TestCASAdapter();
    FlushCriteria criteria = new FlushCriteria(FlushScope.INSTANCE, "test-instance", null);
    
    FlushResult result = adapter.flushEntries(criteria);
    
    assertThat(adapter.flushInstanceCalled).isTrue();
    assertThat(adapter.lastInstanceName).isEqualTo("test-instance");
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getMessage()).isEqualTo("Instance CAS entries flushed");
    assertThat(result.getEntriesRemoved()).isEqualTo(10);
    assertThat(result.getBytesReclaimed()).isEqualTo(1024);
  }

  @Test
  public void testFlushDigestPrefix() {
    TestCASAdapter adapter = new TestCASAdapter();
    FlushCriteria criteria = new FlushCriteria(FlushScope.DIGEST_PREFIX, null, "abc123");
    
    FlushResult result = adapter.flushEntries(criteria);
    
    assertThat(adapter.flushDigestPrefixCalled).isTrue();
    assertThat(adapter.lastDigestPrefix).isEqualTo("abc123");
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getMessage()).isEqualTo("Digest prefix CAS entries flushed");
    assertThat(result.getEntriesRemoved()).isEqualTo(5);
    assertThat(result.getBytesReclaimed()).isEqualTo(512);
  }
}