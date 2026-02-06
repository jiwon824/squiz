package com.ssafy.domain.study.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.calendar.service.CalendarSyncService;
import com.ssafy.domain.study.dto.request.StudySessionCreateRequest;
import com.ssafy.domain.study.dto.request.StudySessionUpdateRequest;
import com.ssafy.domain.study.dto.response.StudySessionResponse;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.SessionStatus;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyMember;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final StudyRepository studyRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final NotificationService notificationService;
    private final CalendarSyncService calendarSyncService;

    /**
     * 세션 생성
     */
    @Transactional
    public StudySessionResponse createSession(Long studyId, Long userId, StudySessionCreateRequest request) {
        // 스터디 존재 확인 및 권한 검증
        Study study = getStudyOrThrow(studyId);
        validateStudyLeader(study, userId);

        // 다음 회차 번호 계산
        Integer nextSessionNumber = studySessionRepository.findMaxSessionNumberByStudyId(studyId) + 1;

        // 세션 생성
        StudySession session = StudySession.builder()
                .studyId(studyId)
                .sessionNumber(nextSessionNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 60)
                .location(request.getLocation())
                .isOnline(request.getIsOnline() != null ? request.getIsOnline() : true)
                .build();

        StudySession saved = studySessionRepository.save(session);
        studySessionRepository.flush();
// 날짜 순서에 따라 세션 번호 재정렬
        reorderSessionsByScheduledAt(studyId);

        // 재정렬 후 최신 세션 정보 조회
        StudySession reorderedSession = getSessionOrThrow(saved.getId());

        // 스터디 멤버들의 Google Calendar에 이벤트 자동 추가 (트랜잭션 커밋 후 비동기 처리)
        String studyName = study.getName();
        Long sessionId = reorderedSession.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                calendarSyncService.syncSessionsToMemberCalendarsAsync(List.of(sessionId), studyId, studyName);
            }
        });

        return StudySessionResponse.from(reorderedSession);
    }


    /**
     * 세션 일괄 생성 (커리큘럼용)
     * 하나의 트랜잭션으로 여러 세션을 효율적으로 생성
     */
    @Transactional
    public List<StudySessionResponse> createSessionsBulk(Long studyId, Long userId, List<StudySessionCreateRequest> requests) {
        // 스터디 존재 확인 및 권한 검증
        Study study = getStudyOrThrow(studyId);
        validateStudyLeader(study, userId);

        // 현재 최대 회차 번호 조회
        Integer currentMaxNumber = studySessionRepository.findMaxSessionNumberByStudyId(studyId);

        // 세션 일괄 생성
        List<StudySession> sessions = new java.util.ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            StudySessionCreateRequest request = requests.get(i);
            StudySession session = StudySession.builder()
                    .studyId(studyId)
                    .sessionNumber(currentMaxNumber + i + 1)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .scheduledAt(request.getScheduledAt())
                    .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 60)
                    .location(request.getLocation())
                    .isOnline(request.getIsOnline() != null ? request.getIsOnline() : true)
                    .build();
            sessions.add(session);
        }

        // 일괄 저장
        List<StudySession> savedSessions = studySessionRepository.saveAll(sessions);
        studySessionRepository.flush();
// 날짜 순서에 따라 세션 번호 재정렬
        reorderSessionsByScheduledAt(studyId);

        // 재정렬 후 최신 세션 목록 조회
        List<StudySession> reorderedSessions = studySessionRepository.findByStudyIdOrderByScheduledAtAsc(studyId);

        // Google Calendar 동기화 (트랜잭션 커밋 후 비동기 처리)
        String studyName = study.getName();
        List<Long> sessionIds = reorderedSessions.stream().map(StudySession::getId).toList();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                calendarSyncService.syncSessionsToMemberCalendarsAsync(sessionIds, studyId, studyName);
            }
        });

        return reorderedSessions.stream()
                .map(StudySessionResponse::from)
                .toList();
    }

    /**
     * 세션 단건 조회
     */
    public StudySessionResponse getSession(Long studyId, Long sessionId) {
        StudySession session = getSessionOrThrow(sessionId);
        validateSessionBelongsToStudy(session, studyId);

        return StudySessionResponse.from(session);
    }

    /**
     * 세션 회차로 조회
     */
    public StudySessionResponse getSessionByNumber(Long studyId, Integer sessionNumber) {
        StudySession session = studySessionRepository.findByStudyIdAndSessionNumber(studyId, sessionNumber)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND",
                        "세션을 찾을 수 없습니다: " + sessionNumber + "회차"));

        return StudySessionResponse.from(session);
    }

    /**
     * 스터디별 세션 목록 조회 (회차 순)
     */
    public List<StudySessionResponse> getSessionsByStudyId(Long studyId) {
        // 스터디 존재 확인
        getStudyOrThrow(studyId);

        return studySessionRepository.findByStudyId(studyId).stream()
                .sorted(Comparator.comparing(StudySession::getSessionNumber))
                .map(StudySessionResponse::from)
                .toList();
    }

    /**
     * 스터디별 특정 상태 세션 목록 조회 (회차 순)
     */
    public List<StudySessionResponse> getSessionsByStatus(Long studyId, SessionStatus status) {
        // 스터디 존재 확인
        getStudyOrThrow(studyId);

        return studySessionRepository.findByStudyIdAndStatus(studyId, status).stream()
                .sorted(Comparator.comparing(StudySession::getSessionNumber))
                .map(StudySessionResponse::from)
                .toList();
    }

    /**
     * 다음 예정된 세션 조회
     */
    public StudySessionResponse getNextSession(Long studyId) {
        // 스터디 존재 확인
        getStudyOrThrow(studyId);

        StudySession session = studySessionRepository.findNextScheduledSession(studyId, LocalDateTime.now())
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "예정된 세션이 없습니다"));

        return StudySessionResponse.from(session);
    }

    /**
     * 세션 수정
     */
    @Transactional
    public StudySessionResponse updateSession(Long studyId, Long sessionId, Long userId,
                                              StudySessionUpdateRequest request) {
        // 스터디 존재 확인 및 권한 검증
        Study study = getStudyOrThrow(studyId);
        validateStudyLeader(study, userId);

        // 세션 조회 및 소속 확인
        StudySession session = getSessionOrThrow(sessionId);
        validateSessionBelongsToStudy(session, studyId);

        // 날짜 변경 여부 확인 (재정렬 필요 여부)
        boolean scheduledAtChanged = request.getScheduledAt() != null
                && !request.getScheduledAt().equals(session.getScheduledAt());

        // 세션 정보 수정 (Entity 비즈니스 로직 활용)
        session.updateInfo(
                request.getTitle(),
                request.getDescription(),
                request.getScheduledAt(),
                request.getDurationMinutes(),
                request.getLocation(),
                request.getIsOnline()
        );

// 날짜가 변경된 경우 세션 번호 재정렬
        if (scheduledAtChanged) {
            studySessionRepository.flush();
            reorderSessionsByScheduledAt(studyId);
        }

        // 재정렬 후 최신 세션 정보 조회
        StudySession updatedSession = getSessionOrThrow(sessionId);

        // Google Calendar 이벤트도 업데이트 (비동기)
        calendarSyncService.updateSessionInMemberCalendarsAsync(sessionId, studyId, study.getName());

        return StudySessionResponse.from(updatedSession);
    }

    /**
     * 세션 삭제
     */
    @Transactional
    public void deleteSession(Long studyId, Long sessionId, Long userId) {
        // 스터디 존재 확인 및 권한 검증
        Study study = getStudyOrThrow(studyId);
        validateStudyLeader(study, userId);

        // 세션 조회 및 소속 확인
        StudySession session = getSessionOrThrow(sessionId);
        validateSessionBelongsToStudy(session, studyId);

        // 완료된 세션은 삭제 불가
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new IllegalStateException("완료된 세션은 삭제할 수 없습니다");
        }

        // Google Calendar 이벤트 삭제 (비동기)
        calendarSyncService.deleteSessionFromMemberCalendarsAsync(sessionId, studyId);

        studySessionRepository.delete(session);
        studySessionRepository.flush();
// 삭제 후 남은 세션 번호 재정렬
        reorderSessionsByScheduledAt(studyId);
    }

    /**
     * 세션 시작
     */
    @Transactional
    public StudySessionResponse startSession(Long studyId, Long sessionId, Long userId) {
        // 스터디 존재 확인 및 권한 검증
        Study study = getStudyOrThrow(studyId);
        validateStudyLeader(study, userId);

        // 세션 조회 및 소속 확인
        StudySession session = getSessionOrThrow(sessionId);
        validateSessionBelongsToStudy(session, studyId);

        // 세션 시작 (Entity 비즈니스 로직)
        session.start();
// 스터디 멤버들에게 세션 시작 알림 전송
        sendSessionStartNotification(study, session);

        return StudySessionResponse.from(session);
    }

    /**
     * 세션 완료
     */
    @Transactional
    public StudySessionResponse completeSession(Long studyId, Long sessionId, Long userId) {
        // 스터디 존재 확인 및 권한 검증
        Study study = getStudyOrThrow(studyId);
        validateStudyLeader(study, userId);

        // 세션 조회 및 소속 확인
        StudySession session = getSessionOrThrow(sessionId);
        validateSessionBelongsToStudy(session, studyId);

        // 세션 완료 (Entity 비즈니스 로직)
        session.complete();
        return StudySessionResponse.from(session);
    }

    /**
     * 세션 취소
     */
    @Transactional
    public StudySessionResponse cancelSession(Long studyId, Long sessionId, Long userId) {
        // 스터디 존재 확인 및 권한 검증
        Study study = getStudyOrThrow(studyId);
        validateStudyLeader(study, userId);

        // 세션 조회 및 소속 확인
        StudySession session = getSessionOrThrow(sessionId);
        validateSessionBelongsToStudy(session, studyId);

        // 세션 취소 (Entity 비즈니스 로직)
        session.cancel();
        return StudySessionResponse.from(session);
    }

    /**
     * 스터디별 세션 통계 조회
     */
    public SessionStatistics getSessionStatistics(Long studyId) {
        // 스터디 존재 확인
        getStudyOrThrow(studyId);

        long totalCount = studySessionRepository.countByStudyId(studyId);
        long completedCount = studySessionRepository.countByStudyIdAndStatus(studyId, SessionStatus.COMPLETED);
        long scheduledCount = studySessionRepository.countByStudyIdAndStatus(studyId, SessionStatus.SCHEDULED);
        long cancelledCount = studySessionRepository.countByStudyIdAndStatus(studyId, SessionStatus.CANCELLED);

        return new SessionStatistics(totalCount, completedCount, scheduledCount, cancelledCount);
    }

    /**
     * 미팅 결과에 맞춰 세션 진행 시간을 갱신
     */
    @Transactional
    public void updateDurationFromMeeting(Long studyId, Long sessionId, Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return;
        }
        Study study = getStudyOrThrow(studyId);
        StudySession session = getSessionOrThrow(sessionId);
        validateSessionBelongsToStudy(session, studyId);

        session.setDurationMinutes(durationMinutes);
        calendarSyncService.updateSessionInMemberCalendarsAsync(sessionId, studyId, study.getName());
    }

    /**
     * 세션 번호를 scheduledAt 기준으로 재정렬
     * 날짜 순서대로 sessionNumber를 1부터 다시 할당
     */
    private void reorderSessionsByScheduledAt(Long studyId) {
        // scheduledAt 기준 오름차순으로 세션 조회
        List<StudySession> sessions = studySessionRepository.findByStudyIdOrderByScheduledAtAsc(studyId);

        if (sessions.isEmpty()) {
            return;
        }

        // 기존 sessionNumber 목록 확인하여 재정렬 필요 여부 체크
        boolean needsReorder = false;
        for (int i = 0; i < sessions.size(); i++) {
            if (!sessions.get(i).getSessionNumber().equals(i + 1)) {
                needsReorder = true;
                break;
            }
        }

        if (!needsReorder) {
            return;
        }

        // unique constraint 충돌 방지를 위해 일시적으로 음수로 변경
        for (int i = 0; i < sessions.size(); i++) {
            sessions.get(i).setSessionNumber(-(i + 1));
        }
        studySessionRepository.saveAll(sessions);
        studySessionRepository.flush();

        // 다시 양수로 할당
        for (int i = 0; i < sessions.size(); i++) {
            sessions.get(i).setSessionNumber(i + 1);
        }
        studySessionRepository.saveAll(sessions);

}

    private Study getStudyOrThrow(Long studyId) {
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyException.StudyNotFoundException(studyId));
    }

    private StudySession getSessionOrThrow(Long sessionId) {
        return studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다"));
    }

    private void validateStudyLeader(Study study, Long userId) {
        if (!study.getLeaderId().equals(userId)) {
            throw new StudyException.NotStudyLeaderException("스터디장만 세션을 관리할 수 있습니다");
        }
    }

    private void validateSessionBelongsToStudy(StudySession session, Long studyId) {
        if (!session.getStudyId().equals(studyId)) {
            throw new NotFoundException("SESSION_NOT_FOUND", "해당 스터디의 세션이 아닙니다");
        }
    }

    /**
     * 세션 통계 DTO
     */
    public record SessionStatistics(
            long totalCount,
            long completedCount,
            long scheduledCount,
            long cancelledCount
    ) {
        public double getCompletionRate() {
            return totalCount == 0 ? 0 : (double) completedCount / totalCount * 100;
        }
    }

    /**
     * 스터디 멤버들에게 세션 시작 알림 전송
     */
    private void sendSessionStartNotification(Study study, StudySession session) {
        try {
            // 활성 스터디 멤버 조회
            List<StudyMember> activeMembers = studyMemberRepository
                    .findByStudyIdAndStatus(session.getStudyId(), MemberStatus.APPROVED);

            String notificationTitle = "스터디 세션이 시작되었습니다";
            String notificationContent = String.format("'%s' 스터디의 %d회차 세션이 시작되었습니다. 지금 참여하세요!",
                    study.getName(), session.getSessionNumber());

            for (StudyMember member : activeMembers) {
                try {
                    // referenceType: STUDY_SESSION, referenceId: studyId (워크스페이스 이동용)
                    notificationService.createNotification(
                            member.getUserId(),
                            NotificationType.SCHEDULE,
                            notificationTitle,
                            notificationContent,
                            "STUDY_SESSION",
                            study.getId()
                    );
} catch (Exception e) {
                    // 개별 멤버 알림 실패 시 로그만 남기고 계속 진행
}
            }

} catch (Exception e) {
}
    }

    /**
     * 사용자가 참여한 모든 스터디의 세션 조회 (기간별)
     */
    public List<StudySessionResponse> getMyStudySessions(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        // 사용자가 참여한 스터디 목록 조회
        List<StudyMember> myStudies = studyMemberRepository.findByUserIdAndStatus(userId, MemberStatus.APPROVED);
        
        if (myStudies.isEmpty()) {
            return List.of();
        }
        
        List<Long> studyIds = myStudies.stream()
                .map(StudyMember::getStudyId)
                .toList();
        
        // 해당 스터디들의 세션 조회
        List<StudySession> sessions = studySessionRepository.findByStudyIdInAndScheduledAtBetween(
                studyIds, startDate, endDate);
        
        return sessions.stream()
                .map(StudySessionResponse::from)
                .toList();
    }
}

