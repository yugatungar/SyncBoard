package com.syncboard.backend.dto;

import java.time.Instant;

public class PresenceEvent {

    private Long workspaceId;
    private int onlineCount;
    private String eventType;
    private Instant timestamp;

    public PresenceEvent() {
        this.timestamp = Instant.now();
        this.eventType = "PRESENCE_UPDATED";
    }

    public PresenceEvent(Long workspaceId, int onlineCount) {
        this.workspaceId = workspaceId;
        this.onlineCount = onlineCount;
        this.eventType = "PRESENCE_UPDATED";
        this.timestamp = Instant.now();
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public int getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(int onlineCount) {
        this.onlineCount = onlineCount;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
