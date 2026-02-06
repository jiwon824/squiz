package com.ssafy.domain.meeting.dto.response;

import com.ssafy.domain.meeting.entity.MeetingTextTrackType;
import java.time.LocalDateTime;

public record MeetingSttSummaryResponse(
        Long id,
        Long meetingId,
        Long userId,
        MeetingTextTrackType trackType,
        String fileUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
        ) {
}
