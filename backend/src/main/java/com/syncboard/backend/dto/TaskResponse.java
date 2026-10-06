package com.syncboard.backend.dto;

import com.syncboard.backend.entity.Task;
import com.syncboard.backend.entity.TaskPriority;
import com.syncboard.backend.entity.TaskStatus;

import java.time.Instant;

public class TaskResponse {

    private Long id;
    private Long workspaceId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    /**
     * JPA optimistic-lock version. Clients must echo this value back in
     * {@link UpdateTaskRequest#getVersion()} to prove they are editing the
     * latest copy they fetched.
     */
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public TaskResponse() {
    }

    public TaskResponse(Long id, Long workspaceId, String title, String description,
                        TaskStatus status, TaskPriority priority,
                        Instant createdAt, Instant updatedAt) {
        this(id, workspaceId, title, description, status, priority, 0L, createdAt, updatedAt);
    }

    public TaskResponse(Long id, Long workspaceId, String title, String description,
                        TaskStatus status, TaskPriority priority, Long version,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TaskResponse fromEntity(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getWorkspace() != null ? task.getWorkspace().getId() : null,
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getVersion(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
