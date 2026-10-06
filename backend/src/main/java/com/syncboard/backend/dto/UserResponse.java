package com.syncboard.backend.dto;

import com.syncboard.backend.entity.User;

import java.time.Instant;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Instant createdAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String name, String email, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
    }

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
