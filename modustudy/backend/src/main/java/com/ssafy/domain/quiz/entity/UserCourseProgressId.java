// UserCourseProgressId.java
package com.ssafy.domain.quiz.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for UserCourseProgress.
 * 
 * PK: (userId, courseId)
 * Maps to: user_course_progress(user_id, course_id)
 */
 @Getter
 @NoArgsConstructor
 @AllArgsConstructor
 @EqualsAndHashCode
 public class UserCourseProgressId implements Serializable {

    private Long userId;
    private Long courseId;
}
