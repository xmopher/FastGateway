package com.mo.gateway.component.ratelimit.storage;

import com.mo.gateway.model.ratelimit.RateLimitBucket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Redis-based Rate Limit Storage
 * Provides distributed rate limiting across multiple gateway instances
 */
@Component
public class RedisRateLimitStorage implements RateLimitStorage {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitStorage.class);

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    // Lua script for atomic increment with expiration
    private static final String INCREMENT_SCRIPT = """
        local current = redis.call('GET', KEYS[1])
        if current == false then
            redis.call('SET', KEYS[1], 1)
            redis.call('EXPIRE', KEYS[1], ARGV[1])
            return 1
        else
            return redis.call('INCR', KEYS[1])
        end
        """;

    public RedisRateLimitStorage(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<Long> increment(String key, long expiration) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var script = new DefaultRedisScript<Long>(INCREMENT_SCRIPT, Long.class);
                return redisTemplate.execute(script,
                        Collections.singletonList(key),
                        String.valueOf(expiration / 1000));
            } catch (Exception e) {
                log.error("Failed to increment key: {}", key, e);
                throw new RuntimeException("Redis increment failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<RateLimitBucket> getBucket(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var bucketJson = (String) redisTemplate.opsForValue().get(key);
                if (bucketJson == null) {
                    return null;
                }
                return objectMapper.readValue(bucketJson, RateLimitBucket.class);
            } catch (Exception e) {
                log.error("Failed to get bucket for key: {}", key, e);
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> setBucket(String key, RateLimitBucket bucket, long ttl) {
        return CompletableFuture.runAsync(() -> {
            try {
                var bucketJson = objectMapper.writeValueAsString(bucket);
                redisTemplate.opsForValue().set(key, bucketJson, Duration.ofMillis(ttl));
            } catch (Exception e) {
                log.error("Failed to set bucket for key: {}", key, e);
                throw new RuntimeException("Redis set bucket failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> setIfNotExists(String key, RateLimitBucket bucket, long ttl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var bucketJson = objectMapper.writeValueAsString(bucket);
                return redisTemplate.opsForValue().setIfAbsent(key, bucketJson, Duration.ofMillis(ttl));
            } catch (Exception e) {
                log.error("Failed to set bucket if not exists for key: {}", key, e);
                return false;
            }
        });
    }
}