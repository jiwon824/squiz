package com.ssafy.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 일별 출석 통계 DTO
 */
 @Getter
 @Setter
 public class DailyAttendanceStatsDto {
    private String date;
    private int count;
}
