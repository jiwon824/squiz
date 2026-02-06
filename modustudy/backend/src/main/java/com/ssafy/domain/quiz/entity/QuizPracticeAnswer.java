package com.ssafy.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_practice_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(QuizPracticeAnswer.QuizPracticeAnswerId.class)
public class QuizPracticeAnswer {

    @Id
    @Column(name = "practice_record_id")
    private Long practiceRecordId;

    @Id
    @Column(name = "question_pool_id")
    private Long questionPoolId;

    @Column(name = "user_answer", columnDefinition = "JSON")
    private String userAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizPracticeAnswerId implements Serializable {
        private Long practiceRecordId;
        private Long questionPoolId;
    }
}
