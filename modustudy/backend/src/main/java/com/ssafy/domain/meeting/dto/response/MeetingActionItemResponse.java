package com.ssafy.domain.meeting.dto.response;

import com.ssafy.domain.meeting.entity.ActionItemStatus;

public record MeetingActionItemResponse(
        Long id,
        String content,
        Long assigneeId,
        ActionItemStatus status
        ) {
}
