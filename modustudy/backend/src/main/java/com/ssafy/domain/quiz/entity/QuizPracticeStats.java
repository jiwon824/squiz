package com.ssafy.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_practice_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(QuizPracticeStats.QuizPracticeStatsId.class)
public class QuizPracticeStats {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "total_attempted")
    private Integer totalAttempted = 0;

    @Column(name = "total_correct")
    private Integer totalCorrect = 0;

    @Column(name = "best_score")
    private Integer bestScore = 0;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizPracticeStatsId implements Serializable {
        private Long userId;
        private Long categoryId;
    }
}
