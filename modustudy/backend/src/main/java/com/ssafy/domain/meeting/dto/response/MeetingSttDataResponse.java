package com.ssafy.domain.meeting.dto.response;

public record MeetingSttDataResponse(
        Long userId,
        String nickname,
        String content,
        Boolean isFinal,
        Integer timestampSeconds,
        Integer startMs,
        Integer endMs
        ) {
}
