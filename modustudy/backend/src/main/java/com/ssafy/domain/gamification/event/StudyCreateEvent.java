package com.ssafy.domain.gamification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 스터디 생성 이벤트
 */
 @Getter
 @AllArgsConstructor
 public class StudyCreateEvent {
    private final Long userId;
    private final Long studyId;
    private final String studyName;
    private final LocalDate createDate;
    private final boolean isFirstStudy; // 첫 스터디 생성 여부 (보너스용)
}
