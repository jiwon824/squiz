package com.ssafy.domain.gamification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 첫 스터디장 리뷰 작성 이벤트
 * 사용자가 처음으로 스터디장 리뷰를 작성할 때 발행
 */
 @Getter
 @AllArgsConstructor
 public class FirstLeaderReviewEvent {
    private final Long userId;        // 리뷰 작성자
    private final Long studyId;       // 스터디 ID
    private final String studyName;   // 스터디 이름
    private final Long leaderId;      // 평가받은 스터디장 ID
    private final LocalDate reviewDate;
}
