package com.mos.common.audit.service;

import com.mos.common.audit.dto.AuditLogResponse;
import com.mos.common.audit.entity.AuditLog;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog log(AuditLogEntry entry) {
        AuditLog auditLog = AuditLog.builder()
                .userId(entry.userId())
                .gameSessionId(entry.gameSessionId())
                .action(entry.action())
                .entityType(entry.entityType())
                .entityId(entry.entityId())
                .description(entry.description())
                .metadata(entry.metadata() != null ? new HashMap<>(entry.metadata()) : new HashMap<>())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit action={} userId={} sessionId={} entityType={} entityId={} description={}",
                entry.action(), entry.userId(), entry.gameSessionId(),
                entry.entityType(), entry.entityId(), entry.description());
        return saved;
    }

    @Transactional
    public AuditLog log(
            UUID userId,
            UUID gameSessionId,
            AuditAction action,
            String entityType,
            String entityId,
            String description
    ) {
        return log(new AuditLogEntry(userId, gameSessionId, action, entityType, entityId, description, Map.of()));
    }

    @Transactional
    public AuditLog log(
            UUID userId,
            UUID gameSessionId,
            AuditAction action,
            String entityType,
            String entityId,
            String description,
            Map<String, Object> metadata
    ) {
        return log(new AuditLogEntry(userId, gameSessionId, action, entityType, entityId, description, metadata));
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findLogs(
            UUID gameSessionId,
            UUID userId,
            AuditAction action,
            Pageable pageable
    ) {
        return auditLogRepository.findFiltered(gameSessionId, userId, action, pageable)
                .map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getById(UUID id) {
        return auditLogRepository.findById(id)
                .map(AuditLogResponse::from)
                .orElseThrow(() -> new com.mos.common.exception.BusinessException("Audit log not found"));
    }
}
