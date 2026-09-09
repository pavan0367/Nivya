package com.nivya.audit.service;

import com.nivya.audit.entity.AuditLog;
import com.nivya.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for immutable security and audit logging.
 * Captures authentication successes/failures, token operations, device access violations,
 * and pairing security events.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists an audit log entry in a new transaction to ensure audit trail survival
     * even if the outer business transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logEvent(Long userId, String action, String details, String ipAddress) {
        try {
            AuditLog entry = new AuditLog(userId, action, details, ipAddress);
            entry = auditLogRepository.save(entry);
            log.info("AUDIT [{}]: user={} ip={} details={}", action, userId, ipAddress, details);
            return entry;
        } catch (Exception e) {
            log.error("Failed to persist audit log for action {}: {}", action, e.getMessage());
            return null;
        }
    }
}
