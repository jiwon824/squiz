package com.ssafy.domain.meeting.dto.response;

import java.time.LocalDateTime;

public record MeetingTranscriptItemResponse(
        Long id,
        MeetingUserResponse user,
        String content,
        Integer timestampSeconds,
        Integer startMs,
        Integer endMs,
        LocalDateTime createdAt
        ) {
}
