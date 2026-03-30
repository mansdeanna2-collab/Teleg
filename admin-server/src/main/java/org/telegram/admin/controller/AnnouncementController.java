package org.telegram.admin.controller;

import org.telegram.admin.dto.ApiResponse;
import org.telegram.admin.model.Announcement;
import org.telegram.admin.service.AnnouncementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Announcement>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Announcement> announcements = announcementService.getAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(announcements));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Announcement>> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(announcementService.getById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Announcement>> create(@RequestBody Announcement announcement, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(announcementService.create(announcement, principal.getName())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Announcement>> update(@PathVariable Long id,
                                                             @RequestBody Announcement announcement,
                                                             Principal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(announcementService.update(id, announcement, principal.getName())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivate(@PathVariable Long id, Principal principal) {
        try {
            announcementService.deactivate(id, principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("Announcement deactivated", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<String>> activate(@PathVariable Long id, Principal principal) {
        try {
            announcementService.activate(id, principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("Announcement activated", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
