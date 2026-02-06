package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 스터디 퀴즈 문제 엔티티
 */
 @Entity
 @Table(name = "study_quiz_question")
 @Getter
 @Builder
 @AllArgsConstructor
 @NoArgsConstructor(access = AccessLevel.PROTECTED)
 public class StudyQuizQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private StudyQuiz quiz;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    @Builder.Default
    private QuestionType questionType = QuestionType.MULTIPLE_CHOICE;

    /**
     * 객관식 보기 (JSON 형태)
     * 예: ["A. 선택1", "B. 선택2", "C. 선택3", "D. 선택4"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String options;

    @Column(name = "correct_answer", nullable = false, length = 500)
    private String correctAnswer;

    /**
     * 서술형 문제 채점용 키워드 (JSON 배열)
     * 예: ["키워드1", "키워드2", "키워드3"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_keywords", columnDefinition = "json")
    private String answerKeywords;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    /**
     * 부모 Quiz 설정 (양방향 관계)
     */
    void setQuiz(StudyQuiz quiz) {
        this.quiz = quiz;
    }
}
