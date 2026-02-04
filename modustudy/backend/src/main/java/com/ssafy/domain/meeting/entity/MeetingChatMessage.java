package com.ssafy.domain.meeting.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingChatMessage extends BaseEntity {
    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Builder
    private MeetingChatMessage(Long meetingId, Long userId, String senderName, String content, LocalDateTime sentAt) {
        this.meetingId = meetingId;
        this.userId = userId;
        this.senderName = senderName;
        this.content = content;
        this.sentAt = sentAt;
    }
}
