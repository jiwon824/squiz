package com.ssafy.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizStatsDto {
    private List<DailyQuizAttemptDto> dailyAttempts;
    private List<CourseParticipationDto> courseParticipation;

    @Getter
    @lombok.Setter
    public static class DailyQuizAttemptDto {
        private String date;
        private int count;
    }

    @Getter
    @lombok.Setter
    public static class CourseParticipationDto {
        private String courseName;
        private int participantCount;
    }
}
