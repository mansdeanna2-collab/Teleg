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

    public ClientApiController(UserService userService, ModerationService moderationService,
                                AnnouncementService announcementService, ConfigService configService) {
        this.userService = userService;
        this.moderationService = moderationService;
        this.announcementService = announcementService;
        this.configService = configService;
    }

    /**
     * Register/update user info when client connects.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AppUser>> registerUser(@RequestBody AppUser user) {
        try {
            AppUser saved = userService.createOrUpdateUser(user);
            return ResponseEntity.ok(ApiResponse.ok(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Report heartbeat / user activity.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> heartbeat(
            @RequestParam Long telegramId) {
        try {
            AppUser user = userService.getUserByTelegramId(telegramId);
            user.setLastActiveAt(java.time.LocalDateTime.now());
            userService.createOrUpdateUser(user);

            // Check if user is banned
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("status", user.getStatus());
            if ("BANNED".equals(user.getStatus())) {
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
     */
    @GetMapping("/user-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStatus(
            @RequestParam Long telegramId) {
        try {
            AppUser user = userService.getUserByTelegramId(telegramId);
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("status", user.getStatus());
            response.put("banReason", user.getBanReason());
            response.put("banExpiresAt", user.getBanExpiresAt());
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (RuntimeException e) {
            Map<String, Object> defaultResponse = new java.util.HashMap<>();
            defaultResponse.put("status", "ACTIVE");
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
