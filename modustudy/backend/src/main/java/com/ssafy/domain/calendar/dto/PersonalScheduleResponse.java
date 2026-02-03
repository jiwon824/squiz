package com.ssafy.domain.calendar.dto;

import com.ssafy.domain.calendar.entity.PersonalSchedule;
import lombok.*;

import java.time.format.DateTimeFormatter;

/**
 * 개인 일정 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalScheduleResponse {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String startDate;    // YYYY-MM-DD
    private String startTime;    // HH:mm
    private String endDate;      // YYYY-MM-DD
    private String endTime;      // HH:mm
    private String location;
    private Boolean isOnline;
    private String color;
    private String googleEventId;
    private Boolean isSyncedWithGoogle;
    private String lastSyncedAt;
    private String createdAt;
    private String updatedAt;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static PersonalScheduleResponse from(PersonalSchedule entity) {
        return PersonalScheduleResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startDate(entity.getStartDate() != null ? entity.getStartDate().format(DATE_FORMATTER) : null)
                .startTime(entity.getStartTime() != null ? entity.getStartTime().format(TIME_FORMATTER) : null)
                .endDate(entity.getEndDate() != null ? entity.getEndDate().format(DATE_FORMATTER) : null)
                .endTime(entity.getEndTime() != null ? entity.getEndTime().format(TIME_FORMATTER) : null)
                .location(entity.getLocation())
                .isOnline(entity.getIsOnline())
                .color(entity.getColor())
                .googleEventId(entity.getGoogleEventId())
                .isSyncedWithGoogle(entity.getIsSyncedWithGoogle())
                .lastSyncedAt(entity.getLastSyncedAt() != null ? entity.getLastSyncedAt().format(DATETIME_FORMATTER) : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(DATETIME_FORMATTER) : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(DATETIME_FORMATTER) : null)
                .build();
    }
}
