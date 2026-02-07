package com.ssafy.domain.meeting.dto.request;

import com.ssafy.domain.meeting.entity.SummaryStatus;

import java.util.List;

public record MeetingSummaryUpdateRequest(
        String summary,
        List<MeetingActionItemRequest> actionItems,
        List<String> keywords,
        SummaryStatus status
        ) {
}
