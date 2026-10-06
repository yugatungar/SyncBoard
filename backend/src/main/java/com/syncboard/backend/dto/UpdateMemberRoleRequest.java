package com.syncboard.backend.dto;

import com.syncboard.backend.entity.MemberRole;
import jakarta.validation.constraints.NotNull;

public class UpdateMemberRoleRequest {

    @NotNull(message = "Role is required")
    private MemberRole role;

    public UpdateMemberRoleRequest() {
    }

    public UpdateMemberRoleRequest(MemberRole role) {
        this.role = role;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }
}
