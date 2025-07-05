package com.mo.gateway.service.auth;

import com.mo.gateway.model.dto.GatewayRequest;
import com.mo.gateway.spi.auth.AuthenticationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication Context Factory
 * Creates authentication contexts for authentication providers
 */
@Component
public class AuthContextFactory {

    private static final Logger log = LoggerFactory.getLogger(AuthContextFactory.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public AuthContextFactory(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Create authentication context
     */
    public AuthenticationContext createContext(GatewayRequest request) {
        return new AuthenticationContext(
                request,
                new RedisCacheManager(),
                new PropertyConfigManager(),
                new Slf4jLoggerManager(),
                new ConcurrentHashMap<>()
        );
    }

    /**
     * Redis Cache Manager Implementation
     */
    private class RedisCacheManager implements AuthenticationContext.CacheManager {

        @Override
        public void put(String key, Object value, Duration ttl) {
            try {
                redisTemplate.opsForValue().set("auth:" + key, value, ttl);
            } catch (Exception e) {
                log.error("Failed to cache value for key: {}", key, e);
            }
        }

        @Override
        public <T> Optional<T> get(String key, Class<T> type) {
            try {
                Object value = redisTemplate.opsForValue().get("auth:" + key);
                if (value != null && type.isInstance(value)) {
                    return Optional.of((T) value);
                }
            } catch (Exception e) {
                log.error("Failed to get cached value for key: {}", key, e);
            }
            return Optional.empty();
        }

        @Override
        public void remove(String key) {
            try {
                redisTemplate.delete("auth:" + key);
            } catch (Exception e) {
                log.error("Failed to remove cached value for key: {}", key, e);
            }
        }

        @Override
        public void clear() {
            try {
                redisTemplate.delete(redisTemplate.keys("auth:*"));
            } catch (Exception e) {
                log.error("Failed to clear auth cache", e);
            }
        }
    }

    /**
     * Property Configuration Manager Implementation
     */
    private static class PropertyConfigManager implements AuthenticationContext.ConfigManager {
        @Override
        public <T> Optional<T> get(String key, Class<T> type) {
            String value = System.getProperty(key);
            if (value == null) {
                value = System.getenv(key);
            }
            if (value != null) {
                try {
                    if (type == String.class) {
                        return Optional.of((T) value);
                    } else if (type == Integer.class) {
                        return Optional.of((T) Integer.valueOf(value));
                    } else if (type == Long.class) {
                        return Optional.of((T) Long.valueOf(value));
                    } else if (type == Boolean.class) {
                        return Optional.of((T) Boolean.valueOf(value));
                    } else if (type == Double.class) {
                        return Optional.of((T) Double.valueOf(value));
                    }
                } catch (NumberFormatException e) {
                    log.warn("Failed to convert config value {} to type {}", value, type.getSimpleName());
                }
            }
            return Optional.empty();
        }

        @Override
        public boolean containsKey(String key) {
            return System.getProperty(key) != null || System.getenv(key) != null;
        }
    }

    /**
     * SLF4J Logger Manager Implementation
     */
    private static class Slf4jLoggerManager implements AuthenticationContext.LoggerManager {

        private static final Logger authLogger = LoggerFactory.getLogger("AUTH");

        @Override
        public void info(String message, Object... args) {
            authLogger.info(message, args);
        }

        @Override
        public void warn(String message, Object... args) {
            authLogger.warn(message, args);
        }

        @Override
        public void error(String message, Object... args) {
            authLogger.error(message, args);
        }

        @Override
        public void error(String message, Throwable throwable, Object... args) {
            authLogger.error(message, throwable, args);
        }
    }
}