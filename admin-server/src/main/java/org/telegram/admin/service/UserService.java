package org.telegram.admin.service;

import org.telegram.admin.model.AppUser;
import org.telegram.admin.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final AuditLogService auditLogService;

    public UserService(AppUserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public Page<AppUser> searchUsers(String keyword, String status, Pageable pageable) {
        return userRepository.searchUsers(keyword, status, pageable);
    }

    public AppUser getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public AppUser getUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public AppUser banUser(Long id, String reason, LocalDateTime expiresAt, String adminUsername) {
        AppUser user = getUserById(id);
        user.setStatus("BANNED");
        user.setBanReason(reason);
        user.setBanExpiresAt(expiresAt);
        AppUser saved = userRepository.save(user);
        auditLogService.log(adminUsername, "BAN", "USER", id.toString(),
                "Banned user: " + user.getUsername() + ", reason: " + reason, null);
        return saved;
    }

    public AppUser unbanUser(Long id, String adminUsername) {
        AppUser user = getUserById(id);
        user.setStatus("ACTIVE");
        user.setBanReason(null);
        user.setBanExpiresAt(null);
        AppUser saved = userRepository.save(user);
        auditLogService.log(adminUsername, "UNBAN", "USER", id.toString(),
                "Unbanned user: " + user.getUsername(), null);
        return saved;
    }

    public AppUser restrictUser(Long id, String reason, String adminUsername) {
        AppUser user = getUserById(id);
        user.setStatus("RESTRICTED");
        user.setBanReason(reason);
        AppUser saved = userRepository.save(user);
        auditLogService.log(adminUsername, "RESTRICT", "USER", id.toString(),
                "Restricted user: " + user.getUsername(), null);
        return saved;
    }

    public AppUser createOrUpdateUser(AppUser user) {
        if (user.getTelegramId() != null) {
            AppUser existing = userRepository.findByTelegramId(user.getTelegramId()).orElse(null);
            if (existing != null) {
                existing.setFirstName(user.getFirstName());
                existing.setLastName(user.getLastName());
                existing.setUsername(user.getUsername());
                existing.setPhoneNumber(user.getPhoneNumber());
                existing.setLastActiveAt(LocalDateTime.now());
                existing.setDeviceInfo(user.getDeviceInfo());
                existing.setAppVersion(user.getAppVersion());
                return userRepository.save(existing);
            }
        }
        user.setRegisteredAt(LocalDateTime.now());
        user.setLastActiveAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public void deleteUser(Long id, String adminUsername) {
        AppUser user = getUserById(id);
        user.setStatus("DELETED");
        userRepository.save(user);
        auditLogService.log(adminUsername, "DELETE", "USER", id.toString(),
                "Deleted user: " + user.getUsername(), null);
    }

    public long countByStatus(String status) {
        return userRepository.countByStatus(status);
    }

    public long countPremiumUsers() {
        return userRepository.countByPremium(true);
    }

    public long countBotUsers() {
        return userRepository.countByBot(true);
    }

    public long countActiveUsersSince(LocalDateTime since) {
        return userRepository.countActiveUsersSince(since);
    }

    public long countNewUsersSince(LocalDateTime since) {
        return userRepository.countNewUsersSince(since);
    }
}
