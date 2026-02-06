package com.ssafy.domain.quiz.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for QuizCourseSection.
 * 
 * PK: (quizCourseId, sectionNumber)
 * Maps to: quiz_course_section(quiz_course_id, section_number)
 */
 @Getter
 @NoArgsConstructor
 @AllArgsConstructor
 @EqualsAndHashCode
 public class QuizCourseSectionId implements Serializable {

    private Long quizCourseId;
    private Integer sectionNumber;
}
