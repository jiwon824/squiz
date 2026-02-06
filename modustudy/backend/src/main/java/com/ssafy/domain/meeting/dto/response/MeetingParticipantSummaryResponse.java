package com.ssafy.domain.meeting.dto.response;

import java.time.LocalDateTime;

public record MeetingParticipantSummaryResponse(
        Long id,
        Long userId,
        String summary,
        LocalDateTime createdAt
        ) {
}
