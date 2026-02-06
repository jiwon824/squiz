package com.ssafy.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;

/**
 * 퀴즈 코스 섹션 엔티티.
 *
 * 코스 내 학습 단계를 나타내며, 각 섹션은 여러 문제를 포함한다.
 * 사용자는 이전 섹션을 통과해야 다음 섹션을 해금할 수 있다.
 *
 * PK: (quiz_course_id, section_number) - 복합키
 * 
 * DDL 참조: docs/sql/ERD.sql - quiz_course_section
 */
 @Entity
 @Table(name = "quiz_course_section")
 @IdClass(QuizCourseSectionId.class)
 @Getter
 @Builder
 @AllArgsConstructor
 @NoArgsConstructor(access = AccessLevel.PROTECTED)
 public class QuizCourseSection {

    /**
     * Part of composite PK - Course ID.
     */
    @Id
    @Column(name = "quiz_course_id")
    private Long quizCourseId;

    /**
     * Part of composite PK - Section number (1-based).
     */
    @Id
    @Column(name = "section_number")
    private Integer sectionNumber;

    /**
     * Parent course (MapsId to sync with quizCourseId).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("quizCourseId")
    @JoinColumn(name = "quiz_course_id", nullable = false)
    private QuizCourse course;

    /**
     * 섹션 이름 (예: 기본 문법, 객체지향).
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 섹션 설명.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 섹션 내 총 문제 수.
     */
    @Column(name = "total_questions")
    @Builder.Default
    private Integer totalQuestions = 0;

    /**
     * 통과 점수 (%, 기본값 70).
     */
    @Column(name = "pass_score")
    @Builder.Default
    private Integer passScore = 70;

    /**
     * 생성 일시.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 섹션에 속한 문제 목록.
     */
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionNumber ASC")
    @Builder.Default
    private List<QuizCourseQuestion> questions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * 정적 팩토리 메서드.
     *
     * @param course        소속 코스
     * @param sectionNumber 섹션 순서 번호 (Service에서 계산하여 전달)
     * @param name          섹션 이름
     * @param description   섹션 설명
     * @param passScore     통과 점수 (null이면 기본값 70)
     * @return 새 섹션 인스턴스
     */
    public static QuizCourseSection create(
            QuizCourse course,
            Integer sectionNumber,
            String name,
            String description,
            Integer passScore) {
        QuizCourseSection section = new QuizCourseSection();
        section.quizCourseId = course.getId(); // courseId 직접 설정
        section.sectionNumber = sectionNumber;
        section.course = course;
        section.name = name;
        section.description = description;
        section.passScore = passScore != null ? passScore : 70;
        section.questions = new ArrayList<>();
        return section;
    }
}
