package com.ssafy.domain.meeting.dto.response;

public record MeetingEndResponse(
        Integer durationSeconds,
        Integer participantCount,
        String summaryStatus
        ) {
}
