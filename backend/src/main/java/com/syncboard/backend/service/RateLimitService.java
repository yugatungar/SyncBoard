package com.syncboard.backend.service;

import com.syncboard.backend.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private final org.springframework.data.redis.core.RedisOperations<String, String> redisTemplate;

    public RateLimitService(org.springframework.data.redis.core.RedisOperations<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks rate limit using Redis increment and TTL expiration.
     *
     * @param key unique identifier (e.g. ratelimit:login:127.0.0.1 or ratelimit:task:user42)
     * @param maxRequests max allowed requests in window
     * @param windowSeconds time window in seconds
     */
    public void checkRateLimit(String key, int maxRequests, int windowSeconds) {
        try {
            Long current = redisTemplate.opsForValue().increment(key);
            if (current != null && current == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }

            if (current != null && current > maxRequests) {
                log.warn("Rate limit exceeded for key '{}' ({}/{})", key, current, maxRequests);
                throw new RateLimitExceededException(
                        "Rate limit exceeded. Maximum " + maxRequests + " requests allowed per " + windowSeconds + " seconds."
                );
            }
        } catch (RateLimitExceededException ex) {
            throw ex;
        } catch (Exception ex) {
            // Fallback: If Redis is down, log warning and allow request to prevent breaking application
            log.warn("Redis rate limit check failed for key '{}': {}", key, ex.getMessage());
        }
    }
}
