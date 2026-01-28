package com.ssafy.domain.gamification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BadgeListResponse {
    private List<BadgeCategory> categories;
    private Integer totalBadges;
    private Integer earnedCount;

    @Getter
    @Builder
    public static class BadgeCategory {
        private String category;
        private String categoryName;
        private List<BadgeInfo> badges;
    }

    @Getter
    @Builder
    public static class BadgeInfo {
        private Long id;
        private String code;
        private String name;
        private String description;
        private String icon;
        private Boolean isEarned;
        private LocalDateTime earnedAt;
        private BadgeProgress progress;
    }

    @Getter
    @Builder
    public static class BadgeProgress {
        private Integer current;
        private Integer required;
        private Double percentage;
    }
}