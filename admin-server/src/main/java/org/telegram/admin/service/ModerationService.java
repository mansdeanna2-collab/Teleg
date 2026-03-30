package org.telegram.admin.service;

import org.telegram.admin.model.ModerationReport;
import org.telegram.admin.repository.ModerationReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ModerationService {

    private final ModerationReportRepository reportRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public ModerationService(ModerationReportRepository reportRepository,
                             AuditLogService auditLogService,
                             UserService userService) {
        this.reportRepository = reportRepository;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    public Page<ModerationReport> getAll(Pageable pageable) {
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<ModerationReport> getByStatus(String status, Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    public ModerationReport getById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
    }

    public ModerationReport createReport(ModerationReport report) {
        return reportRepository.save(report);
    }

    public ModerationReport resolve(Long id, String action, String note, String adminUsername) {
        ModerationReport report = getById(id);
        report.setStatus("RESOLVED");
        report.setActionTaken(action);
        report.setResolutionNote(note);
        report.setResolvedBy(adminUsername);
        report.setResolvedAt(LocalDateTime.now());

        // Apply action on reported user if needed
        if ("BAN".equals(action) && report.getReportedUserId() != null) {
            userService.banUser(report.getReportedUserId(), "Moderation: " + note, null, adminUsername);
        } else if ("MUTE".equals(action) && report.getReportedUserId() != null) {
            userService.restrictUser(report.getReportedUserId(), "Moderation: " + note, adminUsername);
        }

        ModerationReport saved = reportRepository.save(report);
        auditLogService.log(adminUsername, "RESOLVE_REPORT", "REPORT", id.toString(),
                "Resolved report, action: " + action + ", note: " + note, null);
        return saved;
    }

    public ModerationReport dismiss(Long id, String note, String adminUsername) {
        ModerationReport report = getById(id);
        report.setStatus("DISMISSED");
        report.setResolutionNote(note);
        report.setResolvedBy(adminUsername);
        report.setResolvedAt(LocalDateTime.now());
        report.setActionTaken("NONE");
        ModerationReport saved = reportRepository.save(report);
        auditLogService.log(adminUsername, "DISMISS_REPORT", "REPORT", id.toString(),
                "Dismissed report: " + note, null);
        return saved;
    }

    public long countPending() {
        return reportRepository.countByStatus("PENDING");
    }
}
