package com.ssafy.domain.calendar.dto;

import lombok.*;

/**
 * 스터디 세션 응답 DTO (캘린더용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySessionResponse {
    private Long id;
    private Long studyId;
    private Integer sessionNumber;
    private String title;
    private String description;
    private String scheduledAt;    // ISO DateTime
    private Integer durationMinutes;
    private String location;
    private Boolean isOnline;
    private String status;
    private String completedAt;
    private String createdAt;
}
