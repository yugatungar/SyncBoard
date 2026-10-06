package com.syncboard.backend.controller;

import com.syncboard.backend.dto.PresenceEvent;
import com.syncboard.backend.service.PresenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<PresenceEvent> heartbeat(
            @PathVariable Long workspaceId,
            @RequestParam String clientId) {
        PresenceEvent event = presenceService.recordHeartbeat(workspaceId, clientId);
        return ResponseEntity.ok(event);
    }

    @PostMapping("/disconnect")
    public ResponseEntity<PresenceEvent> disconnect(
            @PathVariable Long workspaceId,
            @RequestParam String clientId) {
        PresenceEvent event = presenceService.recordDisconnect(workspaceId, clientId);
        return ResponseEntity.ok(event);
    }

    @GetMapping
    public ResponseEntity<PresenceEvent> getOnlineCount(@PathVariable Long workspaceId) {
        int count = presenceService.getOnlineCount(workspaceId);
        return ResponseEntity.ok(new PresenceEvent(workspaceId, count));
    }
}
