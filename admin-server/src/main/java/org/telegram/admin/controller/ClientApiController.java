package org.telegram.admin.controller;

import org.telegram.admin.dto.ApiResponse;
import org.telegram.admin.model.AppUser;
import org.telegram.admin.model.ModerationReport;
import org.telegram.admin.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Client API - endpoints for the Android Telegram client to communicate with admin server.
 * These endpoints do NOT require admin authentication (they use client tokens).
 */
@RestController
@RequestMapping("/api/client")
public class ClientApiController {

    private final UserService userService;
    private final ModerationService moderationService;
    private final AnnouncementService announcementService;
    private final ConfigService configService;
    private final AuditLogService auditLogService;

    public ClientApiController(UserService userService, ModerationService moderationService,
                                AnnouncementService announcementService, ConfigService configService,
                                AuditLogService auditLogService) {
        this.userService = userService;
        this.moderationService = moderationService;
        this.announcementService = announcementService;
        this.configService = configService;
        this.auditLogService = auditLogService;
    }

    /**
     * Register/update user info when client connects.
     * Checks allow_registration config and rejects banned/deleted users.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AppUser>> registerUser(@RequestBody AppUser user) {
        try {
            // Check if this is a new user (not found by telegramId or phone)
            boolean isNewUser = false;
            if (user.getTelegramId() != null) {
                AppUser existing = userService.findByTelegramIdOptional(user.getTelegramId());
                isNewUser = (existing == null);
            }
            // Check if new registration is allowed
            if (isNewUser) {
                String allowReg = configService.getValue("allow_registration", "true");
                if ("false".equalsIgnoreCase(allowReg)) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("New user registration is currently disabled"));
                }
            }
            AppUser saved = userService.createOrUpdateUser(user);
            if (isNewUser) {
                auditLogService.log("CLIENT", "REGISTER", "USER", saved.getId().toString(),
                        "New user registered via client: " + saved.getUsername() +
                        " (telegramId: " + saved.getTelegramId() + ")", null);
            }
            return ResponseEntity.ok(ApiResponse.ok(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Report heartbeat / user activity.
     * Returns ban/restriction status for enforcement on client side.
     * Uses direct save to avoid createOrUpdateUser's ban rejection logic,
     * since heartbeat must return ban status rather than reject banned users.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> heartbeat(
            @RequestParam Long telegramId) {
        try {
            AppUser user = userService.findByTelegramIdOptional(telegramId);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
            }

            // Auto-unban if ban has expired
            if ("BANNED".equals(user.getStatus()) && user.getBanExpiresAt() != null
                    && user.getBanExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                user.setStatus("ACTIVE");
                user.setBanReason(null);
                user.setBanExpiresAt(null);
            }

            // Update last active time directly (bypass createOrUpdateUser ban check)
            user.setLastActiveAt(java.time.LocalDateTime.now());
            userService.saveUser(user);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("status", user.getStatus());
            if ("BANNED".equals(user.getStatus()) || "RESTRICTED".equals(user.getStatus())) {
                response.put("banReason", user.getBanReason());
                response.put("banExpiresAt", user.getBanExpiresAt());
            }
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Check user status (for ban enforcement).
     * Supports lookup by telegramId or phoneNumber.
     * Returns proper error for unknown users instead of defaulting to ACTIVE.
     */
    @GetMapping("/user-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStatus(
            @RequestParam(required = false) Long telegramId,
            @RequestParam(required = false) String phoneNumber) {
        try {
            AppUser user = null;
            if (telegramId != null) {
                user = userService.findByTelegramIdOptional(telegramId);
            }
            if (user == null && phoneNumber != null && !phoneNumber.isEmpty()) {
                user = userService.findByPhoneNumberOptional(phoneNumber);
            }
            if (user == null) {
                // Unknown user: return UNREGISTERED status so client knows to register
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("status", "UNREGISTERED");
                return ResponseEntity.ok(ApiResponse.ok(response));
            }
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("status", user.getStatus());
            response.put("banReason", user.getBanReason());
            response.put("banExpiresAt", user.getBanExpiresAt());
            response.put("telegramId", user.getTelegramId());
            response.put("username", user.getUsername());
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (RuntimeException e) {
            Map<String, Object> defaultResponse = new java.util.HashMap<>();
            defaultResponse.put("status", "UNREGISTERED");
            return ResponseEntity.ok(ApiResponse.ok(defaultResponse));
        }
    }

    /**
     * Submit a report from the client.
     */
    @PostMapping("/report")
    public ResponseEntity<ApiResponse<ModerationReport>> submitReport(@RequestBody ModerationReport report) {
        try {
            ModerationReport saved = moderationService.createReport(report);
            return ResponseEntity.ok(ApiResponse.ok("Report submitted", saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get active announcements for the client.
     */
    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<org.telegram.admin.model.Announcement>>> getAnnouncements() {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.getActiveAnnouncements()));
    }

    /**
     * Get a specific configuration value for the client.
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfig(@RequestParam String key) {
        String value = configService.getValue(key, null);
        Map<String, String> response = new java.util.HashMap<>();
        response.put("key", key);
        response.put("value", value);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get all client-relevant configurations.
     */
    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<org.telegram.admin.model.SystemConfig>>> getAllConfigs() {
        return ResponseEntity.ok(ApiResponse.ok(configService.getByCategory("CLIENT")));
    }
}
