package org.telegram.admin.repository;

import org.telegram.admin.model.ChatGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {
    Optional<ChatGroup> findByGroupId(Long groupId);

    @Query("SELECT g FROM ChatGroup g WHERE " +
           "(:keyword IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%',:keyword,'%'))) " +
           "AND (:status IS NULL OR g.status = :status)")
    Page<ChatGroup> searchGroups(@Param("keyword") String keyword,
                                 @Param("status") String status,
                                 Pageable pageable);

    long countByStatus(String status);
}
