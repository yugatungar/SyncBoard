package com.syncboard.backend.service;

import com.syncboard.backend.dto.CreateTaskRequest;
import com.syncboard.backend.dto.TaskResponse;
import com.syncboard.backend.dto.UpdateTaskRequest;
import com.syncboard.backend.entity.AuditAction;
import com.syncboard.backend.entity.MemberRole;
import com.syncboard.backend.entity.ProcessedOperation;
import com.syncboard.backend.entity.Task;
import com.syncboard.backend.entity.User;
import com.syncboard.backend.entity.Workspace;
import com.syncboard.backend.exception.ConflictException;
import com.syncboard.backend.exception.DuplicateOperationException;
import com.syncboard.backend.exception.ResourceNotFoundException;
import com.syncboard.backend.repository.ProcessedOperationRepository;
import com.syncboard.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final WorkspaceService workspaceService;
    private final TaskEventPublisher taskEventPublisher;
    private final ProcessedOperationRepository processedOperationRepository;
    private final TaskStateTransitionValidator stateTransitionValidator;
    private final AuditService auditService;

    public TaskService(TaskRepository taskRepository,
                        WorkspaceService workspaceService,
                        TaskEventPublisher taskEventPublisher,
                        ProcessedOperationRepository processedOperationRepository,
                        TaskStateTransitionValidator stateTransitionValidator,
                        AuditService auditService) {
        this.taskRepository = taskRepository;
        this.workspaceService = workspaceService;
        this.taskEventPublisher = taskEventPublisher;
        this.processedOperationRepository = processedOperationRepository;
        this.stateTransitionValidator = stateTransitionValidator;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByWorkspaceId(Long workspaceId, User user) {
        if (user != null) {
            workspaceService.verifyMembership(workspaceId, user.getId());
        } else {
            workspaceService.getWorkspaceEntity(workspaceId);
        }

        return taskRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse createTask(Long workspaceId, CreateTaskRequest request, User actor) {
        checkOperationId(request.getOperationId());

        if (actor != null) {
            workspaceService.verifyRole(workspaceId, actor.getId(), MemberRole.EDITOR);
        }

        Workspace workspace = workspaceService.getWorkspaceEntity(workspaceId);

        Task task = new Task(
                workspace,
                request.getTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getPriority()
        );

        Task saved = taskRepository.saveAndFlush(task);
        recordOperation(request.getOperationId());
        TaskResponse response = TaskResponse.fromEntity(saved);

        // Publish WebSocket event AFTER database operation succeeds
        taskEventPublisher.publishTaskCreated(workspaceId, response);

        // Audit log
        if (actor != null) {
            auditService.record(
                    workspaceId,
                    actor.getId(),
                    actor.getName(),
                    AuditAction.TASK_CREATED,
                    "Task",
                    saved.getId(),
                    "Created task '" + saved.getTitle() + "'"
            );
        }

        return response;
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, User actor) {
        checkOperationId(request.getOperationId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        Long workspaceId = task.getWorkspace().getId();
        if (actor != null) {
            workspaceService.verifyRole(workspaceId, actor.getId(), MemberRole.EDITOR);
        }

        // Optimistic concurrency check: if client supplied a version, compare with current
        if (request.getVersion() != null && !request.getVersion().equals(task.getVersion())) {
            throw new ConflictException(
                    "Task version mismatch. Server version is " + task.getVersion() + ", client version is " + request.getVersion(),
                    TaskResponse.fromEntity(task)
            );
        }

        // Validate state transition if status is changing
        if (request.getStatus() != task.getStatus()) {
            stateTransitionValidator.validate(task.getStatus(), request.getStatus());
        }

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());

        Task updated = taskRepository.saveAndFlush(task);
        recordOperation(request.getOperationId());
        TaskResponse response = TaskResponse.fromEntity(updated);

        // Publish WebSocket event AFTER database operation succeeds
        taskEventPublisher.publishTaskUpdated(workspaceId, response);

        // Audit log
        if (actor != null) {
            auditService.record(
                    workspaceId,
                    actor.getId(),
                    actor.getName(),
                    AuditAction.TASK_UPDATED,
                    "Task",
                    updated.getId(),
                    "Updated task '" + updated.getTitle() + "' status to " + updated.getStatus()
            );
        }

        return response;
    }

    @Transactional
    public void deleteTask(Long taskId, String operationId, User actor) {
        checkOperationId(operationId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        Long workspaceId = task.getWorkspace().getId();
        if (actor != null) {
            workspaceService.verifyRole(workspaceId, actor.getId(), MemberRole.EDITOR);
        }

        String taskTitle = task.getTitle();
        taskRepository.delete(task);
        recordOperation(operationId);

        // Publish WebSocket event AFTER database operation succeeds
        taskEventPublisher.publishTaskDeleted(workspaceId, taskId);

        // Audit log
        if (actor != null) {
            auditService.record(
                    workspaceId,
                    actor.getId(),
                    actor.getName(),
                    AuditAction.TASK_DELETED,
                    "Task",
                    taskId,
                    "Deleted task '" + taskTitle + "'"
            );
        }
    }

    private void checkOperationId(String operationId) {
        if (operationId != null && !operationId.trim().isEmpty()) {
            if (processedOperationRepository.existsByOperationId(operationId.trim())) {
                throw new DuplicateOperationException(operationId.trim());
            }
        }
    }

    private void recordOperation(String operationId) {
        if (operationId != null && !operationId.trim().isEmpty()) {
            processedOperationRepository.save(new ProcessedOperation(operationId.trim()));
        }
    }
}
