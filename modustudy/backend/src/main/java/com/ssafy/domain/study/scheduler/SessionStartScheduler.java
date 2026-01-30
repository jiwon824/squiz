package com.ssafy.domain.study.scheduler;

import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.SessionStatus;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyMember;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 세션 시작 시간 자동 알림 스케줄러
 * 1분마다 실행되어 시작 시간이 된 세션을 찾아 멤버들에게 알림 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionStartScheduler {

    private final StudySessionRepository studySessionRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final NotificationService notificationService;

    /**
     * 1분마다 실행 - 세션 시작 시간이 된 세션 자동 알림
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkAndNotifySessionStart() {
        LocalDateTime now = LocalDateTime.now();

        // 현재 시간 기준 -1분 ~ +1분 사이에 시작 예정인 세션 조회
        List<StudySession> sessions = studySessionRepository.findByScheduledAtBetween(
                now.minusMinutes(1),
                now.plusMinutes(1)
        );

        for (StudySession session : sessions) {
            // SCHEDULED 상태인 세션만 처리 (이미 시작된 세션 제외)
            if (session.getStatus() != SessionStatus.SCHEDULED) {
                continue;
            }

            try {
                // 세션 시작 처리
                session.start();
                log.info("세션 자동 시작 - sessionId: {}, studyId: {}", session.getId(), session.getStudyId());

                // 알림 전송
                sendSessionStartNotification(session);
            } catch (Exception e) {
                log.error("세션 자동 시작 실패 - sessionId: {}, error: {}", session.getId(), e.getMessage());
            }
        }
    }

    /**
     * 스터디 멤버들에게 세션 시작 알림 전송
     */
    private void sendSessionStartNotification(StudySession session) {
        try {
            // 스터디 정보 조회
            Study study = studyRepository.findById(session.getStudyId())
                    .orElse(null);

            if (study == null) {
                log.warn("스터디를 찾을 수 없음 - studyId: {}", session.getStudyId());
                return;
            }

            // 활성 스터디 멤버 조회
            List<StudyMember> activeMembers = studyMemberRepository
                    .findByStudyIdAndStatus(session.getStudyId(), MemberStatus.APPROVED);

            String notificationTitle = "스터디 세션이 시작되었습니다";
            String notificationContent = String.format("'%s' 스터디의 %d회차 세션이 시작되었습니다. 지금 참여하세요!",
                    study.getName(), session.getSessionNumber());

            for (StudyMember member : activeMembers) {
                try {
                    notificationService.createNotification(
                            member.getUserId(),
                            NotificationType.SCHEDULE,
                            notificationTitle,
                            notificationContent,
                            "STUDY_SESSION",
                            study.getId()
                    );
                } catch (Exception e) {
                    log.warn("세션 시작 알림 전송 실패 - userId: {}, error: {}",
                            member.getUserId(), e.getMessage());
                }
            }

            log.info("세션 시작 알림 전송 완료 - studyId: {}, sessionId: {}, memberCount: {}",
                    study.getId(), session.getId(), activeMembers.size());
        } catch (Exception e) {
            log.error("세션 시작 알림 전송 중 오류 발생 - sessionId: {}, error: {}",
                    session.getId(), e.getMessage());
        }
    }
}
