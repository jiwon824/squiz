package com.ssafy.domain.meeting.dto.request;

import com.ssafy.domain.meeting.entity.RecordingStatus;

import java.time.LocalDateTime;

public record MeetingRecordingRequest(
        String recordingUrl,
        String format,
        Integer durationSeconds,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long fileSize,
        RecordingStatus status
        ) {
}
