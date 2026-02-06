package com.ssafy.domain.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MeetingTranscriptRequest(
        @NotNull Long userId,
        @NotBlank String content,
        @NotNull Integer timestampSeconds,
        Integer startMs,
        Integer endMs,
        Boolean isFinal
        ) {
}
