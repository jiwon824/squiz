package com.ssafy.domain.gamification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 회고록 작성 이벤트
 */
@Getter
@AllArgsConstructor
public class RetrospectiveWriteEvent {
    private final Long userId;
    private final Long studyId;
    private final String studyName;
    private final Long retrospectiveId;
    private final LocalDate writeDate;
}
