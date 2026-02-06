package com.ssafy.domain.meeting.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingSummaryResponse(
        Long id,
        String summary,
        List<MeetingActionItemResponse> actionItems,
        List<String> keywords,
        List<String> highlights,
        String status,
        LocalDateTime createdAt
        ) {
}
