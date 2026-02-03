package com.ssafy.domain.gamification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ContributionResponse {
    private Integer year;
    private Integer month;  // 월간 조회 시에만
    private List<ContributionDay> contributions;
    private ContributionSummary summary;
    private List<MonthlyStats> monthlyStats;  // 연간 조회 시에만

    @Getter
    @Builder
    public static class ContributionDay {
        private LocalDate date;
        private Boolean hasActivity;
        private Integer activityCount;  // 활동 횟수 (레벨 계산용)
    }

    @Getter
    @Builder
    public static class ContributionSummary {
        private Integer totalDays;
        private Integer activeDays;
        private Integer currentStreak;
        private Integer maxStreak;
    }

    @Getter
    @Builder
    public static class MonthlyStats {
        private Integer month;
        private Integer activeDays;
    }
}