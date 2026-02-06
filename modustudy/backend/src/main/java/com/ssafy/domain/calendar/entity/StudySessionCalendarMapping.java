package com.ssafy.domain.calendar.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "study_session_calendar_mapping",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "user_id"}))
        @Getter
        @Setter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @AllArgsConstructor
        @Builder
        public class StudySessionCalendarMapping extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "google_event_id", nullable = false)
    private String googleEventId;
}
