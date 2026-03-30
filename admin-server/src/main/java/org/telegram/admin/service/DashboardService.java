package org.telegram.admin.service;

import org.telegram.admin.dto.DashboardStats;
import org.telegram.admin.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DashboardService {

    private final AppUserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ChatGroupRepository groupRepository;
    private final ModerationReportRepository reportRepository;
    private final AnnouncementRepository announcementRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardService(AppUserRepository userRepository,
                            ChannelRepository channelRepository,
                            ChatGroupRepository groupRepository,
                            ModerationReportRepository reportRepository,
                            AnnouncementRepository announcementRepository,
                            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.groupRepository = groupRepository;
        this.reportRepository = reportRepository;
        this.announcementRepository = announcementRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);

        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.countActiveUsersSince(last24h));
        stats.setBannedUsers(userRepository.countByStatus("BANNED"));
        stats.setNewUsersToday(userRepository.countNewUsersSince(todayStart));
        stats.setPremiumUsers(userRepository.countByPremium(true));
        stats.setBotUsers(userRepository.countByBot(true));

        stats.setTotalChannels(channelRepository.count());
        stats.setActiveChannels(channelRepository.countByStatus("ACTIVE"));

        stats.setTotalGroups(groupRepository.count());
        stats.setActiveGroups(groupRepository.countByStatus("ACTIVE"));

        stats.setPendingReports(reportRepository.countByStatus("PENDING"));
        stats.setActiveAnnouncements(announcementRepository.countByActive(true));
        stats.setTodayAuditActions(auditLogRepository.countByCreatedAtAfter(todayStart));

        return stats;
    }
}
