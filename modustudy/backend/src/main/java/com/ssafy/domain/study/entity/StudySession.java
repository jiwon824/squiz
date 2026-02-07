package com.ssafy.domain.study.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "study_session",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_study_session",
                        columnNames = {"study_id", "session_number"}
                )
        }
        )
        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 60;

    @Column(length = 200)
    private String location;

    @Column(name = "is_online")
    @Builder.Default
    private Boolean isOnline = true;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ==================== 비즈니스 로직 ====================

    /**
     * 세션 시작
     */
    public void start() {
        if (this.status != SessionStatus.SCHEDULED) {
            throw new IllegalStateException("예정된 세션만 시작할 수 있습니다.");
        }
        this.status = SessionStatus.IN_PROGRESS;
    }

    /**
     * 세션 완료
     * 진행 중(IN_PROGRESS) 또는 예정(SCHEDULED) 상태의 세션을 완료 처리
     * 오프라인 녹음 업로드 시 SCHEDULED 상태에서 바로 완료될 수 있음
     */
    public void complete() {
        if (this.status != SessionStatus.IN_PROGRESS && this.status != SessionStatus.SCHEDULED) {
            throw new IllegalStateException("진행 중이거나 예정된 세션만 완료할 수 있습니다.");
        }
        this.status = SessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 세션 취소
     */
    public void cancel() {
        if (this.status == SessionStatus.COMPLETED) {
            throw new IllegalStateException("완료된 세션은 취소할 수 없습니다.");
        }
        this.status = SessionStatus.CANCELLED;
    }

    /**
     * 세션 정보 수정
     */
    public void updateInfo(String title, String description, LocalDateTime scheduledAt,
                           Integer durationMinutes, String location, Boolean isOnline) {
        if (this.status != SessionStatus.SCHEDULED) {
            throw new IllegalStateException("예정된 세션만 수정할 수 있습니다.");
        }

        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (scheduledAt != null) this.scheduledAt = scheduledAt;
        if (durationMinutes != null) this.durationMinutes = durationMinutes;
        if (location != null) this.location = location;
        if (isOnline != null) this.isOnline = isOnline;
    }
}
