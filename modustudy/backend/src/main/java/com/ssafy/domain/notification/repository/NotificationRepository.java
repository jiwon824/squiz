package com.ssafy.domain.notification.repository;

import com.ssafy.domain.notification.entity.Notification;
import com.ssafy.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자별 알림 목록 조회 (페이징, 최신순)
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 사용자별 + 타입별 알림 목록 조회 (페이징, 최신순)
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type, Pageable pageable);

    // 사용자별 알림 목록 조회 (전체)
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 사용자 + 알림 ID로 조회 (권한 체크용)
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    // 읽지 않은 알림 수 (전체)
    long countByUserIdAndIsReadFalse(Long userId);

    // 읽지 않은 알림 수 (타입별)
    long countByUserIdAndTypeAndIsReadFalse(Long userId, NotificationType type);

    // 전체 읽음 처리 (벌크 업데이트)
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);

    // 사용자별 알림 전체 삭제
    void deleteByUserId(Long userId);
}
