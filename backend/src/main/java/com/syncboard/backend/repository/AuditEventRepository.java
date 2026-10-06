package com.syncboard.backend.repository;

import com.syncboard.backend.entity.AuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);
}
