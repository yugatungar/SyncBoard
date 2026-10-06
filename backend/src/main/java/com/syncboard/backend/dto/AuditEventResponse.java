package com.syncboard.backend.dto;

import com.syncboard.backend.entity.AuditAction;
import com.syncboard.backend.entity.AuditEvent;

import java.time.Instant;

public class AuditEventResponse {

    private Long id;
    private Long workspaceId;
    private Long userId;
    private String userName;
    private AuditAction action;
    private String entityType;
    private Long entityId;
    private String details;
    private Instant createdAt;

    public AuditEventResponse() {
    }

    public AuditEventResponse(Long id, Long workspaceId, Long userId, String userName, AuditAction action, String entityType, Long entityId, String details, Instant createdAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.userName = userName;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public static AuditEventResponse fromEntity(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getWorkspaceId(),
                event.getUserId(),
                event.getUserName(),
                event.getAction(),
                event.getEntityType(),
                event.getEntityId(),
                event.getDetails(),
                event.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
