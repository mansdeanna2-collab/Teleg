package org.telegram.admin.repository;

import org.telegram.admin.model.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Page<Announcement> findByActiveOrderByCreatedAtDesc(boolean active, Pageable pageable);
    List<Announcement> findByActiveTrueOrderByCreatedAtDesc();
    long countByActive(boolean active);
}
