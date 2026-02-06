package com.ssafy.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 알림 설정 엔티티
 */
 @Entity
 @Table(name = "notification_setting",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_type",
                columnNames = {"user_id", "notification_type"}))
                @Getter
                @NoArgsConstructor(access = AccessLevel.PROTECTED)
                @AllArgsConstructor
                @Builder
                public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ========== 비즈니스 메서드 ==========

    /**
     * 알림 설정 변경
     */
    public void updateEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}
