package com.ssafy.domain.meeting.dto.response;

import java.util.List;

public record MeetingChatMessagePageResponse(
        List<MeetingChatMessageResponse> content,
        long totalElements,
        boolean hasMore
        ) {
}
