package com.ssafy.domain.gamification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PenaltyListResponse {
    private List<PenaltyInfo> activePenalties;
    private List<RemovedPenalty> removedPenalties;
    private Integer totalActive;
    private Integer totalRemoved;

    @Getter
    @Builder
    public static class PenaltyInfo {
        private Long id;
        private String penaltyCode;
        private String name;
        private String description;
        private String icon;
        private LocalDateTime grantedAt;
        private Long studyId;
        private String studyName;
        private Boolean isActive;
        private String removalCondition;
        private Integer removalProgress;
        private Integer removalRequired;
    }

    @Getter
    @Builder
    public static class RemovedPenalty {
        private Long id;
        private String penaltyCode;
        private String name;
        private String description;
        private String icon;
        private LocalDateTime grantedAt;
        private LocalDateTime removedAt;
        private Long studyId;
        private String studyName;
    }
}