package com.syncboard.backend.dto;

import java.util.UUID;

public class TaskEvent {

    private String eventId;
    private TaskEventType type;
    private Long workspaceId;
    private Long taskId;
    private String operationId;
    private String originInstanceId;
    private TaskResponse task;

    public TaskEvent() {
        this.eventId = UUID.randomUUID().toString();
    }

    public TaskEvent(TaskEventType type, Long workspaceId, Long taskId, TaskResponse task) {
        this.eventId = UUID.randomUUID().toString();
        this.type = type;
        this.workspaceId = workspaceId;
        this.taskId = taskId;
        this.task = task;
    }

    public TaskEvent(String eventId, TaskEventType type, Long workspaceId, Long taskId, String operationId, String originInstanceId, TaskResponse task) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        this.type = type;
        this.workspaceId = workspaceId;
        this.taskId = taskId;
        this.operationId = operationId;
        this.originInstanceId = originInstanceId;
        this.task = task;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public TaskEventType getType() {
        return type;
    }

    public void setType(TaskEventType type) {
        this.type = type;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getOriginInstanceId() {
        return originInstanceId;
    }

    public void setOriginInstanceId(String originInstanceId) {
        this.originInstanceId = originInstanceId;
    }

    public TaskResponse getTask() {
        return task;
    }

    public void setTask(TaskResponse task) {
        this.task = task;
    }
}
