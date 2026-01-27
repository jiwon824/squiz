package com.ssafy.domain.gamification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class StudyAttendanceEvent extends ApplicationEvent {
    private final Long userId;
    private final Long studyId;
    private final String studyName;
    private final LocalDate attendanceDate;

    public StudyAttendanceEvent(Object source, Long userId, Long studyId, String studyName, LocalDate attendanceDate) {
        super(source);
        this.userId = userId;
        this.studyId = studyId;
        this.studyName = studyName;
        this.attendanceDate = attendanceDate;
    }
}