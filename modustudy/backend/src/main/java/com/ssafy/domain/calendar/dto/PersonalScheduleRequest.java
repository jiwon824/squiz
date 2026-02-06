package com.ssafy.domain.calendar.dto;

import lombok.*;

/**
 * 개인 일정 생성/수정 요청 DTO
 */
 @Getter
 @Setter
 @NoArgsConstructor
 @AllArgsConstructor
 @Builder
 public class PersonalScheduleRequest {
    private String title;
    private String description;
    private String startDate;    // YYYY-MM-DD
    private String startTime;    // HH:mm
    private String endDate;      // YYYY-MM-DD
    private String endTime;      // HH:mm
    private String location;
    private Boolean isOnline;
    private String color;
    private Boolean syncToGoogle;
}
