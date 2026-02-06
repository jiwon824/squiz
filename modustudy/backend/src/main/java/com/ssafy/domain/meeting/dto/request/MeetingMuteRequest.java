package com.ssafy.domain.meeting.dto.request;

import jakarta.validation.constraints.NotNull;

public record MeetingMuteRequest(
        @NotNull Boolean muted
        ) {
}
