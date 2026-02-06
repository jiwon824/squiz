package com.ssafy.domain.study.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyRecommendDto {

    private Long studyId;
    private String studyName;
    private String description;
    private String topicName;
    private String parentTopicName;
    private String formatName;
    private String difficulty;
    private String studyType;
    private String meetingType;
    private String scheduleDays;
    private LocalTime scheduleTime;
    private String scheduleSummary;
    private Integer maxMembers;
    private Integer currentMembers;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate recruitEndDate;
    private String goal;
    private String textbook;

    // 스터디장 정보
    private Long leaderId;
    private String leaderNickname;
    private String leaderProfileImage;
    private BigDecimal leaderAvgRating;
    private Integer leaderReviewCount;

    // 매칭 점수 (서비스에서 계산)
    private BigDecimal matchingScore;
    private String matchReason;

    // 추천 로그 ID (프론트에서 action 로깅 시 사용)
    private Long recommendLogId;

    // SQL에서 계산하는 중간 값들
    private Integer techMatchCount;
    private Integer scheduleMatchCount;
    private Integer topicMatchCount;
}
