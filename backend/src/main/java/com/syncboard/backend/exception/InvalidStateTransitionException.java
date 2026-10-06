package com.syncboard.backend.exception;

import com.syncboard.backend.entity.TaskStatus;

/**
 * Thrown when a requested task status transition is not allowed by business rules.
 *
 * <p>Allowed transitions:</p>
 * <ul>
 *   <li>TODO → IN_PROGRESS</li>
 *   <li>IN_PROGRESS → DONE</li>
 *   <li>IN_PROGRESS → TODO (move back)</li>
 *   <li>same → same (no-op, always allowed)</li>
 * </ul>
 * <p>DONE is a terminal state — no outgoing transitions are permitted.</p>
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(TaskStatus from, TaskStatus to) {
        super("Invalid task state transition: " + from + " → " + to);
    }
}
