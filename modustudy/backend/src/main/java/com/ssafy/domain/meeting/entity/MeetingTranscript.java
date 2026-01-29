package com.ssafy.domain.meeting.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 미팅 실시간 STT 결과 (화자별 발언 단위)
 */
@Entity
@Table(name = "meeting_transcript")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingTranscript extends BaseEntity {

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "timestamp_seconds", nullable = false)
    private Integer timestampSeconds;

    @Builder
    private MeetingTranscript(Long meetingId, Long userId, String content, Integer timestampSeconds) {
        this.meetingId = meetingId;
        this.userId = userId;
        this.content = content;
        this.timestampSeconds = timestampSeconds;
    }

    public static MeetingTranscript create(Long meetingId, Long userId, String content, Integer timestampSeconds) {
        return MeetingTranscript.builder()
                .meetingId(meetingId)
                .userId(userId)
                .content(content)
                .timestampSeconds(timestampSeconds)
                .build();
    }
}
