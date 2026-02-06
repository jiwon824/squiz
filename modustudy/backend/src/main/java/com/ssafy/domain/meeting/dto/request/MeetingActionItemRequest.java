package com.ssafy.domain.meeting.dto.request;

import com.ssafy.domain.meeting.entity.ActionItemStatus;

public record MeetingActionItemRequest(
        String content,
        Long assigneeId,
        ActionItemStatus status
        ) {
}
