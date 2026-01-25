package com.ssafy.domain.study.dto.response;

import com.ssafy.domain.study.entity.SessionStatus;
import com.ssafy.domain.study.entity.StudySession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudySessionResponse {

    private Long id;

    private Long studyId;

    private Integer sessionNumber;

    private String title;

    private String description;

    private LocalDateTime scheduledAt;

    private Integer durationMinutes;

    private String location;

    private Boolean isOnline;

    private SessionStatus status;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static StudySessionResponse from(StudySession session) {
        return StudySessionResponse.builder()
                .id(session.getId())
                .studyId(session.getStudyId())
                .sessionNumber(session.getSessionNumber())
                .title(session.getTitle())
                .description(session.getDescription())
                .scheduledAt(session.getScheduledAt())
                .durationMinutes(session.getDurationMinutes())
                .location(session.getLocation())
                .isOnline(session.getIsOnline())
                .status(session.getStatus())
                .completedAt(session.getCompletedAt())
                .createdAt(session.getCreatedAt())
                .build();
    }
}