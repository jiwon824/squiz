package com.ssafy.domain.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 스터디 선호 설정 요청 DTO
 */
 @Getter
 @NoArgsConstructor
 public class StudyPreferenceRequest {
    private List<String> techStacks;          // ["Java", "Python", "React"]
    private List<String> availableDays;       // ["MON", "WED", "FRI"]
    private List<String> preferredTimeSlots;  // ["EVENING", "NIGHT"]
    private Integer preferredDurationWeeks;   // 2~8
}
