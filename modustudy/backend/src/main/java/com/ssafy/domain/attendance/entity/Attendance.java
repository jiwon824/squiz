package com.ssafy.domain.attendance.entity;

import com.ssafy.common.entity.BaseEntity;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_user",
                        columnNames = {"session_id", "user_id"}
                )
        }
        )
        @Getter
        @Setter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @AllArgsConstructor
        @Builder
        public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", length = 20, nullable = false)
    @Builder.Default
    private AttendanceCheckType checkType = AttendanceCheckType.SELF;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by")
    private User checkedBy;

    @Column(name = "excuse_reason", columnDefinition = "TEXT")
    private String excuseReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "excuse_status", length = 20)
    private AttendanceExcuseStatus excuseStatus;
}
