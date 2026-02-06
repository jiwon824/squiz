package com.ssafy.domain.daily.dto.response;

import com.ssafy.domain.daily.entity.DailyCategory;
import com.ssafy.domain.daily.entity.DailyItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
public class MyDailyItemsResponse {

    private Long dailyReportId;
    private Long userId;
    private List<DailyItemResponse> yesterday;  // 어제 한 일
    private List<DailyItemResponse> today;      // 오늘 할 일
    private List<DailyItemResponse> blocker;    // 블로커
    private int totalCount;

    public static MyDailyItemsResponse from(Long dailyReportId, Long userId, List<DailyItem> items) {
        Map<DailyCategory, List<DailyItemResponse>> grouped = items.stream()
                .map(DailyItemResponse::from)
                .collect(Collectors.groupingBy(DailyItemResponse::getCategory));

        return MyDailyItemsResponse.builder()
                .dailyReportId(dailyReportId)
                .userId(userId)
                .yesterday(grouped.getOrDefault(DailyCategory.YESTERDAY, List.of()))
                .today(grouped.getOrDefault(DailyCategory.TODAY, List.of()))
                .blocker(grouped.getOrDefault(DailyCategory.BLOCKER, List.of()))
                .totalCount(items.size())
                .build();
    }
}
