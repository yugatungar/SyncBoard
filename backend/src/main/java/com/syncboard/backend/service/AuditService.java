package com.syncboard.backend.service;

import com.syncboard.backend.dto.AuditEventResponse;
import com.syncboard.backend.entity.AuditAction;
import com.syncboard.backend.entity.AuditEvent;
import com.syncboard.backend.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(Long workspaceId, Long userId, String userName, AuditAction action, String entityType, Long entityId, String details) {
        try {
            AuditEvent event = new AuditEvent(workspaceId, userId, userName, action, entityType, entityId, details);
            auditEventRepository.save(event);
            log.info("Audit logged: User '{}' ({}) performed {} on {} #{}", userName, userId, action, entityType, entityId);
        } catch (Exception ex) {
            log.error("Failed to save audit event: {}", ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAuditHistory(Long workspaceId, int limit) {
        return auditEventRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, PageRequest.of(0, Math.min(limit, 100)))
                .stream()
                .map(AuditEventResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
