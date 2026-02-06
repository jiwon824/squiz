package com.ssafy.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 일별 활동 통계 DTO (잔디)
 */
 @Getter
 @Setter
 public class DailyActivityStatsDto {
    private String date;
    private int count;
}
