package com.syncboard.backend.service;

import com.syncboard.backend.dto.TaskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Production implementation of {@link WorkspaceEventBroadcaster}.
 *
 * <p>Publishes {@link TaskEvent} instances to the shared Redis Pub/Sub topic.
 * All backend instances (including self) subscribe to this Redis topic and
 * deliver events to their locally connected WebSocket clients.</p>
 */
@Component
@Primary
public class RedisWorkspaceEventBroadcaster implements WorkspaceEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(RedisWorkspaceEventBroadcaster.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic workspaceTopic;
    private final StompWorkspaceEventBroadcaster fallbackLocalBroadcaster;
    private final String serverInstanceId;

    public RedisWorkspaceEventBroadcaster(RedisTemplate<String, Object> redisTemplate,
                                         ChannelTopic workspaceTopic,
                                         StompWorkspaceEventBroadcaster fallbackLocalBroadcaster) {
        this.redisTemplate = redisTemplate;
        this.workspaceTopic = workspaceTopic;
        this.fallbackLocalBroadcaster = fallbackLocalBroadcaster;
        this.serverInstanceId = UUID.randomUUID().toString();
    }

    public String getServerInstanceId() {
        return serverInstanceId;
    }

    @Override
    public void broadcast(Long workspaceId, TaskEvent event) {
        if (event.getOriginInstanceId() == null) {
            event.setOriginInstanceId(serverInstanceId);
        }

        try {
            log.info("Publishing TaskEvent [{}] ({}) to Redis channel {}", event.getEventId(), event.getType(), workspaceTopic.getTopic());
            redisTemplate.convertAndSend(workspaceTopic.getTopic(), event);
        } catch (Exception ex) {
            log.warn("Redis unavailable for event publishing. Falling back to local STOMP broadcast: {}", ex.getMessage());
            // Graceful fallback if Redis is down/unavailable
            fallbackLocalBroadcaster.broadcast(workspaceId, event);
        }
    }
}
