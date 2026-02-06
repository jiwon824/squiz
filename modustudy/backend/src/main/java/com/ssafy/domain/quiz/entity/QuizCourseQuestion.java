package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 퀴즈 코스 문제 엔티티.
 *
 * 코스 섹션 내 개별 문제를 나타낸다.
 * 객관식/단답형 유형을 지원하며, 객관식의 경우 options에 보기를 JSON 형태로 저장한다.
 *
 * FK: (quiz_course_id, section_number) references quiz_course_section
 * 
 * DDL: docs/sql/ERD.sql - quiz_course_question
 */
 @Entity
 @Table(name = "quiz_course_question")
 @Getter
 @Builder
 @AllArgsConstructor
 @NoArgsConstructor(access = AccessLevel.PROTECTED)
 public class QuizCourseQuestion extends BaseEntity {

    /**
     * 소속 섹션 (복합 FK: quiz_course_id + section_number).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "quiz_course_id", referencedColumnName = "quiz_course_id", nullable = false),
            @JoinColumn(name = "section_number", referencedColumnName = "section_number", nullable = false)
    })
    private QuizCourseSection section;

    /**
     * 문제 순서 번호(섹션 내)
     */
    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    /**
     * 문제 내용.
     */
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /**
     * 문제 유형 (MULTIPLE_CHOICE, SHORT_ANSWER, MULTIPLE_CHOICE_MULTIPLE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    @Builder.Default
    private QuestionType questionType = QuestionType.MULTIPLE_CHOICE;

    /**
     * 객관식 문제의 경우, 보기를 JSON 형태로 저장
     * Example: [{"id": "A", "text": "int"}, {"id": "B", "text": "integer"}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String options;

    /**
     * Correct answer.
     */
    @Column(name = "correct_answer", nullable = false, length = 500)
    private String correctAnswer;

    /**
     * Explanation for the question.
     */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    /**
     * 서술형 문제 채점용 핵심 키워드.
     * JSON 배열 형태로 저장 (예: ["DDL", "데이터 정의"])
     * 사용자 답변에 모든 키워드가 포함되어야 정답 처리됨 (AND 조건)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "json")
    private String keywords;
}
