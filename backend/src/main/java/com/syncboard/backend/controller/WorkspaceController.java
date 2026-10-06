package com.syncboard.backend.controller;

import com.syncboard.backend.dto.AddMemberRequest;
import com.syncboard.backend.dto.UpdateMemberRoleRequest;
import com.syncboard.backend.dto.WorkspaceCreateRequest;
import com.syncboard.backend.dto.WorkspaceMemberResponse;
import com.syncboard.backend.dto.WorkspaceResponse;
import com.syncboard.backend.entity.User;
import com.syncboard.backend.security.UserPrincipal;
import com.syncboard.backend.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspaces(@AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        List<WorkspaceResponse> workspaces = workspaceService.getWorkspacesForUser(user);
        return ResponseEntity.ok(workspaces);
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @Valid @RequestBody WorkspaceCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        WorkspaceResponse created = workspaceService.createWorkspace(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<List<WorkspaceMemberResponse>> getMembers(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        List<WorkspaceMemberResponse> members = workspaceService.getWorkspaceMembers(workspaceId, user);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<WorkspaceMemberResponse> addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        WorkspaceMemberResponse member = workspaceService.addMember(workspaceId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @DeleteMapping("/{workspaceId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        workspaceService.removeMember(workspaceId, targetUserId, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{workspaceId}/members/{targetUserId}/role")
    public ResponseEntity<WorkspaceMemberResponse> updateMemberRole(
            @PathVariable Long workspaceId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        WorkspaceMemberResponse updated = workspaceService.updateMemberRole(workspaceId, targetUserId, request, user);
        return ResponseEntity.ok(updated);
    }
}
