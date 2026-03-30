package org.telegram.admin.controller;

import org.telegram.admin.dto.ApiResponse;
import org.telegram.admin.model.ModerationReport;
import org.telegram.admin.service.ModerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ModerationReport>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ModerationReport> reports;
        if (status != null) {
            reports = moderationService.getByStatus(status,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            reports = moderationService.getAll(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ModerationReport>> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(moderationService.getById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<ModerationReport>> resolve(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        try {
            ModerationReport report = moderationService.resolve(id,
                    body.getOrDefault("action", "NONE"),
                    body.getOrDefault("note", ""),
                    principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("Report resolved", report));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<ModerationReport>> dismiss(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        try {
            ModerationReport report = moderationService.dismiss(id,
                    body.getOrDefault("note", ""),
                    principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("Report dismissed", report));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
