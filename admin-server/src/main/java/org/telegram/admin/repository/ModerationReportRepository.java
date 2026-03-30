package org.telegram.admin.repository;

import org.telegram.admin.model.ModerationReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationReportRepository extends JpaRepository<ModerationReport, Long> {
    Page<ModerationReport> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Page<ModerationReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(String status);
}
