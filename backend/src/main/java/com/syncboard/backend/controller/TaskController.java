package com.syncboard.backend.controller;

import com.syncboard.backend.dto.CreateTaskRequest;
import com.syncboard.backend.dto.TaskResponse;
import com.syncboard.backend.dto.UpdateTaskRequest;
import com.syncboard.backend.entity.User;
import com.syncboard.backend.security.UserPrincipal;
import com.syncboard.backend.service.RateLimitService;
import com.syncboard.backend.service.TaskService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;
    private final RateLimitService rateLimitService;

    public TaskController(TaskService taskService, RateLimitService rateLimitService) {
        this.taskService = taskService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/workspaces/{workspaceId}/tasks")
    public ResponseEntity<List<TaskResponse>> getWorkspaceTasks(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal != null ? principal.getUser() : null;
        List<TaskResponse> tasks = taskService.getTasksByWorkspaceId(workspaceId, user);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/workspaces/{workspaceId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal != null ? principal.getUser() : null;
        if (user != null) {
            rateLimitService.checkRateLimit("ratelimit:task:mutation:" + user.getId(), 30, 60);
        }

        TaskResponse created = taskService.createTask(workspaceId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal != null ? principal.getUser() : null;
        if (user != null) {
            rateLimitService.checkRateLimit("ratelimit:task:mutation:" + user.getId(), 30, 60);
        }

        TaskResponse updated = taskService.updateTask(taskId, request, user);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @RequestParam(required = false) String operationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal != null ? principal.getUser() : null;
        if (user != null) {
            rateLimitService.checkRateLimit("ratelimit:task:mutation:" + user.getId(), 30, 60);
        }

        taskService.deleteTask(taskId, operationId, user);
        return ResponseEntity.noContent().build();
    }
}
