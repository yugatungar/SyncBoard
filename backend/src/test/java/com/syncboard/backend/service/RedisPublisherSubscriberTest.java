package com.syncboard.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.syncboard.backend.dto.PresenceEvent;
import com.syncboard.backend.dto.TaskEvent;
import com.syncboard.backend.dto.TaskEventType;
import com.syncboard.backend.dto.TaskResponse;
import com.syncboard.backend.entity.TaskPriority;
import com.syncboard.backend.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for Redis messaging components without Spring context or external Redis connection.
 */
class RedisPublisherSubscriberTest {

    static class CapturingStompBroadcaster extends StompWorkspaceEventBroadcaster {
        final List<TaskEvent> capturedEvents = new ArrayList<>();

        public CapturingStompBroadcaster() {
            super(null);
        }

        @Override
        public void broadcast(Long workspaceId, TaskEvent event) {
            capturedEvents.add(event);
        }
    }

    private ObjectMapper objectMapper;
    private CapturingStompBroadcaster localStompBroadcaster;
    private RedisEventSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        localStompBroadcaster = new CapturingStompBroadcaster();
        subscriber = new RedisEventSubscriber(objectMapper, localStompBroadcaster, null);
    }

    @Test
    void onMessage_deserializesTaskEvent_and_deliversToLocalStomp() throws Exception {
        TaskResponse taskResp = new TaskResponse(100L, 1L, "Redis Test Task", "Desc",
                TaskStatus.TODO, TaskPriority.HIGH, 0L, Instant.now(), Instant.now());

        TaskEvent originalEvent = new TaskEvent(TaskEventType.TASK_CREATED, 1L, 100L, taskResp);
        originalEvent.setOperationId("op-123");
        originalEvent.setOriginInstanceId("server-node-1");

        byte[] payload = objectMapper.writeValueAsBytes(originalEvent);
        Message redisMessage = new DefaultMessage("syncboard:workspace-events".getBytes(), payload);

        subscriber.onMessage(redisMessage, null);

        assertThat(localStompBroadcaster.capturedEvents).hasSize(1);
        TaskEvent received = localStompBroadcaster.capturedEvents.get(0);

        assertThat(received.getEventId()).isEqualTo(originalEvent.getEventId());
        assertThat(received.getType()).isEqualTo(TaskEventType.TASK_CREATED);
        assertThat(received.getWorkspaceId()).isEqualTo(1L);
        assertThat(received.getTaskId()).isEqualTo(100L);
        assertThat(received.getOperationId()).isEqualTo("op-123");
        assertThat(received.getOriginInstanceId()).isEqualTo("server-node-1");
        assertThat(received.getTask().getTitle()).isEqualTo("Redis Test Task");
    }

    @Test
    void taskEvent_generatesUniqueEventId_ifNull() {
        TaskEvent event = new TaskEvent();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).isNotEmpty();
    }
}
