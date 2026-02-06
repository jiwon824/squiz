package com.ssafy.domain.calendar.service;

import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.StudyMember;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Google Calendar 동기화 전용 서비스
 * - @Async 메서드를 별도 서비스로 분리하여 프록시 문제 해결
 * - 세션 생성/수정/삭제 시 멤버들의 캘린더에 이벤트 동기화
 */
 @Slf4j
 @Service
 @RequiredArgsConstructor
 public class CalendarSyncService {

    private final StudySessionRepository studySessionRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final GoogleCalendarService googleCalendarService;

    /**
     * 여러 세션을 멤버 캘린더에 비동기로 동기화
     */
    @Async
    public void syncSessionsToMemberCalendarsAsync(List<Long> sessionIds, Long studyId, String studyTitle) {
        for (Long sessionId : sessionIds) {
            try {
                StudySession session = studySessionRepository.findById(sessionId).orElse(null);
                if (session != null) {
                    syncSessionToMemberCalendars(session, studyTitle);
                }
            } catch (Exception e) {
}
        }
}

    /**
     * 단일 세션을 멤버 캘린더에 동기화
     */
    public void syncSessionToMemberCalendars(StudySession session, String studyTitle) {
        try {
            // 활성 스터디 멤버 조회
            List<StudyMember> activeMembers = studyMemberRepository
                    .findByStudyIdAndStatus(session.getStudyId(), MemberStatus.APPROVED);

            for (StudyMember member : activeMembers) {
                try {
                    // Google Calendar 연동 확인
                    if (googleCalendarService.isCalendarLinked(member.getUserId())) {
                        String eventId = googleCalendarService.createEvent(
                                member.getUserId(), session, studyTitle);

                        // 이벤트 매핑 저장
                        googleCalendarService.saveEventMapping(
                                session.getId(), member.getUserId(), eventId);

}
                } catch (Exception e) {
                    // 개별 멤버 캘린더 동기화 실패 시 로그만 남기고 계속 진행
}
            }
        } catch (Exception e) {
}
    }

    /**
     * 세션 수정 시 멤버 캘린더 이벤트 업데이트 (비동기)
     */
    @Async
    public void updateSessionInMemberCalendarsAsync(Long sessionId, Long studyId, String studyTitle) {
        try {
            StudySession session = studySessionRepository.findById(sessionId).orElse(null);
            if (session != null) {
                updateSessionInMemberCalendars(session, studyTitle);
            }
        } catch (Exception e) {
}
}

    /**
     * 단일 세션 캘린더 이벤트 업데이트
     */
    public void updateSessionInMemberCalendars(StudySession session, String studyTitle) {
        try {
            List<StudyMember> activeMembers = studyMemberRepository
                    .findByStudyIdAndStatus(session.getStudyId(), MemberStatus.APPROVED);

            for (StudyMember member : activeMembers) {
                try {
                    googleCalendarService.getEventMapping(session.getId(), member.getUserId())
                            .ifPresent(mapping -> {
                                googleCalendarService.updateEvent(
                                        member.getUserId(), session,
                                        mapping.getGoogleEventId(), studyTitle);
                            });
                } catch (Exception e) {
}
            }
        } catch (Exception e) {
}
    }

    /**
     * 세션 삭제 시 멤버 캘린더 이벤트 삭제 (비동기)
     */
    @Async
    public void deleteSessionFromMemberCalendarsAsync(Long sessionId, Long studyId) {
        try {
            List<StudyMember> activeMembers = studyMemberRepository
                    .findByStudyIdAndStatus(studyId, MemberStatus.APPROVED);

            for (StudyMember member : activeMembers) {
                try {
                    googleCalendarService.getEventMapping(sessionId, member.getUserId())
                            .ifPresent(mapping -> {
                                googleCalendarService.deleteEvent(
                                        member.getUserId(), mapping.getGoogleEventId());
                                googleCalendarService.deleteEventMapping(
                                        sessionId, member.getUserId());
                            });
                } catch (Exception e) {
}
            }
        } catch (Exception e) {
}
}
}

