package build.buildfarm.admin.api;

import build.buildfarm.admin.cache.service.CacheFlushService;
import build.buildfarm.admin.cache.model.ActionCacheFlushRequest;
import build.buildfarm.admin.cache.model.ActionCacheFlushResponse;
import build.buildfarm.admin.cache.model.CASFlushRequest;
import build.buildfarm.admin.cache.model.CASFlushResponse;
import build.buildfarm.admin.cache.model.FlushScope;
import build.buildfarm.admin.cache.model.ErrorResponse;
import com.google.common.base.Strings;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST resource for cache flushing operations.
 */
@Path("/admin/v1/cache")
public class CacheFlushResource {
  private static final Logger logger = Logger.getLogger(CacheFlushResource.class.getName());
  private final CacheFlushService cacheFlushService;

  @Inject
  public CacheFlushResource(CacheFlushService cacheFlushService) {
    this.cacheFlushService = cacheFlushService;
  }

  /**
   * Flushes Action Cache entries based on specified criteria.
   *
   * @param request the flush request
   * @return the response
   */
  @POST
  @Path("/action/flush")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response flushActionCache(ActionCacheFlushRequest request) {
    // Validate request
    Response validationResponse = validateActionCacheFlushRequest(request);
    if (validationResponse != null) {
      return validationResponse;
    }

    try {
      logger.info("Processing Action Cache flush request: scope=" + request.getScope() 
          + ", flushRedis=" + request.isFlushRedis() 
          + ", flushInMemory=" + request.isFlushInMemory());
      
      ActionCacheFlushResponse response = cacheFlushService.flushActionCache(request);
      
      if (!response.isSuccess()) {
        logger.warning("Action Cache flush operation failed: " + response.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(response)
            .build();
      }
      
      logger.info("Action Cache flush operation completed successfully: " 
          + response.getEntriesRemoved() + " entries removed");
      return Response.ok(response).build();
    } catch (IllegalArgumentException e) {
      logger.log(Level.WARNING, "Invalid Action Cache flush request", e);
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error flushing Action Cache", e);
      ErrorResponse errorResponse = new ErrorResponse("INTERNAL_ERROR", 
          "Error flushing Action Cache: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(errorResponse)
          .build();
    }
  }

  /**
   * Flushes Content Addressable Storage entries based on specified criteria.
   *
   * @param request the flush request
   * @return the response
   */
  @POST
  @Path("/cas/flush")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response flushCAS(CASFlushRequest request) {
    // Validate request
    Response validationResponse = validateCASFlushRequest(request);
    if (validationResponse != null) {
      return validationResponse;
    }

    try {
      logger.info("Processing CAS flush request: scope=" + request.getScope() 
          + ", flushFilesystem=" + request.isFlushFilesystem() 
          + ", flushInMemoryLRU=" + request.isFlushInMemoryLRU()
          + ", flushRedisWorkerMap=" + request.isFlushRedisWorkerMap());
      
      CASFlushResponse response = cacheFlushService.flushCAS(request);
      
      if (!response.isSuccess()) {
        logger.warning("CAS flush operation failed: " + response.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(response)
            .build();
      }
      
      logger.info("CAS flush operation completed successfully: " 
          + response.getEntriesRemoved() + " entries removed, " 
          + response.getBytesReclaimed() + " bytes reclaimed");
      return Response.ok(response).build();
    } catch (IllegalArgumentException e) {
      logger.log(Level.WARNING, "Invalid CAS flush request", e);
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error flushing CAS", e);
      ErrorResponse errorResponse = new ErrorResponse("INTERNAL_ERROR", 
          "Error flushing CAS: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(errorResponse)
          .build();
    }
  }
  
  /**
   * Validates an Action Cache flush request.
   *
   * @param request the request to validate
   * @return a Response if validation fails, null if validation succeeds
   */
  private Response validateActionCacheFlushRequest(ActionCacheFlushRequest request) {
    if (request == null) {
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", "Request cannot be null");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    }
    
    if (request.getScope() == null) {
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", "Scope must be specified");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    }
    
    if (request.getScope() == FlushScope.INSTANCE 
        && Strings.isNullOrEmpty(request.getInstanceName())) {
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", 
          "Instance name must be specified when scope is INSTANCE");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    }
    
    if (request.getScope() == FlushScope.DIGEST_PREFIX 
        && Strings.isNullOrEmpty(request.getDigestPrefix())) {
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", 
          "Digest prefix must be specified when scope is DIGEST_PREFIX");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    }
    
    if (!request.isFlushRedis() && !request.isFlushInMemory()) {
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", 
          "At least one backend must be selected for flushing");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    }
    
    return null;
  }
  
  /**
   * Validates a CAS flush request.
   *
   * @param request the request to validate
   * @return a Response if validation fails, null if validation succeeds
   */
  private Response validateCASFlushRequest(CASFlushRequest request) {
    if (request == null) {
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", "Request cannot be null");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    }
    
    if (request.getScope() == null) {
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", "Scope must be specified");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    }
    
    if (request.getScope() == FlushScope.INSTANCE 
        && Strings.isNullOrEmpty(request.getInstanceName())) {
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", 
          "Instance name must be specified when scope is INSTANCE");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    }
    
    if (request.getScope() == FlushScope.DIGEST_PREFIX 
        && Strings.isNullOrEmpty(request.getDigestPrefix())) {
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", 
          "Digest prefix must be specified when scope is DIGEST_PREFIX");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    }
    
    if (!request.isFlushFilesystem() 
        && !request.isFlushInMemoryLRU() 
        && !request.isFlushRedisWorkerMap()) {
      ErrorResponse errorResponse = new ErrorResponse("INVALID_ARGUMENT", 
          "At least one backend must be selected for flushing");
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(errorResponse)
          .build();
    }
    
    return null;
  }
}