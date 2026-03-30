package org.telegram.admin.controller;

import org.telegram.admin.dto.ApiResponse;
import org.telegram.admin.model.Channel;
import org.telegram.admin.service.ChannelService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Channel>>> listChannels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Channel> channels = channelService.searchChannels(keyword, status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(channels));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Channel>> getChannel(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(channelService.getById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Channel>> createChannel(@RequestBody Channel channel) {
        return ResponseEntity.ok(ApiResponse.ok(channelService.create(channel)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Channel>> updateChannel(@PathVariable Long id, @RequestBody Channel channel) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(channelService.update(id, channel)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<Channel>> suspend(@PathVariable Long id, Principal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Channel suspended", channelService.suspend(id, principal.getName())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Channel>> activate(@PathVariable Long id, Principal principal) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Channel activated", channelService.activate(id, principal.getName())));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id, Principal principal) {
        try {
            channelService.delete(id, principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("Channel deleted", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
