package org.telegram.admin.repository;

import org.telegram.admin.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    Optional<SystemConfig> findByConfigKey(String configKey);
    List<SystemConfig> findByCategory(String category);
    boolean existsByConfigKey(String configKey);
}
