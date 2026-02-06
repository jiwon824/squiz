package com.ssafy.domain.attendance.scheduler;

import com.ssafy.domain.attendance.service.AttendanceService;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.study.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {
    private static final int INIT_WINDOW_MINUTES = 5;
    private static final int FINALIZE_DELAY_MINUTES = 10;
    private static final int LOOKBACK_HOURS = 6;

    private final StudySessionRepository studySessionRepository;
    private final AttendanceService attendanceService;

    @Scheduled(fixedDelay = 60000)
    public void initializeAttendanceRows() {
        LocalDateTime now = LocalDateTime.now();
        List<StudySession> sessions = studySessionRepository.findByScheduledAtBetween(
                now.minusMinutes(INIT_WINDOW_MINUTES),
                now.plusMinutes(1)
        );
        for (StudySession session : sessions) {
            try {
                attendanceService.initializeAttendanceRows(session);
            } catch (Exception ex) {
}
        }
    }

    @Scheduled(fixedDelay = 120000)
    public void finalizeAttendanceRows() {
        LocalDateTime now = LocalDateTime.now();
        List<StudySession> sessions = studySessionRepository.findByScheduledAtBetween(
                now.minusHours(LOOKBACK_HOURS),
                now
        );
        for (StudySession session : sessions) {
            int duration = session.getDurationMinutes() == null ? 60 : session.getDurationMinutes();
            LocalDateTime endAt = session.getScheduledAt()
                    .plusMinutes(duration)
                    .plusMinutes(FINALIZE_DELAY_MINUTES);
            if (now.isAfter(endAt)) {
                try {
                    attendanceService.finalizeAttendanceForSession(session);
                } catch (Exception ex) {
}
            }
        }
    }
}

