package com.ssafy.domain.meeting.dto.response;

import com.ssafy.domain.meeting.entity.MeetingAudioTrackType;
import java.time.LocalDateTime;

public record MeetingAudioRecordingResponse(
        Long id,
        Long meetingId,
        Long userId,
        MeetingAudioTrackType trackType,
        String recordingUrl,
        String format,
        Long fileSize,
        LocalDateTime createdAt
        ) {
}
