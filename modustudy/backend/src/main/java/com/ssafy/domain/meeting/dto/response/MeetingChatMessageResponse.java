package com.ssafy.domain.meeting.dto.response;

import java.time.LocalDateTime;

public record MeetingChatMessageResponse(
        Long id,
        Long userId,
        String senderName,
        String content,
        LocalDateTime sentAt
        ) {
}
