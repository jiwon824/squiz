package com.ssafy.domain.meeting.dto.response;

import java.time.LocalDateTime;

public record MeetingPhotoResponse(
        Long id,
        String imageUrl,
        LocalDateTime capturedAt,
        Boolean isSelected
        ) {
}
