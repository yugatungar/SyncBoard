package com.syncboard.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncboard.backend.dto.PresenceEvent;
import com.syncboard.backend.dto.TaskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Listens for Redis Pub/Sub messages across all instances.
 *
 * <p>When a message arrives from Redis, this subscriber deserializes the event
 * and delivers it to local WebSocket subscribers connected to this backend instance.
 * It NEVER republishes the event to Redis, preventing event loops.</p>
 */
@Component
public class RedisEventSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisEventSubscriber.class);

    private final ObjectMapper objectMapper;
    private final StompWorkspaceEventBroadcaster localStompBroadcaster;
    private final SimpMessagingTemplate messagingTemplate;

    public RedisEventSubscriber(ObjectMapper objectMapper,
                                StompWorkspaceEventBroadcaster localStompBroadcaster,
                                SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.localStompBroadcaster = localStompBroadcaster;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            log.debug("Received Redis Pub/Sub message: {}", json);

            // Attempt to deserialize as TaskEvent
            if (json.contains("\"type\"") && (json.contains("TASK_CREATED") || json.contains("TASK_UPDATED") || json.contains("TASK_DELETED"))) {
                TaskEvent event = objectMapper.readValue(json, TaskEvent.class);
                log.info("Delivering TaskEvent [{}] ({}) to local WebSocket subscribers for workspace {}",
                        event.getEventId(), event.getType(), event.getWorkspaceId());
                localStompBroadcaster.broadcast(event.getWorkspaceId(), event);

            } else if (json.contains("PRESENCE_UPDATED") || json.contains("\"onlineCount\"")) {
                PresenceEvent presenceEvent = objectMapper.readValue(json, PresenceEvent.class);
                log.info("Delivering PresenceEvent for workspace {} (online: {}) to local WebSocket subscribers",
                        presenceEvent.getWorkspaceId(), presenceEvent.getOnlineCount());
                messagingTemplate.convertAndSend("/topic/workspaces/" + presenceEvent.getWorkspaceId(), presenceEvent);
            }
        } catch (IOException ex) {
            log.error("Failed to deserialize Redis Pub/Sub message: {}", ex.getMessage(), ex);
        }
    }
}
