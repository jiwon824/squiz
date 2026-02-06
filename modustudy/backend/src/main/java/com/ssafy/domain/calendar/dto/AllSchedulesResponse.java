package com.ssafy.domain.calendar.dto;

import lombok.*;

import java.util.List;

/**
 * 모든 일정 통합 조회 응답 DTO
 */
 @Getter
 @Setter
 @NoArgsConstructor
 @AllArgsConstructor
 @Builder
 public class AllSchedulesResponse {
    private List<PersonalScheduleResponse> personal;
    private List<StudySessionResponse> studySessions;
    private List<CalendarEventResponse> googleEvents;
}
