package org.telegram.admin.controller;

import org.telegram.admin.dto.ApiResponse;
import org.telegram.admin.model.AuditLog;
import org.telegram.admin.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLog>>> list(
            @RequestParam(required = false) String admin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> logs;
        if (admin != null) {
            logs = auditLogService.getLogsByAdmin(admin, PageRequest.of(page, size));
        } else {
            logs = auditLogService.getAllLogs(PageRequest.of(page, size));
        }
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
