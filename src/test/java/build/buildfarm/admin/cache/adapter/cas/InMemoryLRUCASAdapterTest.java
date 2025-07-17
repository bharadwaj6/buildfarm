package build.buildfarm.admin.cache.adapter.cas;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import build.buildfarm.cas.MemoryCAS;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class InMemoryLRUCASAdapterTest {
  
  @Mock private MemoryCAS mockMemoryCAS;
  
  private InMemoryLRUCASAdapter adapter;
  
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    adapter = new InMemoryLRUCASAdapter(mockMemoryCAS);
  }
  
  @Test
  public void flushAllEntries_shouldReturnSuccess() {
    // Act
    FlushResult result = adapter.flushEntries(new FlushCriteria(FlushScope.ALL, null, null));
    
    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getMessage()).contains("Flushed");
    assertThat(result.getMessage()).contains("in-memory LRU CAS entries");
    assertThat(result.getEntriesRemoved()).isEqualTo(100);
    assertThat(result.getBytesReclaimed()).isEqualTo(1024 * 1024); // 1MB
  }
  
  @Test
  public void flushInstanceEntries_shouldIndicateNotSupported() {
    // Act
    FlushResult result = adapter.flushEntries(
        new FlushCriteria(FlushScope.INSTANCE, "test-instance", null));
    
    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getMessage()).contains("Instance-specific flush not supported");
    assertThat(result.getEntriesRemoved()).isEqualTo(0);
    assertThat(result.getBytesReclaimed()).isEqualTo(0);
  }
  
  @Test
  public void flushDigestPrefixEntries_shouldIndicateNotSupported() {
    // Act
    FlushResult result = adapter.flushEntries(
        new FlushCriteria(FlushScope.DIGEST_PREFIX, null, "a1"));
    
    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getMessage()).contains("Digest prefix-specific flush not supported");
    assertThat(result.getEntriesRemoved()).isEqualTo(0);
    assertThat(result.getBytesReclaimed()).isEqualTo(0);
  }
  
  @Test
  public void flushEntries_withUnknownScope_shouldReturnError() {
    // Arrange
    FlushCriteria criteria = mock(FlushCriteria.class);
    when(criteria.getScope()).thenReturn(null);
    
    // Act
    FlushResult result = adapter.flushEntries(criteria);
    
    // Assert
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getMessage()).contains("Unknown flush scope");
    assertThat(result.getEntriesRemoved()).isEqualTo(0);
    assertThat(result.getBytesReclaimed()).isEqualTo(0);
  }
  
  @Test(expected = NullPointerException.class)
  public void flushEntries_withNullCriteria_shouldThrowException() {
    // Act
    adapter.flushEntries(null);
  }
  
  @Test(expected = NullPointerException.class)
  public void constructor_withNullMemoryCAS_shouldThrowException() {
    // Act
    new InMemoryLRUCASAdapter(null);
  }
}