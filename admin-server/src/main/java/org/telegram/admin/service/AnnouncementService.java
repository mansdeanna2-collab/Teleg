package org.telegram.admin.service;

import org.telegram.admin.model.Announcement;
import org.telegram.admin.repository.AnnouncementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AuditLogService auditLogService;

    public AnnouncementService(AnnouncementRepository announcementRepository, AuditLogService auditLogService) {
        this.announcementRepository = announcementRepository;
        this.auditLogService = auditLogService;
    }

    public Page<Announcement> getAll(Pageable pageable) {
        return announcementRepository.findAll(pageable);
    }

    public Page<Announcement> getActive(Pageable pageable) {
        return announcementRepository.findByActiveOrderByCreatedAtDesc(true, pageable);
    }

    public List<Announcement> getActiveAnnouncements() {
        return announcementRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public Announcement getById(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
    }

    public Announcement create(Announcement announcement, String adminUsername) {
        announcement.setCreatedBy(adminUsername);
        announcement.setPublishedAt(LocalDateTime.now());
        Announcement saved = announcementRepository.save(announcement);
        auditLogService.log(adminUsername, "CREATE", "ANNOUNCEMENT", saved.getId().toString(),
                "Created announcement: " + announcement.getTitle(), null);
        return saved;
    }

    public Announcement update(Long id, Announcement announcement, String adminUsername) {
        Announcement existing = getById(id);
        existing.setTitle(announcement.getTitle());
        existing.setContent(announcement.getContent());
        existing.setType(announcement.getType());
        existing.setPriority(announcement.getPriority());
        existing.setTargetAudience(announcement.getTargetAudience());
        existing.setExpiresAt(announcement.getExpiresAt());
        Announcement saved = announcementRepository.save(existing);
        auditLogService.log(adminUsername, "UPDATE", "ANNOUNCEMENT", id.toString(),
                "Updated announcement: " + announcement.getTitle(), null);
        return saved;
    }

    public void deactivate(Long id, String adminUsername) {
        Announcement announcement = getById(id);
        announcement.setActive(false);
        announcementRepository.save(announcement);
        auditLogService.log(adminUsername, "DEACTIVATE", "ANNOUNCEMENT", id.toString(),
                "Deactivated announcement: " + announcement.getTitle(), null);
    }

    public void activate(Long id, String adminUsername) {
        Announcement announcement = getById(id);
        announcement.setActive(true);
        announcementRepository.save(announcement);
        auditLogService.log(adminUsername, "ACTIVATE", "ANNOUNCEMENT", id.toString(),
                "Activated announcement: " + announcement.getTitle(), null);
    }

    public long countActive() {
        return announcementRepository.countByActive(true);
    }
}
