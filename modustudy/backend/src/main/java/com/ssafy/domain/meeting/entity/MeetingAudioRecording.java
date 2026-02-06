package com.ssafy.domain.meeting.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_audio_recording")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingAudioRecording extends BaseEntity {
    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "track_type", nullable = false, length = 20)
    private MeetingAudioTrackType trackType;

    @Column(name = "recording_url", nullable = false, length = 500)
    private String recordingUrl;

    @Column(name = "format", length = 20)
    private String format;

    @Column(name = "file_size")
    private Long fileSize;

    @Builder
    private MeetingAudioRecording(Long meetingId, Long userId, MeetingAudioTrackType trackType,
                                  String recordingUrl, String format, Long fileSize) {
        this.meetingId = meetingId;
        this.userId = userId;
        this.trackType = trackType;
        this.recordingUrl = recordingUrl;
        this.format = format;
        this.fileSize = fileSize;
    }

    public void updateRecording(String recordingUrl, String format, Long fileSize) {
        if (recordingUrl != null && !recordingUrl.isBlank()) {
            this.recordingUrl = recordingUrl;
        }
        if (format != null && !format.isBlank()) {
            this.format = format;
        }
        if (fileSize != null) {
            this.fileSize = fileSize;
        }
    }
}
