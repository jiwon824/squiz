package com.ssafy.domain.meeting.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_recording")
@AttributeOverride(name = "id", column = @Column(name = "meeting_recording_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingRecording extends BaseEntity {
    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "recording_url", nullable = false, length = 500)
    private String recordingUrl;

    @Column(name = "format", length = 20)
    private String format;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "started_at")
    private java.time.LocalDateTime startedAt;

    @Column(name = "ended_at")
    private java.time.LocalDateTime endedAt;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecordingStatus status;

    @Builder
    private MeetingRecording(Long meetingId, String recordingUrl, String format, Integer durationSeconds,
                             java.time.LocalDateTime startedAt, java.time.LocalDateTime endedAt,
                             Long fileSize, RecordingStatus status) {
        this.meetingId = meetingId;
        this.recordingUrl = recordingUrl;
        this.format = format;
        this.durationSeconds = durationSeconds;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.fileSize = fileSize;
        this.status = status;
    }

    public void updateStatus(RecordingStatus status) {
        this.status = status;
    }

    public void updateDetails(String recordingUrl, String format, Integer durationSeconds,
                              java.time.LocalDateTime startedAt, java.time.LocalDateTime endedAt,
                              Long fileSize) {
        if (recordingUrl != null) {
            this.recordingUrl = recordingUrl;
        }
        if (format != null) {
            this.format = format;
        }
        if (durationSeconds != null) {
            this.durationSeconds = durationSeconds;
        }
        if (startedAt != null) {
            this.startedAt = startedAt;
        }
        if (endedAt != null) {
            this.endedAt = endedAt;
        }
        if (fileSize != null) {
            this.fileSize = fileSize;
        }
    }
}
