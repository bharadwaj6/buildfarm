package build.buildfarm.admin.cache.model;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FlushResultTest {

  @Test
  public void testDefaultConstructor() {
    FlushResult result = new FlushResult();
    
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getMessage()).isEmpty();
    assertThat(result.getEntriesRemoved()).isEqualTo(0);
    assertThat(result.getBytesReclaimed()).isEqualTo(0);
  }

  @Test
  public void testParameterizedConstructor() {
    FlushResult result = new FlushResult(false, "Failed", 10, 1024);
    
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getMessage()).isEqualTo("Failed");
    assertThat(result.getEntriesRemoved()).isEqualTo(10);
    assertThat(result.getBytesReclaimed()).isEqualTo(1024);
  }

  @Test
  public void testSetters() {
    FlushResult result = new FlushResult();
    
    result.setSuccess(false);
    result.setMessage("Test message");
    result.setEntriesRemoved(5);
    result.setBytesReclaimed(512);
    
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getMessage()).isEqualTo("Test message");
    assertThat(result.getEntriesRemoved()).isEqualTo(5);
    assertThat(result.getBytesReclaimed()).isEqualTo(512);
  }

  @Test
  public void testMergeWithEmptyResults() {
    FlushResult result1 = new FlushResult();
    FlushResult result2 = new FlushResult();
    
    result1.merge(result2);
    
    assertThat(result1.isSuccess()).isTrue();
    assertThat(result1.getMessage()).isEmpty();
    assertThat(result1.getEntriesRemoved()).isEqualTo(0);
    assertThat(result1.getBytesReclaimed()).isEqualTo(0);
  }

  @Test
  public void testMergeWithNonEmptyResults() {
    FlushResult result1 = new FlushResult(true, "Success 1", 5, 512);
    FlushResult result2 = new FlushResult(true, "Success 2", 10, 1024);
    
    result1.merge(result2);
    
    assertThat(result1.isSuccess()).isTrue();
    assertThat(result1.getMessage()).isEqualTo("Success 1, Success 2");
    assertThat(result1.getEntriesRemoved()).isEqualTo(15);
    assertThat(result1.getBytesReclaimed()).isEqualTo(1536);
  }

  @Test
  public void testMergeWithFailure() {
    FlushResult result1 = new FlushResult(true, "Success", 5, 512);
    FlushResult result2 = new FlushResult(false, "Failed", 0, 0);
    
    result1.merge(result2);
    
    assertThat(result1.isSuccess()).isFalse();
    assertThat(result1.getMessage()).isEqualTo("Success, Failed");
    assertThat(result1.getEntriesRemoved()).isEqualTo(5);
    assertThat(result1.getBytesReclaimed()).isEqualTo(512);
  }
}