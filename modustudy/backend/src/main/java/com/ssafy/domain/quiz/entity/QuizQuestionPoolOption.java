package com.ssafy.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_question_pool_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(QuizQuestionPoolOption.QuizQuestionPoolOptionId.class)
public class QuizQuestionPoolOption {

    @Id
    @Column(name = "question_pool_id")
    private Long questionPoolId;

    @Id
    @Column(name = "option_label", length = 10)
    private String optionLabel;

    @Column(name = "option_text", length = 500)
    private String optionText;

    @Column(name = "is_correct")
    private Boolean isCorrect = false;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestionPoolOptionId implements Serializable {
        private Long questionPoolId;
        private String optionLabel;
    }
}
