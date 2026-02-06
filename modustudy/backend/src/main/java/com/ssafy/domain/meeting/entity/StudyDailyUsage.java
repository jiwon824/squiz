package com.ssafy.domain.meeting.entity;

import com.ssafy.domain.study.entity.Study;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 스터디별 일일 사용량 추적 엔티티
 * - 온라인 미팅: 3시간(10800초) 한도
 * - 오프라인 STT: 2시간(7200초) 한도
 */
 @Entity
 @Table(name = "study_daily_usage",
        uniqueConstraints = @UniqueConstraint(columnNames = {"study_id", "usage_date"}))
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public class StudyDailyUsage {

    /** 온라인 미팅 일일 한도 (3시간 = 10800초) */
    public static final int ONLINE_MEETING_DAILY_LIMIT_SECONDS = 10800;

    /** 오프라인 STT 일일 한도 (2시간 = 7200초) */
    public static final int OFFLINE_STT_DAILY_LIMIT_SECONDS = 7200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    /** 온라인 미팅 누적 시간 (초) */
    @Column(name = "online_meeting_seconds", nullable = false)
    @Builder.Default
    private Integer onlineMeetingSeconds = 0;

    /** 온라인 미팅 횟수 */
    @Column(name = "online_meeting_count", nullable = false)
    @Builder.Default
    private Integer onlineMeetingCount = 0;

    /** 오프라인 STT 누적 시간 (초) */
    @Column(name = "offline_stt_seconds", nullable = false)
    @Builder.Default
    private Integer offlineSttSeconds = 0;

    /** 오프라인 STT 횟수 */
    @Column(name = "offline_stt_count", nullable = false)
    @Builder.Default
    private Integer offlineSttCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== 편의 메서드 =====

    /** 온라인 미팅 남은 시간 (초) */
    public int getOnlineMeetingRemainingSeconds() {
        return Math.max(0, ONLINE_MEETING_DAILY_LIMIT_SECONDS - onlineMeetingSeconds);
    }

    /** 오프라인 STT 남은 시간 (초) */
    public int getOfflineSttRemainingSeconds() {
        return Math.max(0, OFFLINE_STT_DAILY_LIMIT_SECONDS - offlineSttSeconds);
    }

    /** 온라인 미팅 시작 가능 여부 */
    public boolean canStartOnlineMeeting() {
        return onlineMeetingSeconds < ONLINE_MEETING_DAILY_LIMIT_SECONDS;
    }

    /** 오프라인 STT 업로드 가능 여부 */
    public boolean canUploadOfflineStt() {
        return offlineSttSeconds < OFFLINE_STT_DAILY_LIMIT_SECONDS;
    }

    /** 특정 길이의 오프라인 STT 업로드 가능 여부 */
    public boolean canUploadOfflineStt(int durationSeconds) {
        return (offlineSttSeconds + durationSeconds) <= OFFLINE_STT_DAILY_LIMIT_SECONDS;
    }

    /** 온라인 미팅 시간 추가 */
    public void addOnlineMeetingTime(int seconds) {
        this.onlineMeetingSeconds += seconds;
        this.onlineMeetingCount++;
    }

    /** 오프라인 STT 시간 추가 */
    public void addOfflineSttTime(int seconds) {
        this.offlineSttSeconds += seconds;
        this.offlineSttCount++;
    }
}
