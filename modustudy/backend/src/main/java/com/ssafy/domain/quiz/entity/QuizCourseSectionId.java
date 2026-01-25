package com.ssafy.domain.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * 퀴즈 코스 섹션 복합키.
 *
 * PK: (section_number, quiz_course_id)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class QuizCourseSectionId implements Serializable {

    private Long quizCourseId;
    private Integer sectionNumber;
}
