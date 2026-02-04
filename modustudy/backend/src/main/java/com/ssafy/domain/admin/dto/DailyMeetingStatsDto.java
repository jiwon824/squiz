package com.ssafy.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 일별 미팅 통계 DTO
 */
@Getter
@Setter
public class DailyMeetingStatsDto {
    private String date;
    private int count;
}
