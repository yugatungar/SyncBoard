package com.syncboard.backend.service;

import com.syncboard.backend.dto.TaskEvent;

/**
 * Abstraction over the messaging infrastructure used to broadcast task events
 * to workspace subscribers.
 *
 * <p>Decoupling {@link TaskEventPublisher} from {@code SimpMessagingTemplate}
 * directly allows unit tests to use a simple in-process fake rather than
 * requiring Mockito to byte-instrument Spring framework classes (which fails
 * on Java 25 due to Byte Buddy version constraints).</p>
 */
public interface WorkspaceEventBroadcaster {

    /**
     * Send {@code event} to all subscribers of the given workspace topic.
     *
     * @param workspaceId the target workspace
     * @param event       the task event payload
     */
    void broadcast(Long workspaceId, TaskEvent event);
}
