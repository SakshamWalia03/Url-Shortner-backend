package com.url.shortener.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Generic rate limiting service using Redis.
 *
 * Supports:
 * - login rate limiting
 * - registration rate limiting
 * - API request limiting
 * - spam protection
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Checks whether a request is allowed within a rate limit window.
     *
     * @param key unique identifier (userId or IP)
     * @param limit max allowed requests
     * @param windowSeconds time window
     * @return true if request allowed, false if limit exceeded
     */
    public boolean isAllowed(String key, int limit, int windowSeconds) {

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            return false;
        }

        if (count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        return count <= limit;
    }
}