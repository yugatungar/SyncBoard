package com.syncboard.backend.controller;

import com.syncboard.backend.exception.RateLimitExceededException;
import com.syncboard.backend.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitTest {

    @Mock
    private org.springframework.data.redis.core.RedisOperations<String, String> redisOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisOperations);
    }

    @Test
    void checkRateLimit_withinLimit_doesNotThrow() {
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        assertDoesNotThrow(() -> rateLimitService.checkRateLimit("test-key", 10, 60));
    }

    @Test
    void checkRateLimit_exceededLimit_throwsRateLimitExceededException() {
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(11L);

        assertThrows(RateLimitExceededException.class, () -> rateLimitService.checkRateLimit("test-key", 10, 60));
    }
}
