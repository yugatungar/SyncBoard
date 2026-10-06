package com.syncboard.backend.service;

import com.syncboard.backend.dto.TaskEvent;
import com.syncboard.backend.dto.TaskEventType;
import com.syncboard.backend.dto.TaskResponse;
import org.springframework.stereotype.Service;

@Service
public class TaskEventPublisher {

    private final WorkspaceEventBroadcaster broadcaster;

    public TaskEventPublisher(WorkspaceEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void publishTaskCreated(Long workspaceId, TaskResponse task) {
        TaskEvent event = new TaskEvent(TaskEventType.TASK_CREATED, workspaceId, task.getId(), task);
        broadcaster.broadcast(workspaceId, event);
    }

    public void publishTaskUpdated(Long workspaceId, TaskResponse task) {
        TaskEvent event = new TaskEvent(TaskEventType.TASK_UPDATED, workspaceId, task.getId(), task);
        broadcaster.broadcast(workspaceId, event);
    }

    public void publishTaskDeleted(Long workspaceId, Long taskId) {
        TaskEvent event = new TaskEvent(TaskEventType.TASK_DELETED, workspaceId, taskId, null);
        broadcaster.broadcast(workspaceId, event);
    }

    public void publishMemberUpdated(Long workspaceId) {
        TaskEvent event = new TaskEvent(TaskEventType.MEMBER_UPDATED, workspaceId, null, null);
        broadcaster.broadcast(workspaceId, event);
    }
}
