package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 퀴즈 코스 문제 엔티티.
 *
 * 코스 섹션 내 개별 문제를 나타낸다.
 * 객관식/단답형 유형을 지원하며, 객관식의 경우 options에 보기를 JSON 형태로 저장한다.
 *
 * DDL 참조: docs/sql/ERD.sql - quiz_course_question
 */
@Entity
@Table(name = "quiz_course_question")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizCourseQuestion extends BaseEntity {

    /**
     * 소속 섹션 (복합 FK: section_number + quiz_course_id).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "section_number", referencedColumnName = "section_number", nullable = false),
        @JoinColumn(name = "quiz_course_id", referencedColumnName = "quiz_course_id", nullable = false)
    })
    private QuizCourseSection section;

    /**
     * 문제 순서 번호.
     */
    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    /**
     * 문제 내용.
     */
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /**
     * 문제 유형 (MULTIPLE_CHOICE: 객관식, SHORT_ANSWER: 단답형).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionType questionType = QuestionType.MULTIPLE_CHOICE;

    /**
     * 객관식 보기 목록 (JSON 형태).
     * 예: [{"id": "A", "text": "int"}, {"id": "B", "text": "integer"}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String options;

    /**
     * 정답.
     * 객관식: "A", "B" 등 보기 ID
     * 단답형: 정답 텍스트
     */
    @Column(name = "correct_answer", nullable = false, length = 500)
    private String correctAnswer;

    /**
     * 문제 해설.
     */
    @Column(columnDefinition = "TEXT")
    private String explanation;
}
