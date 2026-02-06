package com.ssafy.domain.daily.dto.response;

import com.ssafy.domain.daily.entity.DailyReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class DailyReportResponse {

    private Long id;
    private Long studyId;
    private LocalDate reportDate;
    private String summary;
    private LocalDateTime createdAt;

    public static DailyReportResponse from(DailyReport entity) {
        return DailyReportResponse.builder()
                .id(entity.getId())
                .studyId(entity.getStudyId())
                .reportDate(entity.getReportDate())
                .summary(entity.getSummary())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
