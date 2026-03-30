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

    public AppUser findByTelegramIdOptional(Long telegramId) {
        return userRepository.findByTelegramId(telegramId).orElse(null);
    }

    public AppUser findByPhoneNumberOptional(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).orElse(null);
    }

    /**
     * Save a user directly, bypassing createOrUpdateUser validation logic.
     * Used by heartbeat to update lastActiveAt for all users including banned ones.
     */
    public AppUser saveUser(AppUser user) {
        return userRepository.save(user);
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
                // Reject registration/update from banned or deleted users
                if ("BANNED".equals(existing.getStatus()) || "DELETED".equals(existing.getStatus())) {
                    throw new RuntimeException("User account is " + existing.getStatus().toLowerCase());
                }
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
        // For new user registration, also try to find by phone number
        // (admin may have pre-created the user with phone but no telegramId)
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            AppUser byPhone = userRepository.findByPhoneNumber(user.getPhoneNumber()).orElse(null);
            if (byPhone != null) {
                if ("BANNED".equals(byPhone.getStatus()) || "DELETED".equals(byPhone.getStatus())) {
                    throw new RuntimeException("User account is " + byPhone.getStatus().toLowerCase());
                }
                // Link the admin-created record to this telegram user
                byPhone.setTelegramId(user.getTelegramId());
                byPhone.setFirstName(user.getFirstName());
                byPhone.setLastName(user.getLastName());
                byPhone.setUsername(user.getUsername());
                byPhone.setLastActiveAt(LocalDateTime.now());
                byPhone.setDeviceInfo(user.getDeviceInfo());
                byPhone.setAppVersion(user.getAppVersion());
                return userRepository.save(byPhone);
            }
        }
        user.setRegisteredAt(LocalDateTime.now());
        user.setLastActiveAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public AppUser createUserByAdmin(AppUser user, String adminUsername) {
        if (user.getTelegramId() != null) {
            if (userRepository.findByTelegramId(user.getTelegramId()).isPresent()) {
                throw new RuntimeException("User with this Telegram ID already exists");
            }
        }
        user.setRegisteredAt(LocalDateTime.now());
        user.setLastActiveAt(LocalDateTime.now());
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }
        AppUser saved = userRepository.save(user);
        auditLogService.log(adminUsername, "CREATE", "USER", saved.getId().toString(),
                "Created user: " + user.getUsername() + " (telegramId: " + user.getTelegramId() + ")", null);
        return saved;
    }

    public AppUser updateUserByAdmin(Long id, AppUser updatedUser, String adminUsername) {
        AppUser existing = getUserById(id);
        if (updatedUser.getFirstName() != null) existing.setFirstName(updatedUser.getFirstName());
        if (updatedUser.getLastName() != null) existing.setLastName(updatedUser.getLastName());
        if (updatedUser.getUsername() != null) existing.setUsername(updatedUser.getUsername());
        if (updatedUser.getPhoneNumber() != null) existing.setPhoneNumber(updatedUser.getPhoneNumber());
        if (updatedUser.getTelegramId() != null) {
            // Ensure no other user has this Telegram ID
            AppUser otherUser = userRepository.findByTelegramId(updatedUser.getTelegramId()).orElse(null);
            if (otherUser != null && !otherUser.getId().equals(id)) {
                throw new RuntimeException("Another user already has this Telegram ID");
            }
            existing.setTelegramId(updatedUser.getTelegramId());
        }
        existing.setPremium(updatedUser.isPremium());
        existing.setBot(updatedUser.isBot());
        AppUser saved = userRepository.save(existing);
        auditLogService.log(adminUsername, "UPDATE", "USER", id.toString(),
                "Updated user: " + existing.getUsername(), null);
        return saved;
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
