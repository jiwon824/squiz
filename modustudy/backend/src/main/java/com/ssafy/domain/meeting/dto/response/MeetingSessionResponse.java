package com.ssafy.domain.meeting.dto.response;

public record MeetingSessionResponse(
        Long id,
        Integer sessionNumber,
        String title
        ) {
}
