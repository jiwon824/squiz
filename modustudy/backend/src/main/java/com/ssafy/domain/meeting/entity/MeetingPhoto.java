package com.ssafy.domain.meeting.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meeting_photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingPhoto extends BaseEntity {
    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "captured_at", nullable = false)
    private java.time.LocalDateTime capturedAt;

    @Column(name = "is_selected")
    private Boolean isSelected;

    @Builder
    private MeetingPhoto(Long meetingId, Long userId, String imageUrl, java.time.LocalDateTime capturedAt, Boolean isSelected) {
        this.meetingId = meetingId;
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.capturedAt = capturedAt;
        this.isSelected = isSelected;
    }

    public static MeetingPhoto capture(Long meetingId, Long userId, String imageUrl, java.time.LocalDateTime capturedAt) {
        return MeetingPhoto.builder()
                .meetingId(meetingId)
                .userId(userId)
                .imageUrl(imageUrl)
                .capturedAt(capturedAt)
                .isSelected(false)
                .build();
    }

    public void updateSelected(boolean selected) {
        this.isSelected = selected;
    }
}
