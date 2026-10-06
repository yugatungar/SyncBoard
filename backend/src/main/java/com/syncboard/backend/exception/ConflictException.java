package com.syncboard.backend.exception;

import com.syncboard.backend.dto.TaskResponse;

/**
 * Thrown when a client sends an update with a stale {@code version}.
 *
 * <p>The exception carries the current server-side {@link TaskResponse} so the
 * {@link com.syncboard.backend.exception.GlobalExceptionHandler} can embed it
 * in the 409 response body, allowing the client to display a conflict UI
 * without making an additional GET request.</p>
 */
public class ConflictException extends RuntimeException {

    private final TaskResponse currentTask;

    public ConflictException(String message, TaskResponse currentTask) {
        super(message);
        this.currentTask = currentTask;
    }

    public TaskResponse getCurrentTask() {
        return currentTask;
    }
}
