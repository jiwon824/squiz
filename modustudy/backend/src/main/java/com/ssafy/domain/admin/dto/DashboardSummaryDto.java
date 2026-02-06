package com.ssafy.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardSummaryDto {
    private long totalUsers;
    private long activeStudies;
    private long todaySignups;
    private long pendingReports;
}
