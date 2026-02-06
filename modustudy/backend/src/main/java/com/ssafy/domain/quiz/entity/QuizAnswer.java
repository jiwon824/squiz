package com.ssafy.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 퀴즈 대회 답안
 */
 @Entity
 @Table(name = "quiz_answer")
 @Getter
 @NoArgsConstructor(access = AccessLevel.PROTECTED)
 @IdClass(QuizAnswer.QuizAnswerId.class)
 public class QuizAnswer {

    @Id
    @Column(name = "participant_id")
    private Long participantId;

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "user_answer", columnDefinition = "JSON")
    private String userAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "score")
    private Integer score = 0;

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizAnswerId implements Serializable {
        private Long participantId;
        private Long questionId;
    }
}
