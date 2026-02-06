package com.ssafy.domain.meeting.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meeting_participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingParticipant extends BaseEntity {
    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private java.time.LocalDateTime joinedAt;

    @Column(name = "left_at")
    private java.time.LocalDateTime leftAt;

    @Column(name = "is_muted")
    private Boolean isMuted;

    @Column(name = "is_camera_on")
    private Boolean isCameraOn;

    @Builder
    private MeetingParticipant(Long meetingId, Long userId, java.time.LocalDateTime joinedAt,
                               java.time.LocalDateTime leftAt, Boolean isMuted, Boolean isCameraOn) {
        this.meetingId = meetingId;
        this.userId = userId;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.isMuted = isMuted;
        this.isCameraOn = isCameraOn;
    }

    public static MeetingParticipant join(Long meetingId, Long userId, java.time.LocalDateTime joinedAt) {
        return MeetingParticipant.builder()
                .meetingId(meetingId)
                .userId(userId)
                .joinedAt(joinedAt)
                .isMuted(false)
                .isCameraOn(false)
                .build();
    }

    public void leave(java.time.LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }

    public void updateMute(boolean muted) {
        this.isMuted = muted;
    }

    public void updateCameraOn(boolean cameraOn) {
        this.isCameraOn = cameraOn;
    }

    public void rejoin(java.time.LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
        this.leftAt = null;
    }
}
