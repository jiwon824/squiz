package com.ssafy.domain.meeting.scheduler;

import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingType;
import com.ssafy.domain.meeting.repository.MeetingRepository;
import com.ssafy.domain.study.entity.SessionStatus;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.study.repository.StudySessionRepository;
import com.ssafy.domain.study.workspace.entity.Workspace;
import com.ssafy.domain.study.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingAutoScheduler {
    private static final int WINDOW_MINUTES = 1;
    private static final int DEFAULT_PLANNED_DURATION_SECONDS = 60 * 60;
    private static final int MAX_PLANNED_DURATION_SECONDS = 3 * 60 * 60;

    private final StudySessionRepository studySessionRepository;
    private final MeetingRepository meetingRepository;
    private final WorkspaceRepository workspaceRepository;

    @Scheduled(fixedDelay = 60000)
    public void openScheduledMeetings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusMinutes(WINDOW_MINUTES);
        LocalDateTime end = now.plusMinutes(WINDOW_MINUTES);
        List<StudySession> sessions = studySessionRepository.findByScheduledAtBetween(start, end);

        for (StudySession session : sessions) {
            if (session.getStatus() != SessionStatus.SCHEDULED) {
                continue;
            }
            // 오프라인 세션은 미팅 자동 생성하지 않음 (오프라인 녹음 업로드 시 별도로 생성)
            if (session.getIsOnline() == null || !session.getIsOnline()) {
                continue;
            }
            if (meetingRepository.existsBySessionId(session.getId())) {
                continue;
            }
            Long workspaceId = workspaceRepository.findByStudyId(session.getStudyId())
                    .map(Workspace::getId)
                    .orElse(null);
            String title = buildMeetingTitle(session);
            int plannedDurationSeconds = resolvePlannedDurationSeconds(session);
            Meeting meeting = Meeting.schedule(
                    session.getStudyId(),
                    session.getId(),
                    workspaceId,
                    title,
                    MeetingType.OTHER,
                    session.getScheduledAt(),
                    plannedDurationSeconds
            );
            meetingRepository.save(meeting);
            log.info("온라인 세션 자동 미팅 생성 완료 - studyId={}, sessionId={}, meetingTitle={}",
                    session.getStudyId(), session.getId(), title);
        }
    }

    private String buildMeetingTitle(StudySession session) {
        String sessionTitle = session.getTitle();
        if (sessionTitle != null && !sessionTitle.isBlank()) {
            return sessionTitle;
        }
        Integer sessionNumber = session.getSessionNumber();
        if (sessionNumber != null) {
            return sessionNumber + "회차 미팅";
        }
        return "미팅";
    }

    private int resolvePlannedDurationSeconds(StudySession session) {
        Integer durationMinutes = session.getDurationMinutes();
        int planned = (durationMinutes == null || durationMinutes <= 0)
                ? DEFAULT_PLANNED_DURATION_SECONDS
                : durationMinutes * 60;
        return Math.min(planned, MAX_PLANNED_DURATION_SECONDS);
    }
}
