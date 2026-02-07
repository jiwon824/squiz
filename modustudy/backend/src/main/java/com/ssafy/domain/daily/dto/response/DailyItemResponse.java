package com.ssafy.domain.daily.dto.response;

import com.ssafy.domain.daily.entity.DailyCategory;
import com.ssafy.domain.daily.entity.DailyItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DailyItemResponse {

    private Long id;
    private Long dailyReportId;
    private Long userId;
    private DailyCategory category;
    private String content;
    private LocalDateTime createdAt;

    public static DailyItemResponse from(DailyItem item) {
        return DailyItemResponse.builder()
                .id(item.getId())
                .dailyReportId(item.getDailyReportId())
                .userId(item.getUserId())
                .category(item.getCategory())
                .content(item.getContent())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
