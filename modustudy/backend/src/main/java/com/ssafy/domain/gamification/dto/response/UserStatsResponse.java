package com.ssafy.domain.gamification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserStatsResponse {
    private Integer level;
    private String levelName;
    private LevelProgress levelProgress;
    private NextLevel nextLevel;
    private Integer totalActivityDays;
    private Integer currentStreak;
    private Integer maxStreak;
    private LocalDate lastActivityDate;
    private Integer totalStudiesJoined;
    private Integer totalStudiesLed;
    private Integer totalAttendance;
    private Integer totalChatCount;
    private Integer totalQuizCount;
    private Integer totalMaterialsUploaded;
    private Integer totalRetrospectives;
    private LocalDateTime joinedAt;

    @Getter
    @Builder
    public static class LevelProgress {
        private Integer current;
        private Integer required;
        private Double percentage;
    }

    @Getter
    @Builder
    public static class NextLevel {
        private Integer level;
        private String name;
    }
}