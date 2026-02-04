package com.ssafy.domain.meeting.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_action_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingActionItem extends BaseEntity {
    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActionItemStatus status;

    @Builder
    private MeetingActionItem(Long meetingId, String content, Long assigneeId, ActionItemStatus status) {
        this.meetingId = meetingId;
        this.content = content;
        this.assigneeId = assigneeId;
        this.status = status;
    }

    public void updateAssignee(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateStatus(ActionItemStatus status) {
        this.status = status;
    }
}
