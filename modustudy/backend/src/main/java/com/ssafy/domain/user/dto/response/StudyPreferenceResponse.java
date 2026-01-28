package com.ssafy.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 스터디 선호 설정 응답 DTO
 */
@Getter
@Builder
public class StudyPreferenceResponse {
    private List<String> techStacks;
    private List<String> availableDays;
    private List<String> preferredTimeSlots;
    private Integer preferredDurationWeeks;
}
