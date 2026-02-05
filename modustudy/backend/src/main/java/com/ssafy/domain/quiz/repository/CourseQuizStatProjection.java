package com.ssafy.domain.quiz.repository;

public interface CourseQuizStatProjection {
    String getCourseName();

    Long getAttemptedCount();

    Long getCorrectCount();

    String getCourseCode();
}
