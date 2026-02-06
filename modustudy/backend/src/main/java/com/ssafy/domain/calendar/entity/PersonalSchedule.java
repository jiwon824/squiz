package com.ssafy.domain.calendar.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 개인 일정 엔티티
 */
 @Entity
 @Table(name = "personal_schedule")
 @Getter
 @Setter
 @NoArgsConstructor(access = AccessLevel.PROTECTED)
 @AllArgsConstructor
 @Builder
 public class PersonalSchedule extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "is_online")
    @Builder.Default
    private Boolean isOnline = false;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    @Column(name = "is_synced_with_google")
    @Builder.Default
    private Boolean isSyncedWithGoogle = false;

    @Column(name = "last_synced_at")
    private java.time.LocalDateTime lastSyncedAt;
}
