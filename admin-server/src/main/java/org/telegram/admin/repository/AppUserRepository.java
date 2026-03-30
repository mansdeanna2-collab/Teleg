package org.telegram.admin.repository;

import org.telegram.admin.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByTelegramId(Long telegramId);
    Optional<AppUser> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM AppUser u WHERE " +
           "(:keyword IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           "OR u.phoneNumber LIKE CONCAT('%',:keyword,'%')) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<AppUser> searchUsers(@Param("keyword") String keyword,
                              @Param("status") String status,
                              Pageable pageable);

    long countByStatus(String status);

    long countByPremium(boolean premium);

    long countByBot(boolean bot);

    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.lastActiveAt >= :since")
    long countActiveUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.registeredAt >= :since")
    long countNewUsersSince(@Param("since") LocalDateTime since);
}
