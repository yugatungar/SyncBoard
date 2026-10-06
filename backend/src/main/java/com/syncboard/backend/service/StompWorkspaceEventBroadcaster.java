package com.syncboard.backend.service;

import com.syncboard.backend.dto.TaskEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Production implementation of {@link WorkspaceEventBroadcaster}.
 * Wraps {@link SimpMessagingTemplate} and publishes events to the STOMP
 * simple broker under {@code /topic/workspaces/{workspaceId}}.
 */
@Component
public class StompWorkspaceEventBroadcaster implements WorkspaceEventBroadcaster {

    private static final String TOPIC_PREFIX = "/topic/workspaces/";

    private final SimpMessagingTemplate messagingTemplate;

    public StompWorkspaceEventBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcast(Long workspaceId, TaskEvent event) {
        messagingTemplate.convertAndSend(TOPIC_PREFIX + workspaceId, event);
    }
}
