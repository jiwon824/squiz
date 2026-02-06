package com.ssafy.domain.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MeetingParticipantSummaryRequest(
        @NotNull Long userId,
        @NotBlank String summary
        ) {
}
