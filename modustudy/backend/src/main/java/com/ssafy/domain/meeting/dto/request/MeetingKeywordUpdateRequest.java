package com.ssafy.domain.meeting.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MeetingKeywordUpdateRequest(
        @NotEmpty List<String> keywords
        ) {
}
