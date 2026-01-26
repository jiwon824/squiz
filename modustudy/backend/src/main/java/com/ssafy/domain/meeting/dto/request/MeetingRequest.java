package com.ssafy.domain.meeting.dto.request;

import com.ssafy.domain.meeting.entity.MeetingType;
import jakarta.validation.constraints.Size;

public record MeetingRequest(
        @Size(max = 200) String title,
        Long sessionId,
        Long workspaceId,
        MeetingType meetingType,
        Boolean autoShareSummary,
        Long shareWorkspaceId
) {
}
