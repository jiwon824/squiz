package com.ssafy.domain.meeting.dto.response;

import java.time.LocalDateTime;

public record MeetingParticipantResponse(
        Long userId,
        String nickname,
        LocalDateTime joinedAt,
        LocalDateTime leftAt
        ) {
}
