package com.syncboard.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.persistence.Index;
import java.time.Instant;

@Entity
@Table(
    name = "processed_operation",
    indexes = @Index(name = "idx_proc_op_id", columnList = "operation_id")
)
public class ProcessedOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", unique = true, nullable = false)
    private String operationId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedOperation() {
    }

    public ProcessedOperation(String operationId) {
        this.operationId = operationId;
        this.processedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
