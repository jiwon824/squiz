package com.ssafy.domain.meeting.dto.response;

import java.time.LocalDateTime;

public record MeetingRecordingResponse(
        Long id,
        String recordingUrl,
        String format,
        Integer durationSeconds,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long fileSize,
        String status,
        LocalDateTime createdAt
        ) {
}
