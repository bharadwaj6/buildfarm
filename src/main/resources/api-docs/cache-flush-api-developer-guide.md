# BuildFarm Cache Flush API Developer Guide

## Overview

The Cache Flush API provides BuildFarm administrators with the ability to selectively or completely flush parts of the Action Cache (AC) and Content Addressable Storage (CAS). This guide explains how to use the API, provides examples, and outlines best practices.

## API Endpoints

The Cache Flush API consists of two main endpoints:

1. `POST /admin/v1/cache/action/flush` - Flush Action Cache entries
2. `POST /admin/v1/cache/cas/flush` - Flush Content Addressable Storage entries

Both endpoints require administrator authentication and accept JSON request bodies.

## Authentication

All API endpoints require administrator authentication. The API uses HTTP Basic Authentication. Make sure to include the appropriate authentication headers in your requests:

```
Authorization: Basic <base64-encoded-credentials>
```

## Flushing the Action Cache

### Endpoint

```
POST /admin/v1/cache/action/flush
```

### Request Format

```json
{
  "scope": "ALL | INSTANCE | DIGEST_PREFIX",
  "instanceName": "string",  // Required when scope is INSTANCE
  "digestPrefix": "string",  // Required when scope is DIGEST_PREFIX
  "flushRedis": boolean,     // Whether to flush Redis-backed entries
  "flushInMemory": boolean   // Whether to flush in-memory entries
}
```

### Scope Options

- `ALL`: Flush all entries in the Action Cache
- `INSTANCE`: Flush entries for a specific instance
- `DIGEST_PREFIX`: Flush entries with a specific digest prefix

### Examples

#### Flush All Action Cache Entries

```json
{
  "scope": "ALL",
  "flushRedis": true,
  "flushInMemory": true
}
```

#### Flush Action Cache Entries for a Specific Instance

```json
{
  "scope": "INSTANCE",
  "instanceName": "default_instance",
  "flushRedis": true,
  "flushInMemory": false
}
```

#### Flush Action Cache Entries with a Specific Digest Prefix

```json
{
  "scope": "DIGEST_PREFIX",
  "digestPrefix": "abc123",
  "flushRedis": false,
  "flushInMemory": true
}
```

### Response Format

```json
{
  "success": boolean,
  "message": "string",
  "entriesRemoved": integer,
  "entriesRemovedByBackend": {
    "redis": integer,
    "in-memory": integer
  }
}
```

## Flushing the Content Addressable Storage

### Endpoint

```
POST /admin/v1/cache/cas/flush
```

### Request Format

```json
{
  "scope": "ALL | INSTANCE | DIGEST_PREFIX",
  "instanceName": "string",      // Required when scope is INSTANCE
  "digestPrefix": "string",      // Required when scope is DIGEST_PREFIX
  "flushFilesystem": boolean,    // Whether to flush filesystem-backed entries
  "flushInMemoryLRU": boolean,   // Whether to flush in-memory LRU cache entries
  "flushRedisWorkerMap": boolean // Whether to flush Redis-backed CAS worker map entries
}
```

### Scope Options

- `ALL`: Flush all entries in the CAS
- `INSTANCE`: Flush entries for a specific instance
- `DIGEST_PREFIX`: Flush entries with a specific digest prefix

### Examples

#### Flush All CAS Entries

```json
{
  "scope": "ALL",
  "flushFilesystem": true,
  "flushInMemoryLRU": true,
  "flushRedisWorkerMap": true
}
```

#### Flush CAS Entries for a Specific Instance

```json
{
  "scope": "INSTANCE",
  "instanceName": "default_instance",
  "flushFilesystem": true,
  "flushInMemoryLRU": false,
  "flushRedisWorkerMap": false
}
```

#### Flush CAS Entries with a Specific Digest Prefix

```json
{
  "scope": "DIGEST_PREFIX",
  "digestPrefix": "abc123",
  "flushFilesystem": false,
  "flushInMemoryLRU": true,
  "flushRedisWorkerMap": true
}
```

### Response Format

```json
{
  "success": boolean,
  "message": "string",
  "entriesRemoved": integer,
  "bytesReclaimed": integer,
  "entriesRemovedByBackend": {
    "filesystem": integer,
    "in-memory-lru": integer,
    "redis-worker-map": integer
  },
  "bytesReclaimedByBackend": {
    "filesystem": integer,
    "in-memory-lru": integer,
    "redis-worker-map": integer
  }
}
```

## Error Handling

The API returns appropriate HTTP status codes and error responses:

- `400 Bad Request`: Invalid request parameters
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: User does not have administrator privileges
- `429 Too Many Requests`: Rate limit exceeded
- `503 Service Unavailable`: Concurrency limit reached
- `500 Internal Server Error`: Server-side error

### Error Response Format

```json
{
  "errorCode": "string",
  "message": "string",
  "details": {
    "additionalProperty": "string"
  }
}
```

### Rate Limit Exceeded Response Format

```json
{
  "message": "string",
  "operationsPerformed": integer,
  "maxOperationsPerWindow": integer,
  "windowSizeMs": integer,
  "timeRemainingMs": integer
}
```

### Concurrency Limit Exceeded Response Format

```json
{
  "message": "string",
  "currentOperations": integer,
  "maxConcurrentOperations": integer
}
```

## Best Practices

### Scope Selection

- **Use `ALL` with caution**: Flushing all entries can impact system performance and ongoing builds. Use this scope only when necessary, such as during maintenance windows.
- **Prefer `INSTANCE` or `DIGEST_PREFIX`**: When possible, target specific instances or digest prefixes to minimize the impact on the system.

### Backend Selection

- **Target specific backends**: Only flush the backends that need to be cleared. For example, if you only need to reclaim disk space, target the filesystem backend for CAS.
- **Stagger operations**: When flushing multiple backends, consider staggering the operations to minimize the impact on system performance.

### Rate Limiting

The API includes rate limiting to prevent abuse. Be aware of the following limits:

- Action Cache flush: 5 operations per minute per user
- CAS flush: 3 operations per minute per user

### Concurrency Control

The API includes concurrency control to prevent too many simultaneous flush operations:

- Action Cache flush: 5 concurrent operations
- CAS flush: 3 concurrent operations

If you receive a concurrency limit exceeded response, wait and retry the operation later.

## Monitoring

After performing flush operations, monitor the system to ensure it continues to function correctly:

- Check system logs for any errors
- Monitor cache hit rates to ensure they return to normal levels
- Monitor build times to ensure they are not significantly impacted

## Example: Using the API with curl

### Flush Action Cache

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  -d '{
    "scope": "ALL",
    "flushRedis": true,
    "flushInMemory": true
  }' \
  http://localhost:8080/admin/v1/cache/action/flush
```

### Flush CAS

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  -d '{
    "scope": "INSTANCE",
    "instanceName": "default_instance",
    "flushFilesystem": true,
    "flushInMemoryLRU": false,
    "flushRedisWorkerMap": false
  }' \
  http://localhost:8080/admin/v1/cache/cas/flush
```

## Example: Using the API with Java

```java
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Base64;

public class CacheFlushExample {
    public static void main(String[] args) {
        try {
            // Flush Action Cache
            URL url = new URL("http://localhost:8080/admin/v1/cache/action/flush");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            
            // Set authentication header
            String auth = "admin:password";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            
            conn.setDoOutput(true);
            
            String jsonInput = "{\"scope\":\"ALL\",\"flushRedis\":true,\"flushInMemory\":true}";
            
            OutputStream os = conn.getOutputStream();
            os.write(jsonInput.getBytes());
            os.flush();
            
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream()));
            
            String output;
            StringBuilder response = new StringBuilder();
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            
            System.out.println("Response: " + response.toString());
            
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Example: Using the API with Python

```python
import requests
import json
import base64

# Set up authentication
username = "admin"
password = "password"
auth_string = f"{username}:{password}"
auth_bytes = auth_string.encode('ascii')
base64_bytes = base64.b64encode(auth_bytes)
base64_auth = base64_bytes.decode('ascii')

headers = {
    "Content-Type": "application/json",
    "Authorization": f"Basic {base64_auth}"
}

# Flush Action Cache
action_cache_url = "http://localhost:8080/admin/v1/cache/action/flush"
action_cache_payload = {
    "scope": "ALL",
    "flushRedis": True,
    "flushInMemory": True
}

response = requests.post(
    action_cache_url,
    headers=headers,
    data=json.dumps(action_cache_payload)
)

print(f"Status Code: {response.status_code}")
print(f"Response: {response.json()}")

# Flush CAS
cas_url = "http://localhost:8080/admin/v1/cache/cas/flush"
cas_payload = {
    "scope": "INSTANCE",
    "instanceName": "default_instance",
    "flushFilesystem": True,
    "flushInMemoryLRU": False,
    "flushRedisWorkerMap": False
}

response = requests.post(
    cas_url,
    headers=headers,
    data=json.dumps(cas_payload)
)

print(f"Status Code: {response.status_code}")
print(f"Response: {response.json()}")
```

## Troubleshooting

### Common Issues

1. **Authentication Failures**
   - Ensure you are using valid administrator credentials
   - Check that the credentials are properly Base64 encoded

2. **Invalid Request Parameters**
   - Ensure the `scope` is one of: `ALL`, `INSTANCE`, or `DIGEST_PREFIX`
   - When using `INSTANCE` scope, ensure `instanceName` is provided
   - When using `DIGEST_PREFIX` scope, ensure `digestPrefix` is provided
   - Ensure at least one backend is selected for flushing

3. **Rate Limit Exceeded**
   - Wait until the rate limit window resets before trying again
   - Consider staggering operations across multiple users if available

4. **Concurrency Limit Exceeded**
   - Wait until other flush operations complete before trying again
   - Monitor the number of active flush operations using system metrics

5. **Operation Taking Too Long**
   - Large flush operations, especially for filesystem-backed CAS, can take a long time
   - Consider using a more targeted scope or backend selection
   - Consider increasing client timeout settings

### Getting Help

If you encounter issues not covered in this guide, please:

1. Check the system logs for detailed error messages
2. Review the API documentation for any updates or changes
3. Contact the BuildFarm team for assistance

## Conclusion

The Cache Flush API provides a powerful tool for managing BuildFarm caches. By following the guidelines in this document, you can effectively use the API to maintain system health and performance.

Remember to use the API responsibly, as flush operations can impact system performance and ongoing builds. Always prefer targeted operations over broad ones, and consider scheduling large operations during maintenance windows.