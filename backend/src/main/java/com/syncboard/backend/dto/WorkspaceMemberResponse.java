package com.syncboard.backend.dto;

import com.syncboard.backend.entity.MemberRole;
import com.syncboard.backend.entity.WorkspaceMember;

import java.time.Instant;

public class WorkspaceMemberResponse {

    private Long id;
    private UserResponse user;
    private MemberRole role;
    private Instant joinedAt;

    public WorkspaceMemberResponse() {
    }

    public WorkspaceMemberResponse(Long id, UserResponse user, MemberRole role, Instant joinedAt) {
        this.id = id;
        this.user = user;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public static WorkspaceMemberResponse fromEntity(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
                member.getId(),
                UserResponse.fromEntity(member.getUser()),
                member.getRole(),
                member.getJoinedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public UserResponse getUser() {
        return user;
    }

    public MemberRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
