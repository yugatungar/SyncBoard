package com.syncboard.backend.service;

import com.syncboard.backend.entity.TaskStatus;
import com.syncboard.backend.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.Map;

/**
 * Validates task status transitions against a fixed set of business rules.
 *
 * <p>Rules (kept deliberately simple):</p>
 * <ul>
 *   <li>TODO       → IN_PROGRESS  ✓</li>
 *   <li>IN_PROGRESS → DONE        ✓</li>
 *   <li>IN_PROGRESS → TODO        ✓ (can move back)</li>
 *   <li>same       → same         ✓ (no-op, always OK)</li>
 *   <li>TODO       → DONE         ✗ (must pass through IN_PROGRESS)</li>
 *   <li>DONE       → anything     ✗ (DONE is terminal)</li>
 * </ul>
 */
@Component
public class TaskStateTransitionValidator {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = Map.of(
            TaskStatus.TODO,        Set.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS),
            TaskStatus.IN_PROGRESS, Set.of(TaskStatus.IN_PROGRESS, TaskStatus.TODO, TaskStatus.DONE),
            TaskStatus.DONE,        Set.of(TaskStatus.DONE)
    );

    /**
     * Validates that {@code from → to} is a permitted transition.
     *
     * @throws InvalidStateTransitionException if the transition is not allowed
     */
    public void validate(TaskStatus from, TaskStatus to) {
        Set<TaskStatus> allowed = ALLOWED.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }
}
