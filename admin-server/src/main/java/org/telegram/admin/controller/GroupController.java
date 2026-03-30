package org.telegram.admin.controller;

import org.telegram.admin.dto.ApiResponse;
import org.telegram.admin.model.ChatGroup;
import org.telegram.admin.service.GroupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ChatGroup>>> listGroups(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (keyword != null && keyword.isEmpty()) keyword = null;
        if (status != null && status.isEmpty()) status = null;
        Page<ChatGroup> groups = groupService.searchGroups(keyword, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(groups));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChatGroup>> getGroup(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(groupService.getById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatGroup>> createGroup(@RequestBody ChatGroup group) {
        return ResponseEntity.ok(ApiResponse.ok(groupService.create(group)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChatGroup>> updateGroup(@PathVariable Long id, @RequestBody ChatGroup group) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(groupService.update(id, group)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<ChatGroup>> suspend(@PathVariable Long id, Principal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Group suspended", groupService.suspend(id, principal.getName())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ChatGroup>> activate(@PathVariable Long id, Principal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Group activated", groupService.activate(id, principal.getName())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id, Principal principal) {
        try {
            groupService.delete(id, principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("Group deleted", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
