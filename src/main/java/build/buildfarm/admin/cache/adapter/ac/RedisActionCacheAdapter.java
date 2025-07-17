package build.buildfarm.admin.cache.adapter.ac;

import build.buildfarm.admin.cache.model.FlushCriteria;
import build.buildfarm.admin.cache.model.FlushResult;
import build.buildfarm.admin.cache.model.FlushScope;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.Set;
import javax.inject.Inject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

/**
 * Implementation of {@link ActionCacheAdapter} for Redis-backed Action Cache.
 */
public class RedisActionCacheAdapter implements ActionCacheAdapter {
  
  private static final String ACTION_CACHE_KEY_PREFIX = "ac:";
  private static final int SCAN_COUNT = 100;
  
  private final JedisPool jedisPool;
  
  /**
   * Creates a new RedisActionCacheAdapter instance.
   *
   * @param jedisPool the Redis connection pool
   */
  @Inject
  public RedisActionCacheAdapter(JedisPool jedisPool) {
    this.jedisPool = Preconditions.checkNotNull(jedisPool, "jedisPool");
  }
  
  @Override
  public FlushResult flushEntries(FlushCriteria criteria) {
    Preconditions.checkNotNull(criteria, "criteria");
    
    switch (criteria.getScope()) {
      case ALL:
        return flushAllEntries();
      case INSTANCE:
        return flushInstanceEntries(criteria.getInstanceName());
      case DIGEST_PREFIX:
        return flushDigestPrefixEntries(criteria.getDigestPrefix());
      default:
        return new FlushResult(false, "Unknown flush scope: " + criteria.getScope(), 0, 0);
    }
  }
  
  /**
   * Flushes all Action Cache entries from Redis.
   *
   * @return the result of the flush operation
   */
  private FlushResult flushAllEntries() {
    try (Jedis jedis = jedisPool.getResource()) {
      Set<String> keys = scanKeys(jedis, ACTION_CACHE_KEY_PREFIX + "*");
      int entriesRemoved = keys.size();
      
      if (!keys.isEmpty()) {
        jedis.del(keys.toArray(new String[0]));
      }
      
      return new FlushResult(true, "Flushed all Action Cache entries from Redis", entriesRemoved, 0);
    } catch (Exception e) {
      return new FlushResult(false, "Failed to flush Action Cache entries: " + e.getMessage(), 0, 0);
    }
  }
  
  /**
   * Flushes Action Cache entries for a specific instance from Redis.
   *
   * @param instanceName the instance name
   * @return the result of the flush operation
   */
  private FlushResult flushInstanceEntries(String instanceName) {
    try (Jedis jedis = jedisPool.getResource()) {
      Set<String> keys = scanKeys(jedis, ACTION_CACHE_KEY_PREFIX + instanceName + ":*");
      int entriesRemoved = keys.size();
      
      if (!keys.isEmpty()) {
        jedis.del(keys.toArray(new String[0]));
      }
      
      return new FlushResult(
          true, "Flushed Action Cache entries for instance " + instanceName, entriesRemoved, 0);
    } catch (Exception e) {
      return new FlushResult(false, "Failed to flush Action Cache entries: " + e.getMessage(), 0, 0);
    }
  }
  
  /**
   * Flushes Action Cache entries with a specific digest prefix from Redis.
   *
   * @param digestPrefix the digest prefix
   * @return the result of the flush operation
   */
  private FlushResult flushDigestPrefixEntries(String digestPrefix) {
    try (Jedis jedis = jedisPool.getResource()) {
      Set<String> keys = scanKeys(jedis, ACTION_CACHE_KEY_PREFIX + "*:" + digestPrefix + "*");
      int entriesRemoved = keys.size();
      
      if (!keys.isEmpty()) {
        jedis.del(keys.toArray(new String[0]));
      }
      
      return new FlushResult(
          true, "Flushed Action Cache entries with digest prefix " + digestPrefix, entriesRemoved, 0);
    } catch (Exception e) {
      return new FlushResult(false, "Failed to flush Action Cache entries: " + e.getMessage(), 0, 0);
    }
  }
  
  /**
   * Scans Redis for keys matching the given pattern.
   *
   * @param jedis the Jedis instance
   * @param pattern the pattern to match
   * @return a set of keys matching the pattern
   */
  @VisibleForTesting
  Set<String> scanKeys(Jedis jedis, String pattern) {
    Set<String> keys = new java.util.HashSet<>();
    String cursor = "0";
    ScanParams scanParams = new ScanParams().match(pattern).count(SCAN_COUNT);
    
    do {
      ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
      keys.addAll(scanResult.getResult());
      cursor = scanResult.getCursor();
    } while (!cursor.equals("0"));
    
    return keys;
  }
}