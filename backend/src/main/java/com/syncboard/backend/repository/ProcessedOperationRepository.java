package com.syncboard.backend.repository;

import com.syncboard.backend.entity.ProcessedOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedOperationRepository extends JpaRepository<ProcessedOperation, Long> {

    boolean existsByOperationId(String operationId);
}
