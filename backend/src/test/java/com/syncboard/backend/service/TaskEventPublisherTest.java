package com.syncboard.backend.service;

import com.syncboard.backend.dto.TaskEvent;
import com.syncboard.backend.dto.TaskEventType;
import com.syncboard.backend.dto.TaskResponse;
import com.syncboard.backend.entity.TaskPriority;
import com.syncboard.backend.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for {@link TaskEventPublisher}.
 *
 * <p>Uses a hand-rolled in-memory fake for {@link WorkspaceEventBroadcaster}
 * instead of Mockito, which cannot instrument Spring framework classes under
 * Java 25 due to a Byte Buddy version constraint.</p>
 */
class TaskEventPublisherTest {

    /** Captures broadcast calls without any Mockito/Byte Buddy involvement. */
    static class CapturingBroadcaster implements WorkspaceEventBroadcaster {
        final List<Long> capturedWorkspaceIds = new ArrayList<>();
        final List<TaskEvent> capturedEvents = new ArrayList<>();

        @Override
        public void broadcast(Long workspaceId, TaskEvent event) {
            capturedWorkspaceIds.add(workspaceId);
            capturedEvents.add(event);
        }
    }

    private CapturingBroadcaster broadcaster;
    private TaskEventPublisher publisher;

    @BeforeEach
    void setUp() {
        broadcaster = new CapturingBroadcaster();
        publisher = new TaskEventPublisher(broadcaster);
    }

    @Test
    void publishTaskCreated_sendsEventToWorkspaceTopic() {
        TaskResponse task = new TaskResponse(10L, 1L, "Title", "Desc",
                TaskStatus.TODO, TaskPriority.HIGH, Instant.now(), Instant.now());

        publisher.publishTaskCreated(1L, task);

        assertThat(broadcaster.capturedWorkspaceIds).containsExactly(1L);
        TaskEvent event = broadcaster.capturedEvents.get(0);
        assertThat(event.getType()).isEqualTo(TaskEventType.TASK_CREATED);
        assertThat(event.getWorkspaceId()).isEqualTo(1L);
        assertThat(event.getTaskId()).isEqualTo(10L);
        assertThat(event.getTask()).isNotNull();
    }

    @Test
    void publishTaskDeleted_sendsEventToWorkspaceTopic() {
        publisher.publishTaskDeleted(2L, 42L);

        assertThat(broadcaster.capturedWorkspaceIds).containsExactly(2L);
        TaskEvent event = broadcaster.capturedEvents.get(0);
        assertThat(event.getType()).isEqualTo(TaskEventType.TASK_DELETED);
        assertThat(event.getWorkspaceId()).isEqualTo(2L);
        assertThat(event.getTaskId()).isEqualTo(42L);
        assertThat(event.getTask()).isNull();
    }

    @Test
    void publishTaskUpdated_sendsEventToWorkspaceTopic() {
        TaskResponse task = new TaskResponse(5L, 3L, "Updated", null,
                TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, Instant.now(), Instant.now());

        publisher.publishTaskUpdated(3L, task);

        assertThat(broadcaster.capturedWorkspaceIds).containsExactly(3L);
        TaskEvent event = broadcaster.capturedEvents.get(0);
        assertThat(event.getType()).isEqualTo(TaskEventType.TASK_UPDATED);
        assertThat(event.getWorkspaceId()).isEqualTo(3L);
        assertThat(event.getTaskId()).isEqualTo(5L);
        assertThat(event.getTask()).isNotNull();
    }
}
