package com.syncboard.backend.controller;

import com.syncboard.backend.dto.AuditEventResponse;
import com.syncboard.backend.entity.User;
import com.syncboard.backend.security.UserPrincipal;
import com.syncboard.backend.service.AuditService;
import com.syncboard.backend.service.WorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/audit")
public class AuditController {

    private final AuditService auditService;
    private final WorkspaceService workspaceService;

    public AuditController(AuditService auditService, WorkspaceService workspaceService) {
        this.auditService = auditService;
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ResponseEntity<List<AuditEventResponse>> getAuditHistory(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal != null ? principal.getUser() : null;
        if (user != null) {
            workspaceService.verifyMembership(workspaceId, user.getId());
        }

        List<AuditEventResponse> history = auditService.getAuditHistory(workspaceId, limit);
        return ResponseEntity.ok(history);
    }
}
