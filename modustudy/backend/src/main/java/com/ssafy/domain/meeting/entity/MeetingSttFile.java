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
@Table(name = "meeting_stt_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingSttFile extends BaseEntity {
    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "track_type", nullable = false, length = 20)
    private MeetingTextTrackType trackType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Builder
    private MeetingSttFile(Long meetingId, Long userId, MeetingTextTrackType trackType, String fileUrl) {
        this.meetingId = meetingId;
        this.userId = userId;
        this.trackType = trackType;
        this.fileUrl = fileUrl;
    }

    public void updateFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
