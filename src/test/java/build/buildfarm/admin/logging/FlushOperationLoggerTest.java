package build.buildfarm.admin.logging;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import build.buildfarm.admin.cache.model.FlushScope;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link FlushOperationLogger}. */
@RunWith(JUnit4.class)
public class FlushOperationLoggerTest {
  private FlushOperationLogger logger;
  private TestLogHandler logHandler;
  private Logger underlyingLogger;

  @Before
  public void setUp() {
    logger = new FlushOperationLogger();
    logHandler = new TestLogHandler();

    // Get the underlying logger used by FlushOperationLogger and add our test handler
    underlyingLogger = Logger.getLogger(FlushOperationLogger.class.getName());
    underlyingLogger.addHandler(logHandler);
    underlyingLogger.setLevel(Level.ALL);
  }

  @After
  public void tearDown() {
    underlyingLogger.removeHandler(logHandler);
  }

  @Test
  public void logActionCacheFlush_successfulOperation_logsInfo() {
    // Arrange
    String username = "test-admin";
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);
    request.setFlushInMemory(false);

    ActionCacheFlushResponse response = new ActionCacheFlushResponse();
    response.setSuccess(true);
    response.setEntriesRemoved(10);
    Map<String, Integer> entriesRemovedByBackend = new HashMap<>();
    entriesRemovedByBackend.put("redis", 10);
    response.setEntriesRemovedByBackend(entriesRemovedByBackend);

    // Act
    logger.logActionCacheFlush(username, request, response);

    // Assert
    assertTrue(logHandler.hasLogRecordWithLevel(Level.INFO));
    assertTrue(logHandler.hasLogRecordWithMessage("Action Cache flush operation"));
    assertTrue(logHandler.hasLogRecordWithMessage("user='test-admin'"));
    assertTrue(logHandler.hasLogRecordWithMessage("scope='ALL'"));
    assertTrue(logHandler.hasLogRecordWithMessage("flushRedis=true"));
    assertTrue(logHandler.hasLogRecordWithMessage("flushInMemory=false"));
    assertTrue(logHandler.hasLogRecordWithMessage("entriesRemoved=10"));
  }

  @Test
  public void logActionCacheFlush_instanceScope_logsInstanceName() {
    // Arrange
    String username = "test-admin";
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.INSTANCE);
    request.setInstanceName("test-instance");
    request.setFlushRedis(true);

    ActionCacheFlushResponse response = new ActionCacheFlushResponse();
    response.setSuccess(true);
    response.setEntriesRemoved(5);

    // Act
    logger.logActionCacheFlush(username, request, response);

    // Assert
    assertTrue(logHandler.hasLogRecordWithMessage("instanceName='test-instance'"));
  }

  @Test
  public void logActionCacheFlush_digestPrefixScope_logsDigestPrefix() {
    // Arrange
    String username = "test-admin";
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.DIGEST_PREFIX);
    request.setDigestPrefix("abc123");
    request.setFlushRedis(true);

    ActionCacheFlushResponse response = new ActionCacheFlushResponse();
    response.setSuccess(true);
    response.setEntriesRemoved(3);

    // Act
    logger.logActionCacheFlush(username, request, response);

    // Assert
    assertTrue(logHandler.hasLogRecordWithMessage("digestPrefix='abc123'"));
  }

  @Test
  public void logActionCacheFlush_failedOperation_logsWarning() {
    // Arrange
    String username = "test-admin";
    ActionCacheFlushRequest request = new ActionCacheFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushRedis(true);

    ActionCacheFlushResponse response = new ActionCacheFlushResponse();
    response.setSuccess(false);
    response.setMessage("Failed to flush Action Cache");

    // Act
    logger.logActionCacheFlush(username, request, response);

    // Assert
    assertTrue(logHandler.hasLogRecordWithLevel(Level.WARNING));
    assertTrue(logHandler.hasLogRecordWithMessage("message='Failed to flush Action Cache'"));
  }

  @Test
  public void logCASFlush_successfulOperation_logsInfo() {
    // Arrange
    String username = "test-admin";
    CASFlushRequest request = new CASFlushRequest();
    request.setScope(FlushScope.ALL);
    request.setFlushFilesystem(true);
    request.setFlushInMemoryLRU(false);
    request.setFlushRedisWorkerMap(false);

    CASFlushResponse response = new CASFlushResponse();
    response.setSuccess(true);
    response.setEntriesRemoved(20);
    response.setBytesReclaimed(1024);
    Map<String, Integer> entriesRemovedByBackend = new HashMap<>();
    entriesRemovedByBackend.put("filesystem", 20);
    response.setEntriesRemovedByBackend(entriesRemovedByBackend);
    Map<String, Long> bytesReclaimedByBackend = new HashMap<>();
    bytesReclaimedByBackend.put("filesystem", 1024L);
    response.setBytesReclaimedByBackend(bytesReclaimedByBackend);

    // Act
    logger.logCASFlush(username, request, response);

    // Assert
    assertTrue(logHandler.hasLogRecordWithLevel(Level.INFO));
    assertTrue(logHandler.hasLogRecordWithMessage("CAS flush operation"));
    assertTrue(logHandler.hasLogRecordWithMessage("user='test-admin'"));
    assertTrue(logHandler.hasLogRecordWithMessage("scope='ALL'"));
    assertTrue(logHandler.hasLogRecordWithMessage("flushFilesystem=true"));
    assertTrue(logHandler.hasLogRecordWithMessage("flushInMemoryLRU=false"));
    assertTrue(logHandler.hasLogRecordWithMessage("flushRedisWorkerMap=false"));
    assertTrue(logHandler.hasLogRecordWithMessage("entriesRemoved=20"));
    assertTrue(logHandler.hasLogRecordWithMessage("bytesReclaimed=1024"));
  }

  @Test
  public void logFlushError_logsError() {
    // Arrange
    String username = "test-admin";
    String operationType = "Action Cache flush";
    Exception error = new RuntimeException("Test error");

    // Act
    logger.logFlushError(username, operationType, error);

    // Assert
    assertTrue(logHandler.hasLogRecordWithLevel(Level.SEVERE));
    assertTrue(logHandler.hasLogRecordWithMessage("Action Cache flush error"));
    assertTrue(logHandler.hasLogRecordWithMessage("user='test-admin'"));
    assertTrue(logHandler.hasLogRecordWithMessage("error='Test error'"));
  }

  /** Test log handler that captures log records for verification. */
  private static class TestLogHandler extends Handler {
    private final java.util.List<LogRecord> records = new java.util.ArrayList<>();

    @Override
    public void publish(LogRecord record) {
      records.add(record);
    }

    @Override
    public void flush() {
      // No-op
    }

    @Override
    public void close() throws SecurityException {
      records.clear();
    }

    public boolean hasLogRecordWithLevel(Level level) {
      for (LogRecord record : records) {
        if (record.getLevel() == level) {
          return true;
        }
      }
      return false;
    }

    public boolean hasLogRecordWithMessage(String substring) {
      for (LogRecord record : records) {
        if (record.getMessage().contains(substring)) {
          return true;
        }
      }
      return false;
    }
  }
}
