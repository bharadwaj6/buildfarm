package build.buildfarm.admin.cache.adapter.cas;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import build.buildfarm.cas.cfc.CASFileCache;
import build.buildfarm.v1test.Digest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class FilesystemCASAdapterTest {

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock private CASFileCache mockFileCache;

  private FilesystemCASAdapter adapter;
  private Path cacheRoot;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);

    cacheRoot = tempFolder.newFolder("cas").toPath();
    when(mockFileCache.getRoot()).thenReturn(cacheRoot);

    adapter = new FilesystemCASAdapter(mockFileCache);
  }

  @Test
  public void flushAllEntries_shouldRemoveAllCasFiles() throws IOException {
    // Arrange
    createTestFiles();

    // Act
    FlushResult result = adapter.flushEntries(new FlushCriteria(FlushScope.ALL, null, null));

    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(3); // 3 CAS files
    assertThat(result.getBytesReclaimed()).isEqualTo(30); // 10 bytes per file

    // Verify that only CAS files were removed
    assertThat(Files.exists(cacheRoot.resolve("a1b2c3"))).isFalse();
    assertThat(Files.exists(cacheRoot.resolve("d4e5f6_exec"))).isFalse();
    assertThat(Files.exists(cacheRoot.resolve("sha256_g7h8i9_exec"))).isFalse();

    // Non-CAS files should still exist
    assertThat(Files.exists(cacheRoot.resolve("not_a_cas_file.txt"))).isTrue();
    assertThat(Files.exists(cacheRoot.resolve("directory_entry_dir"))).isTrue();
  }

  @Test
  public void flushDigestPrefixEntries_shouldRemoveMatchingFiles() throws IOException {
    // Arrange
    createTestFiles();

    // Act
    FlushResult result =
        adapter.flushEntries(new FlushCriteria(FlushScope.DIGEST_PREFIX, null, "a1"));

    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntriesRemoved()).isEqualTo(1); // Only a1b2c3
    assertThat(result.getBytesReclaimed()).isEqualTo(10); // 10 bytes

    // Verify that only matching files were removed
    assertThat(Files.exists(cacheRoot.resolve("a1b2c3"))).isFalse();
    assertThat(Files.exists(cacheRoot.resolve("d4e5f6_exec"))).isTrue();
    assertThat(Files.exists(cacheRoot.resolve("sha256_g7h8i9_exec"))).isTrue();
  }

  @Test
  public void flushInstanceEntries_shouldHandleInstanceScope() throws IOException {
    // Arrange
    createTestFiles();

    // Act
    FlushResult result =
        adapter.flushEntries(new FlushCriteria(FlushScope.INSTANCE, "test-instance", null));

    // Assert
    assertThat(result.isSuccess()).isTrue();
    // Current implementation doesn't support instance-based filtering
    assertThat(result.getEntriesRemoved()).isEqualTo(0);
  }

  @Test
  public void isCasFile_shouldIdentifyCasFiles() {
    // Arrange
    Path casFile1 = Paths.get("a1b2c3");
    Path casFile2 = Paths.get("d4e5f6_exec");
    Path casFile3 = Paths.get("sha256_g7h8i9_exec");
    Path nonCasFile1 = Paths.get("not_a_cas_file.txt");
    Path nonCasFile2 = Paths.get("directory_entry_dir");

    // Act & Assert
    assertThat(adapter.isCasFile(casFile1)).isTrue();
    assertThat(adapter.isCasFile(casFile2)).isTrue();
    assertThat(adapter.isCasFile(casFile3)).isTrue();
    assertThat(adapter.isCasFile(nonCasFile1)).isFalse();
    assertThat(adapter.isCasFile(nonCasFile2)).isFalse();
  }

  @Test
  public void hasDigestPrefix_shouldCheckDigestPrefix() {
    // Arrange
    Path casFile1 = Paths.get("a1b2c3");
    Path casFile2 = Paths.get("d4e5f6_exec");
    Path casFile3 = Paths.get("sha256_g7h8i9_exec");

    // Act & Assert
    assertThat(adapter.hasDigestPrefix(casFile1, "a1")).isTrue();
    assertThat(adapter.hasDigestPrefix(casFile1, "b2")).isFalse();
    assertThat(adapter.hasDigestPrefix(casFile2, "d4")).isTrue();
    assertThat(adapter.hasDigestPrefix(casFile3, "g7")).isTrue();
  }

  @Test
  public void calculateTotalSize_shouldSumFileSizes() throws IOException {
    // Arrange
    Path file1 = cacheRoot.resolve("file1");
    Path file2 = cacheRoot.resolve("file2");
    Files.write(file1, new byte[10]);
    Files.write(file2, new byte[20]);

    // Act
    long totalSize = adapter.calculateTotalSize(Arrays.asList(file1, file2));

    // Assert
    assertThat(totalSize).isEqualTo(30);
  }

  @Test
  public void deleteFiles_shouldDeleteFilesAndInvalidateCache() throws IOException {
    // Arrange
    Path file1 = cacheRoot.resolve("a1b2c3");
    Path file2 = cacheRoot.resolve("d4e5f6_exec");
    Files.write(file1, new byte[10]);
    Files.write(file2, new byte[10]);

    // Act
    int deletedCount = adapter.deleteFiles(Arrays.asList(file1, file2));

    // Assert
    assertThat(deletedCount).isEqualTo(2);
    assertThat(Files.exists(file1)).isFalse();
    assertThat(Files.exists(file2)).isFalse();

    // Verify that invalidateWrite was called for the executable file
    ArgumentCaptor<Digest> digestCaptor = ArgumentCaptor.forClass(Digest.class);
    verify(mockFileCache).invalidateWrite(digestCaptor.capture());

    // We can't easily verify all calls due to the static DigestUtil.buildDigest,
    // but we can verify at least one call happened
    assertThat(digestCaptor.getAllValues()).isNotEmpty();
  }

  private void createTestFiles() throws IOException {
    // Create CAS files
    Files.write(cacheRoot.resolve("a1b2c3"), new byte[10]);
    Files.write(cacheRoot.resolve("d4e5f6_exec"), new byte[10]);
    Files.write(cacheRoot.resolve("sha256_g7h8i9_exec"), new byte[10]);

    // Create non-CAS files
    Files.write(cacheRoot.resolve("not_a_cas_file.txt"), new byte[10]);
    Files.createDirectory(cacheRoot.resolve("directory_entry_dir"));
  }
}
