package org.telegram.admin.service;

import org.telegram.admin.model.AuditLog;
import org.telegram.admin.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String adminUsername, String action, String targetType, String targetId, String details, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setAdminUsername(adminUsername);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<AuditLog> getLogsByAdmin(String adminUsername, Pageable pageable) {
        return auditLogRepository.findByAdminUsernameOrderByCreatedAtDesc(adminUsername, pageable);
    }
}
