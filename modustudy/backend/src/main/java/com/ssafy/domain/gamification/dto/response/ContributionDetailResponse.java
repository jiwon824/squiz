package com.ssafy.domain.gamification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ContributionDetailResponse {
    private LocalDate date;
    private Boolean hasActivity;
    private List<Activity> activities;

    @Getter
    @Builder
    public static class Activity {
        private String type;
        private Long referenceId;
        private String referenceName;
        private LocalDateTime createdAt;
    }
}
