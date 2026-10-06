package com.syncboard.backend.exception;

/**
 * Thrown when an {@code operationId} is already present in the
 * {@code processed_operation} table, meaning this request has already been
 * successfully processed.
 *
 * <p>This is mapped to HTTP 200 OK (not an error) by the
 * {@link GlobalExceptionHandler} — idempotent retries should appear successful
 * to the caller.</p>
 */
public class DuplicateOperationException extends RuntimeException {

    public DuplicateOperationException(String operationId) {
        super("Operation already processed: " + operationId);
    }
}
