package com.ssafy.domain.meeting.dto.response;

import java.util.List;

public record MeetingTranscriptPageResponse(
        List<MeetingTranscriptItemResponse> content,
        long totalElements,
        boolean hasMore
        ) {
}
