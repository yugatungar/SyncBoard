package com.syncboard.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WorkspaceCreateRequest {

    @NotBlank(message = "Workspace name must not be blank")
    @Size(max = 100, message = "Workspace name must not exceed 100 characters")
    private String name;

    public WorkspaceCreateRequest() {
    }

    public WorkspaceCreateRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
