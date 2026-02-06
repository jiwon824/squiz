package com.ssafy.domain.meeting.dto.request;

import java.util.List;

public record MeetingPhotoSelectionRequest(List<Long> photoIds) {
}
