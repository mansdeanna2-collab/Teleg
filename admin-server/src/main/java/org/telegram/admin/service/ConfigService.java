package org.telegram.admin.service;

import org.telegram.admin.model.SystemConfig;
import org.telegram.admin.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConfigService {

    private final SystemConfigRepository configRepository;
    private final AuditLogService auditLogService;

    public ConfigService(SystemConfigRepository configRepository, AuditLogService auditLogService) {
        this.configRepository = configRepository;
        this.auditLogService = auditLogService;
    }

    public List<SystemConfig> getAll() {
        return configRepository.findAll();
    }

    public List<SystemConfig> getByCategory(String category) {
        return configRepository.findByCategory(category);
    }

    public Optional<SystemConfig> getByKey(String key) {
        return configRepository.findByConfigKey(key);
    }

    public String getValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    public SystemConfig createOrUpdate(String key, String value, String type, String description,
                                       String category, String adminUsername) {
        SystemConfig config = configRepository.findByConfigKey(key).orElse(new SystemConfig());
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType(type);
        config.setDescription(description);
        config.setCategory(category);
        config.setUpdatedBy(adminUsername);
        SystemConfig saved = configRepository.save(config);
        auditLogService.log(adminUsername, "CONFIG_CHANGE", "CONFIG", key,
                "Updated config: " + key + " = " + value, null);
        return saved;
    }

    public SystemConfig update(Long id, String value, String adminUsername) {
        SystemConfig config = configRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        if (!config.isEditable()) {
            throw new RuntimeException("This configuration is not editable");
        }
        String oldValue = config.getConfigValue();
        config.setConfigValue(value);
        config.setUpdatedBy(adminUsername);
        SystemConfig saved = configRepository.save(config);
        auditLogService.log(adminUsername, "CONFIG_CHANGE", "CONFIG", config.getConfigKey(),
                "Changed from: " + oldValue + " to: " + value, null);
        return saved;
    }

    public void delete(Long id, String adminUsername) {
        SystemConfig config = configRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found"));
        configRepository.delete(config);
        auditLogService.log(adminUsername, "DELETE", "CONFIG", config.getConfigKey(),
                "Deleted config: " + config.getConfigKey(), null);
    }
}
