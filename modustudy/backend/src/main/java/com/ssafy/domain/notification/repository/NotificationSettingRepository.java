package com.ssafy.domain.notification.repository;

import com.ssafy.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    // 사용자별 알림 설정 목록 조회
    List<NotificationSetting> findByUserId(Long userId);

    // 사용자 + 알림 타입으로 설정 조회
    Optional<NotificationSetting> findByUserIdAndNotificationType(Long userId, String notificationType);

    // 사용자별 알림 설정 전체 삭제
    void deleteByUserId(Long userId);
}