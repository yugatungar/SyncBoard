package com.syncboard.backend.controller;

import com.syncboard.backend.dto.HealthResponse;
import com.syncboard.backend.service.WorkspaceService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final WorkspaceService workspaceService;
    private final StringRedisTemplate redisTemplate;

    public HealthController(WorkspaceService workspaceService, StringRedisTemplate redisTemplate) {
        this.workspaceService = workspaceService;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {
        boolean dbOk = workspaceService.checkDatabaseHealth();
        boolean redisOk = checkRedisHealth();

        HealthResponse response = new HealthResponse(
                "UP",
                dbOk ? "UP" : "DOWN",
                redisOk ? "UP" : "DOWN"
        );
        return ResponseEntity.ok(response);
    }

    private boolean checkRedisHealth() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            return false;
        }
    }
}
