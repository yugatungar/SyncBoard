package com.syncboard.backend.service;

import com.syncboard.backend.dto.PresenceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Manages short-lived, ephemeral user presence in Redis.
 *
 * <p>Each active client sends periodic heartbeats. Each heartbeat sets a Redis key
 * {@code presence:workspace:{workspaceId}:user:{clientId}} with a 10-second TTL.
 * If a client disconnects or stops pinging, Redis automatically expires the key,
 * causing the user to disappear from the online count.</p>
 */
@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);
    private static final Duration TTL_DURATION = Duration.ofSeconds(10);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic presenceTopic;

    public PresenceService(RedisTemplate<String, Object> redisTemplate, ChannelTopic presenceTopic) {
        this.redisTemplate = redisTemplate;
        this.presenceTopic = presenceTopic;
    }

    public PresenceEvent recordHeartbeat(Long workspaceId, String clientId) {
        if (workspaceId == null || clientId == null || clientId.trim().isEmpty()) {
            return new PresenceEvent(workspaceId != null ? workspaceId : 0L, 0);
        }

        String key = buildPresenceKey(workspaceId, clientId.trim());
        try {
            redisTemplate.opsForValue().set(key, System.currentTimeMillis());
            redisTemplate.expire(key, TTL_DURATION);

            int onlineCount = getOnlineCount(workspaceId);
            PresenceEvent event = new PresenceEvent(workspaceId, onlineCount);

            // Publish presence update to Redis so all instances receive it
            redisTemplate.convertAndSend(presenceTopic.getTopic(), event);
            return event;
        } catch (Exception ex) {
            log.debug("Redis presence error for workspace {}: {}", workspaceId, ex.getMessage());
            return new PresenceEvent(workspaceId, 1);
        }
    }

    public PresenceEvent recordDisconnect(Long workspaceId, String clientId) {
        if (workspaceId == null || clientId == null || clientId.trim().isEmpty()) {
            return new PresenceEvent(workspaceId != null ? workspaceId : 0L, 0);
        }

        String key = buildPresenceKey(workspaceId, clientId.trim());
        try {
            redisTemplate.delete(key);
            int onlineCount = getOnlineCount(workspaceId);
            PresenceEvent event = new PresenceEvent(workspaceId, onlineCount);
            redisTemplate.convertAndSend(presenceTopic.getTopic(), event);
            return event;
        } catch (Exception ex) {
            log.debug("Redis disconnect error for workspace {}: {}", workspaceId, ex.getMessage());
            return new PresenceEvent(workspaceId, 0);
        }
    }

    public int getOnlineCount(Long workspaceId) {
        if (workspaceId == null) return 0;
        try {
            String pattern = "presence:workspace:" + workspaceId + ":user:*";
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception ex) {
            log.debug("Failed to fetch online presence count from Redis: {}", ex.getMessage());
            return 1;
        }
    }

    private String buildPresenceKey(Long workspaceId, String clientId) {
        return "presence:workspace:" + workspaceId + ":user:" + clientId;
    }
}
