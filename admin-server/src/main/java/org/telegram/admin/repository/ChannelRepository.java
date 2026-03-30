package org.telegram.admin.repository;

import org.telegram.admin.model.Channel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    Optional<Channel> findByChannelId(Long channelId);

    @Query("SELECT c FROM Channel c WHERE " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           "OR LOWER(c.username) LIKE LOWER(CONCAT('%',:keyword,'%'))) " +
           "AND (:status IS NULL OR c.status = :status)")
    Page<Channel> searchChannels(@Param("keyword") String keyword,
                                 @Param("status") String status,
                                 Pageable pageable);

    long countByStatus(String status);
}
