package com.ssafy.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_review_log",
        indexes = {
                @Index(name = "idx_review_log_item", columnList = "review_item_id, reviewed_at")
        }
        )
        @Getter
        @Setter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @AllArgsConstructor
        @Builder
        public class UserReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_item_id", nullable = false)
    private UserReviewItem reviewItem;

    // 정답 여부
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    // 응답 시간(ms)
    @Column(name = "response_time_ms", nullable = false)
    private Long responseTimeMs;

    // 복습 시점의 FSRS 스냅샷
    @Column(name = "stability", nullable = false)
    private Double stability;

    @Column(name = "difficulty", nullable = false)
    private Double difficulty;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        if (this.reviewedAt == null) {
            this.reviewedAt = LocalDateTime.now();
        }
    }
}
