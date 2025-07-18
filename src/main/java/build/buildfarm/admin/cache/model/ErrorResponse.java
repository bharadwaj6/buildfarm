package build.buildfarm.admin.cache.model;

import java.util.HashMap;
import java.util.Map;

/** Error response model for API errors. */
public class ErrorResponse {
  private String errorCode;
  private String message;
  private Map<String, String> details;

  /**
   * Creates a new ErrorResponse with the specified error code and message.
   *
   * @param errorCode the error code
   * @param message the error message
   */
  public ErrorResponse(String errorCode, String message) {
    this.errorCode = errorCode;
    this.message = message;
    this.details = new HashMap<>();
  }

  /**
   * Creates a new ErrorResponse with the specified error code, message, and details.
   *
   * @param errorCode the error code
   * @param message the error message
   * @param details the error details
   */
  public ErrorResponse(String errorCode, String message, Map<String, String> details) {
    this.errorCode = errorCode;
    this.message = message;
    this.details = details;
  }

  /**
   * Gets the error code.
   *
   * @return the error code
   */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * Sets the error code.
   *
   * @param errorCode the error code
   */
  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Gets the error message.
   *
   * @return the error message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the error message.
   *
   * @param message the error message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Gets the error details.
   *
   * @return the error details
   */
  public Map<String, String> getDetails() {
    return details;
  }

  /**
   * Sets the error details.
   *
   * @param details the error details
   */
  public void setDetails(Map<String, String> details) {
    this.details = details;
  }

  /**
   * Adds a detail to the error details.
   *
   * @param key the detail key
   * @param value the detail value
   */
  public void addDetail(String key, String value) {
    this.details.put(key, value);
  }
}
