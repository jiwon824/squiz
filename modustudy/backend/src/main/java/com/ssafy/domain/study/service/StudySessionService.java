package com.ssafy.domain.study.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.calendar.service.GoogleCalendarService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final GoogleCalendarService googleCalendarService;

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
        log.info("세션 생성 완료 - studyId: {}, sessionNumber: {}", studyId, nextSessionNumber);

        // 스터디 멤버들의 Google Calendar에 이벤트 자동 추가
        syncSessionToMemberCalendars(saved, study.getTitle());

        return StudySessionResponse.from(saved);
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

        // 세션 정보 수정 (Entity 비즈니스 로직 활용)
        session.updateInfo(
                request.getTitle(),
                request.getDescription(),
                request.getScheduledAt(),
                request.getDurationMinutes(),
                request.getLocation(),
                request.getIsOnline()
        );

        log.info("세션 수정 완료 - sessionId: {}", sessionId);

        // Google Calendar 이벤트도 업데이트
        updateSessionInMemberCalendars(session, study.getTitle());

        return StudySessionResponse.from(session);
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

        // Google Calendar 이벤트 삭제
        googleCalendarService.deleteEventMappings(sessionId);

        studySessionRepository.delete(session);
        log.info("세션 삭제 완료 - sessionId: {}", sessionId);
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
        log.info("세션 시작 - sessionId: {}", sessionId);

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
        log.info("세션 완료 - sessionId: {}", sessionId);

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
        log.info("세션 취소 - sessionId: {}", sessionId);

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
     * 스터디 멤버들의 Google Calendar에 세션 이벤트 동기화
     */
    private void syncSessionToMemberCalendars(StudySession session, String studyTitle) {
        try {
            // 활성 스터디 멤버 조회
            List<StudyMember> activeMembers = studyMemberRepository
                    .findByStudyIdAndStatus(session.getStudyId(), MemberStatus.ACTIVE);

            for (StudyMember member : activeMembers) {
                try {
                    // Google Calendar 연동 확인
                    if (googleCalendarService.isCalendarLinked(member.getUserId())) {
                        String eventId = googleCalendarService.createEvent(
                                member.getUserId(), session, studyTitle);

                        // 이벤트 매핑 저장
                        googleCalendarService.saveEventMapping(
                                session.getId(), member.getUserId(), eventId);

                        log.debug("캘린더 이벤트 생성 - userId: {}, sessionId: {}",
                                member.getUserId(), session.getId());
                    }
                } catch (Exception e) {
                    // 개별 멤버 캘린더 동기화 실패 시 로그만 남기고 계속 진행
                    log.warn("캘린더 이벤트 생성 실패 - userId: {}, error: {}",
                            member.getUserId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("캘린더 동기화 중 오류 발생 - sessionId: {}, error: {}",
                    session.getId(), e.getMessage());
        }
    }

    /**
     * 세션 수정 시 Google Calendar 이벤트도 업데이트
     */
    private void updateSessionInMemberCalendars(StudySession session, String studyTitle) {
        try {
            List<StudyMember> activeMembers = studyMemberRepository
                    .findByStudyIdAndStatus(session.getStudyId(), MemberStatus.ACTIVE);

            for (StudyMember member : activeMembers) {
                try {
                    googleCalendarService.getEventMapping(session.getId(), member.getUserId())
                            .ifPresent(mapping -> {
                                googleCalendarService.updateEvent(
                                        member.getUserId(), session,
                                        mapping.getGoogleEventId(), studyTitle);
                            });
                } catch (Exception e) {
                    log.warn("캘린더 이벤트 수정 실패 - userId: {}, error: {}",
                            member.getUserId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("캘린더 수정 동기화 중 오류 발생 - sessionId: {}", session.getId());
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
}