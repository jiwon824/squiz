package com.ssafy.domain.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CourseQuizStatDto {
    private String courseName;
    private String courseCode;
    private Long attemptedCount;
    private Long correctCount;
}
