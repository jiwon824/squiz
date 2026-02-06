package com.ssafy.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_review_items", uniqueConstraints = @UniqueConstraint(name = "uk_user_content", columnNames = {
                "user_id", "content_type", "content_id" }), indexes = {
                                @Index(name = "idx_user_next_review", columnList = "user_id, next_review_at")
                })
                @Getter
                @Setter
                @NoArgsConstructor(access = AccessLevel.PROTECTED)
                @AllArgsConstructor
                @Builder
                public class UserReviewItem {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_id", nullable = false)
        private Long userId;

        // 다형성 구조: 코스 퀴즈, 스터디 퀴즈 등 수용
        @Enumerated(EnumType.STRING)
        @Column(name = "content_type", nullable = false, length = 20)
        private ReviewContentType contentType;

        @Column(name = "content_id", nullable = false)
        private Long contentId;

        // ── FSRS 핵심 변수 ──

        // 기억 안정성 (S)
        @Builder.Default
        @Column(name = "stability", nullable = false)
        private Double stability = 0.0;

        // 난이도 (D, 1~10)
        @Builder.Default
        @Column(name = "difficulty", nullable = false)
        private Double difficulty = 5.0;

        // 마지막 복습 후 경과분
        @Builder.Default
        @Column(name = "elapsed_minutes")
        private Integer elapsedMinutes = 0;

        // 다음 복습까지의 간격 (분 단위)
        @Builder.Default
        @Column(name = "scheduled_minutes")
        private Integer scheduledMinutes = 0;

        // 전체 복습 횟수
        @Builder.Default
        @Column(name = "reps")
        private Integer reps = 0;

        // 잊어버린(틀린) 횟수
        @Builder.Default
        @Column(name = "lapses")
        private Integer lapses = 0;

        // 상태 (0:New, 1:Learning, 2:Review, 3:Relearning)
        @Builder.Default
        @Column(name = "state")
        private Integer state = 0;

        // ── V15 상태 추적 고도화 ──

        @Builder.Default
        @Column(name = "last_elapsed_minutes")
        private Integer lastElapsedMinutes = 0;

        @Builder.Default
        @Column(name = "last_response_time_ms")
        private Long lastResponseTimeMs = 0L;

        @Builder.Default
        @Column(name = "retrievability")
        private Double retrievability = 0.0;

        // ── 시간 관련 ──

        @Column(name = "last_reviewed_at")
        private LocalDateTime lastReviewedAt;

        // 복습 예정일
        @Column(name = "next_review_at")
        private LocalDateTime nextReviewAt;

        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;

        @PrePersist
        protected void onCreate() {
                this.createdAt = LocalDateTime.now();
        }
}
