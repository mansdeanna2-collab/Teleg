package org.telegram.admin.controller;

import org.telegram.admin.dto.ApiResponse;
import org.telegram.admin.model.AppUser;
import org.telegram.admin.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AppUser>>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AppUser> users = userService.searchUsers(keyword, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppUser>> getUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<ApiResponse<AppUser>> banUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        try {
            String reason = body.getOrDefault("reason", "Banned by admin");
            String expiresStr = body.get("expiresAt");
            LocalDateTime expiresAt = expiresStr != null ? LocalDateTime.parse(expiresStr) : null;
            AppUser user = userService.banUser(id, reason, expiresAt, principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("User banned", user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<ApiResponse<AppUser>> unbanUser(@PathVariable Long id, Principal principal) {
        try {
            AppUser user = userService.unbanUser(id, principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("User unbanned", user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/restrict")
    public ResponseEntity<ApiResponse<AppUser>> restrictUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        try {
            String reason = body.getOrDefault("reason", "Restricted by admin");
            AppUser user = userService.restrictUser(id, reason, principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("User restricted", user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id, Principal principal) {
        try {
            userService.deleteUser(id, principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
