package org.telegram.admin.controller;

import org.telegram.admin.dto.ApiResponse;
import org.telegram.admin.model.SystemConfig;
import org.telegram.admin.service.ConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configs")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(configService.getAll()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.ok(configService.getByCategory(category)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemConfig>> create(@RequestBody Map<String, String> body, Principal principal) {
        SystemConfig config = configService.createOrUpdate(
                body.get("configKey"),
                body.get("configValue"),
                body.getOrDefault("configType", "STRING"),
                body.get("description"),
                body.getOrDefault("category", "GENERAL"),
                principal.getName()
        );
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemConfig>> update(@PathVariable Long id,
                                                             @RequestBody Map<String, String> body,
                                                             Principal principal) {
        try {
            SystemConfig config = configService.update(id, body.get("configValue"), principal.getName());
            return ResponseEntity.ok(ApiResponse.ok(config));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id, Principal principal) {
        try {
            configService.delete(id, principal.getName());
            return ResponseEntity.ok(ApiResponse.ok("Config deleted", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
