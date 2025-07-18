package build.buildfarm.admin.logging;

import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Logger for flush operations. */
public class FlushOperationLogger {
  private static final Logger logger = Logger.getLogger(FlushOperationLogger.class.getName());
  private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

  /**
   * Logs an Action Cache flush operation.
   *
   * @param username the username of the user who initiated the operation
   * @param request the flush request
   * @param response the flush response
   */
  public void logActionCacheFlush(
      String username, ActionCacheFlushRequest request, ActionCacheFlushResponse response) {
    String timestamp = getCurrentTimestamp();

    StringBuilder logMessage = new StringBuilder();
    logMessage.append("Action Cache flush operation: ");
    logMessage.append("user='").append(username).append("', ");
    logMessage.append("timestamp='").append(timestamp).append("', ");
    logMessage.append("scope='").append(request.getScope()).append("', ");

    if (request.getScope().toString().equals("INSTANCE")) {
      logMessage.append("instanceName='").append(request.getInstanceName()).append("', ");
    } else if (request.getScope().toString().equals("DIGEST_PREFIX")) {
      logMessage.append("digestPrefix='").append(request.getDigestPrefix()).append("', ");
    }

    logMessage.append("flushRedis=").append(request.isFlushRedis()).append(", ");
    logMessage.append("flushInMemory=").append(request.isFlushInMemory()).append(", ");
    logMessage.append("success=").append(response.isSuccess()).append(", ");
    logMessage.append("entriesRemoved=").append(response.getEntriesRemoved());

    if (response.isSuccess()) {
      logger.info(logMessage.toString());
    } else {
      logger.warning(
          logMessage.append(", message='").append(response.getMessage()).append("'").toString());
    }
  }

  /**
   * Logs a CAS flush operation.
   *
   * @param username the username of the user who initiated the operation
   * @param request the flush request
   * @param response the flush response
   */
  public void logCASFlush(String username, CASFlushRequest request, CASFlushResponse response) {
    String timestamp = getCurrentTimestamp();

    StringBuilder logMessage = new StringBuilder();
    logMessage.append("CAS flush operation: ");
    logMessage.append("user='").append(username).append("', ");
    logMessage.append("timestamp='").append(timestamp).append("', ");
    logMessage.append("scope='").append(request.getScope()).append("', ");

    if (request.getScope().toString().equals("INSTANCE")) {
      logMessage.append("instanceName='").append(request.getInstanceName()).append("', ");
    } else if (request.getScope().toString().equals("DIGEST_PREFIX")) {
      logMessage.append("digestPrefix='").append(request.getDigestPrefix()).append("', ");
    }

    logMessage.append("flushFilesystem=").append(request.isFlushFilesystem()).append(", ");
    logMessage.append("flushInMemoryLRU=").append(request.isFlushInMemoryLRU()).append(", ");
    logMessage.append("flushRedisWorkerMap=").append(request.isFlushRedisWorkerMap()).append(", ");
    logMessage.append("success=").append(response.isSuccess()).append(", ");
    logMessage.append("entriesRemoved=").append(response.getEntriesRemoved()).append(", ");
    logMessage.append("bytesReclaimed=").append(response.getBytesReclaimed());

    if (response.isSuccess()) {
      logger.info(logMessage.toString());
    } else {
      logger.warning(
          logMessage.append(", message='").append(response.getMessage()).append("'").toString());
    }
  }

  /**
   * Logs an error during a flush operation.
   *
   * @param username the username of the user who initiated the operation
   * @param operationType the type of operation (e.g., "Action Cache flush" or "CAS flush")
   * @param error the error that occurred
   */
  public void logFlushError(String username, String operationType, Throwable error) {
    String timestamp = getCurrentTimestamp();

    StringBuilder logMessage = new StringBuilder();
    logMessage.append(operationType).append(" error: ");
    logMessage.append("user='").append(username).append("', ");
    logMessage.append("timestamp='").append(timestamp).append("', ");
    logMessage.append("error='").append(error.getMessage()).append("'");

    logger.log(Level.SEVERE, logMessage.toString(), error);
  }

  /**
   * Gets the current timestamp in ISO-8601 format.
   *
   * @return the current timestamp
   */
  private String getCurrentTimestamp() {
    return TIMESTAMP_FORMATTER.format(Instant.now());
  }
}
