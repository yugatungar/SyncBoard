package com.syncboard.backend.service;

import com.syncboard.backend.dto.AddMemberRequest;
import com.syncboard.backend.dto.UpdateMemberRoleRequest;
import com.syncboard.backend.dto.WorkspaceCreateRequest;
import com.syncboard.backend.dto.WorkspaceMemberResponse;
import com.syncboard.backend.dto.WorkspaceResponse;
import com.syncboard.backend.entity.AuditAction;
import com.syncboard.backend.entity.MemberRole;
import com.syncboard.backend.entity.User;
import com.syncboard.backend.entity.Workspace;
import com.syncboard.backend.entity.WorkspaceMember;
import com.syncboard.backend.exception.ConflictException;
import com.syncboard.backend.exception.ResourceNotFoundException;
import com.syncboard.backend.repository.UserRepository;
import com.syncboard.backend.repository.WorkspaceMemberRepository;
import com.syncboard.backend.repository.WorkspaceRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TaskEventPublisher taskEventPublisher;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository workspaceMemberRepository,
                            UserRepository userRepository,
                            AuditService auditService,
                            @Lazy TaskEventPublisher taskEventPublisher) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.taskEventPublisher = taskEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getWorkspacesForUser(User user) {
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(user.getId());
        return memberships.stream()
                .map(WorkspaceMember::getWorkspace)
                .map(WorkspaceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspaceEntity(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));
    }

    @Transactional
    public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request, User creator) {
        Workspace workspace = new Workspace(request.getName().trim());
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // Creator automatically becomes OWNER
        WorkspaceMember ownerMember = new WorkspaceMember(savedWorkspace, creator, MemberRole.OWNER);
        workspaceMemberRepository.save(ownerMember);

        auditService.record(
                savedWorkspace.getId(),
                creator.getId(),
                creator.getName(),
                AuditAction.WORKSPACE_CREATED,
                "Workspace",
                savedWorkspace.getId(),
                "Created workspace '" + savedWorkspace.getName() + "'"
        );

        return WorkspaceResponse.fromEntity(savedWorkspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getWorkspaceMembers(Long workspaceId, User requester) {
        verifyMembership(workspaceId, requester.getId());
        return workspaceMemberRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(WorkspaceMemberResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkspaceMemberResponse addMember(Long workspaceId, AddMemberRequest request, User actor) {
        verifyRole(workspaceId, actor.getId(), MemberRole.OWNER);

        String email = request.getEmail().trim().toLowerCase();
        User targetUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUser.getId())) {
            throw new ConflictException("User is already a member of this workspace", null);
        }

        Workspace workspace = getWorkspaceEntity(workspaceId);
        WorkspaceMember newMember = new WorkspaceMember(workspace, targetUser, request.getRole());
        WorkspaceMember savedMember = workspaceMemberRepository.save(newMember);

        auditService.record(
                workspaceId,
                actor.getId(),
                actor.getName(),
                AuditAction.MEMBER_ADDED,
                "User",
                targetUser.getId(),
                "Added " + targetUser.getEmail() + " as " + request.getRole()
        );

        // Broadcast real-time WebSocket event to all clients in workspace
        taskEventPublisher.publishMemberUpdated(workspaceId);

        return WorkspaceMemberResponse.fromEntity(savedMember);
    }

    @Transactional
    public void removeMember(Long workspaceId, Long targetUserId, User actor) {
        verifyRole(workspaceId, actor.getId(), MemberRole.OWNER);

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

        workspaceMemberRepository.delete(member);

        auditService.record(
                workspaceId,
                actor.getId(),
                actor.getName(),
                AuditAction.MEMBER_REMOVED,
                "User",
                targetUserId,
                "Removed user " + targetUserId + " from workspace"
        );

        // Broadcast real-time WebSocket event to all clients in workspace
        taskEventPublisher.publishMemberUpdated(workspaceId);
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(Long workspaceId, Long targetUserId, UpdateMemberRoleRequest request, User actor) {
        verifyRole(workspaceId, actor.getId(), MemberRole.OWNER);

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

        MemberRole oldRole = member.getRole();
        member.setRole(request.getRole());
        WorkspaceMember updated = workspaceMemberRepository.save(member);

        auditService.record(
                workspaceId,
                actor.getId(),
                actor.getName(),
                AuditAction.ROLE_CHANGED,
                "User",
                targetUserId,
                "Changed role from " + oldRole + " to " + request.getRole()
        );

        // Broadcast real-time WebSocket event to all clients in workspace
        taskEventPublisher.publishMemberUpdated(workspaceId);

        return WorkspaceMemberResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public MemberRole getMemberRole(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(WorkspaceMember::getRole)
                .orElseThrow(() -> new AccessDeniedException("User is not a member of workspace #" + workspaceId));
    }

    @Transactional(readOnly = true)
    public void verifyMembership(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new AccessDeniedException("User is not a member of workspace #" + workspaceId);
        }
    }

    @Transactional(readOnly = true)
    public void verifyRole(Long workspaceId, Long userId, MemberRole requiredRole) {
        MemberRole userRole = getMemberRole(workspaceId, userId);
        if (requiredRole == MemberRole.OWNER && userRole != MemberRole.OWNER) {
            String userEmail = userRepository.findById(userId).map(User::getEmail).orElse("ID #" + userId);
            throw new AccessDeniedException("Requires OWNER role for this operation. (Authenticated user '" + userEmail + "' has role " + userRole + " in workspace #" + workspaceId + ")");
        }
        if (requiredRole == MemberRole.EDITOR && (userRole != MemberRole.OWNER && userRole != MemberRole.EDITOR)) {
            String userEmail = userRepository.findById(userId).map(User::getEmail).orElse("ID #" + userId);
            throw new AccessDeniedException("Requires OWNER or EDITOR role for this operation. (Authenticated user '" + userEmail + "' has role " + userRole + " in workspace #" + workspaceId + ")");
        }
    }

    @Transactional(readOnly = true)
    public boolean checkDatabaseHealth() {
        try {
            workspaceRepository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
