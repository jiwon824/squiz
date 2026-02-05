package com.ssafy.domain.attendance.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.response.MessageResponse;
import com.ssafy.domain.attendance.dto.request.AttendanceExcuseDecisionRequest;
import com.ssafy.domain.attendance.dto.request.AttendanceExcuseRequest;
import com.ssafy.domain.attendance.dto.request.AttendanceManualUpdateRequest;
import com.ssafy.domain.attendance.dto.response.AttendanceCalendarResponse;
import com.ssafy.domain.attendance.dto.response.AttendanceResponse;
import com.ssafy.domain.attendance.dto.response.SessionAttendanceInfoResponse;
import com.ssafy.domain.attendance.dto.response.SessionAttendanceMemberResponse;
import com.ssafy.domain.attendance.entity.Attendance;
import com.ssafy.domain.attendance.entity.AttendanceCheckType;
import com.ssafy.domain.attendance.entity.AttendanceExcuseStatus;
import com.ssafy.domain.attendance.entity.AttendanceStatus;
import com.ssafy.domain.attendance.repository.AttendanceRepository;
import com.ssafy.domain.gamification.event.StudyAttendanceEvent;
import com.ssafy.domain.notification.entity.NotificationType;
import com.ssafy.domain.notification.service.NotificationService;
import com.ssafy.domain.study.entity.MemberRole;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyMember;
import com.ssafy.domain.study.entity.StudySession;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {
    private static final int SELF_ATTENDANCE_DELAY_MINUTES = 15;
    private static final int LATE_THRESHOLD_MINUTES = 10;

    // 세션 시간 검증용 상수
    private static final int ATTENDANCE_WINDOW_BEFORE_MINUTES = 30;  // 세션 시작 30분 전부터 출석 가능
    private static final int ATTENDANCE_WINDOW_AFTER_MINUTES = 30;   // 세션 종료 30분 후까지 출석 가능

    private final AttendanceRepository attendanceRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    public MessageResponse startBleAttendance(Long studyId, Long sessionId, Long leaderId) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateLeader(studyId, leaderId);
        validateAttendanceTimeWindow(session);
        initializeAttendanceRows(session);
        return new MessageResponse("BLE 출석이 시작되었습니다.");
    }

    public AttendanceResponse checkAttendanceBle(Long studyId, Long sessionId, Long userId) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateMember(studyId, userId);
        validateAttendanceTimeWindow(session);
        Attendance attendance = getOrCreateAttendance(session, userId);
        updateCheckInfo(attendance, AttendanceCheckType.BLE, session);
        attendance.setCheckedBy(getStudyLeaderUser(studyId));
        Attendance saved = attendanceRepository.save(attendance);

        // 게이미피케이션 이벤트 발행
        publishAttendanceEvent(saved, studyId);

        return AttendanceResponse.from(saved);
    }

    public AttendanceResponse checkAttendanceSelf(Long studyId, Long sessionId, Long userId) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateMember(studyId, userId);
        validateAttendanceTimeWindow(session);
        validateSelfAttendanceWindow(session, studyId);
        Attendance attendance = getOrCreateAttendance(session, userId);
        updateCheckInfo(attendance, AttendanceCheckType.SELF, session);
        attendance.setCheckedBy(null);
        Attendance saved = attendanceRepository.save(attendance);

        // 게이미피케이션 이벤트 발행
        publishAttendanceEvent(saved, studyId);

        return AttendanceResponse.from(saved);
    }

    @Transactional
    public AttendanceResponse checkAttendanceAutoOnline(Long studyId, Long sessionId, Long userId) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateMember(studyId, userId);
        validateAttendanceTimeWindow(session);
        validateAutoAttendanceSession(session);
        Attendance attendance = getOrCreateAttendance(session, userId);
        updateCheckInfo(attendance, AttendanceCheckType.AUTO, session);
        attendance.setCheckedBy(null);
        Attendance saved = attendanceRepository.save(attendance);

        // 게이미피케이션 이벤트 발행
        publishAttendanceEvent(saved, studyId);

        return AttendanceResponse.from(saved);
    }

    /**
     * 출석 성공 시 게이미피케이션 이벤트 발행
     */
    private void publishAttendanceEvent(Attendance attendance, Long studyId) {
        if (attendance.getStatus() == AttendanceStatus.PRESENT ||
            attendance.getStatus() == AttendanceStatus.LATE) {
            try {
                Study study = getStudyOrThrow(studyId);
                eventPublisher.publishEvent(new StudyAttendanceEvent(
                    this,
                    attendance.getUser().getId(),
                    studyId,
                    study.getName(),
                    LocalDate.now()
                ));
                log.info("[Attendance] 게이미피케이션 이벤트 발행: userId={}, studyId={}",
                    attendance.getUser().getId(), studyId);
            } catch (Exception e) {
                log.warn("[Attendance] 게이미피케이션 이벤트 발행 실패: {}", e.getMessage());
            }
        }
    }

    /**
     * 미팅 종료 시 참가하지 않은 멤버를 ABSENT로 처리
     */
    @Transactional
    public void markAbsentForNonParticipants(Long studyId, Long sessionId, List<Long> participantUserIds) {
        StudySession session = studySessionRepository.findById(sessionId).orElse(null);
        if (session == null) return;

        // 스터디 멤버 목록 조회
        List<StudyMember> members = studyMemberRepository.findByStudyIdAndStatus(studyId, MemberStatus.APPROVED);

        for (StudyMember member : members) {
            Long userId = member.getUserId();
            // 참가자 목록에 없으면 ABSENT 처리
            if (!participantUserIds.contains(userId)) {
                Attendance attendance = getOrCreateAttendance(session, userId);
                // 이미 출석 처리된 경우 건너뛰기
                if (attendance.getStatus() == AttendanceStatus.PRESENT ||
                    attendance.getStatus() == AttendanceStatus.LATE) {
                    continue;
                }
                attendance.setStatus(AttendanceStatus.ABSENT);
                attendance.setCheckType(AttendanceCheckType.AUTO);
                attendanceRepository.save(attendance);
            }
        }
    }

    public AttendanceResponse updateAttendanceStatus(
            Long studyId,
            Long sessionId,
            Long leaderId,
            Long targetUserId,
            AttendanceManualUpdateRequest request
    ) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateLeader(studyId, leaderId);
        Attendance attendance = getOrCreateAttendance(session, targetUserId);
        AttendanceStatus previousStatus = attendance.getStatus();
        attendance.setStatus(request.status());
        attendance.setCheckType(AttendanceCheckType.SELF);
        attendance.setCheckedBy(getUserOrThrow(leaderId));
        if (request.reason() != null && !request.reason().isBlank()) {
            attendance.setExcuseReason(request.reason());
        }
        Attendance saved = attendanceRepository.save(attendance);

        // 이전 상태가 출석이 아니었고, 새 상태가 출석이면 이벤트 발행
        if (previousStatus != AttendanceStatus.PRESENT && previousStatus != AttendanceStatus.LATE) {
            publishAttendanceEvent(saved, studyId);
        }

        return AttendanceResponse.from(saved);
    }

    /**
     * 세션 출석 현황 조회 (스터디장 화면용)
     * 출석 체크된 멤버를 먼저 보여주고, 출석 시간 역순으로 정렬
     * 스터디장 본인은 출석 대상에서 제외
     */
    @Transactional(readOnly = true)
    public SessionAttendanceInfoResponse getSessionAttendance(Long studyId, Long sessionId, Long requesterId) {
        validateLeader(studyId, requesterId);
        StudySession session = getSessionOrThrow(sessionId, studyId);

        // 스터디장 ID 조회
        Study study = getStudyOrThrow(studyId);
        Long leaderId = study.getLeaderId();

        // 스터디 멤버 목록 조회 (스터디장 제외)
        List<StudyMember> members = studyMemberRepository.findByStudyIdAndStatus(studyId, MemberStatus.APPROVED)
                .stream()
                .filter(m -> !m.getUserId().equals(leaderId))
                .toList();

        // 기존 출석 기록 조회
        List<Attendance> attendances = attendanceRepository.findBySessionId(sessionId);
        Map<Long, Attendance> attendanceMap = attendances.stream()
                .filter(a -> a.getUser() != null)
                .collect(Collectors.toMap(a -> a.getUser().getId(), a -> a, (a1, a2) -> a1));

        // 멤버별 출석 현황 생성
        List<SessionAttendanceMemberResponse> memberResponses = new ArrayList<>();
        int presentCount = 0;

        for (StudyMember member : members) {
            Attendance attendance = attendanceMap.get(member.getUserId());
            User user = getUserOrThrow(member.getUserId());

            if (attendance != null) {
                memberResponses.add(SessionAttendanceMemberResponse.from(attendance));
                if (attendance.getStatus() == AttendanceStatus.PRESENT ||
                    attendance.getStatus() == AttendanceStatus.LATE) {
                    presentCount++;
                }
            } else {
                // 출석 기록이 없는 멤버는 PENDING 상태로 추가
                memberResponses.add(SessionAttendanceMemberResponse.pending(
                        user.getId(),
                        user.getNickname(),
                        user.getProfileImage()
                ));
            }
        }

        // 정렬: 출석 체크된 멤버를 먼저 (출석 시간 역순), 그 다음 대기 중인 멤버
        memberResponses.sort(Comparator
                .comparing((SessionAttendanceMemberResponse m) -> {
                    // PRESENT, LATE는 0, 나머지는 1 (출석한 멤버가 먼저)
                    String status = m.status();
                    return "PRESENT".equals(status) || "LATE".equals(status) ? 0 : 1;
                })
                .thenComparing((SessionAttendanceMemberResponse m) -> {
                    // 출석 시간 역순 (최근 출석이 먼저)
                    return m.checkedAt() != null ? m.checkedAt() : "";
                }, Comparator.reverseOrder())
        );

        // 세션 제목 생성
        String sessionTitle = session.getTitle() != null && !session.getTitle().isBlank()
                ? session.getTitle()
                : session.getSessionNumber() + "회차 세션";

        return SessionAttendanceInfoResponse.of(
                sessionId,
                sessionTitle,
                members.size(),
                presentCount,
                memberResponses
        );
    }

    @Transactional(readOnly = true)
    public AttendanceCalendarResponse getMonthlyCalendar(Long studyId, Long userId, int year, int month) {
        validateMember(studyId, userId);
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);
        List<Attendance> items = attendanceRepository.findByUserIdAndSessionScheduledAtBetween(userId, start, end);
        List<AttendanceCalendarResponse.AttendanceCalendarItem> responses = new ArrayList<>();
        for (Attendance item : items) {
            LocalDateTime scheduledAt = item.getSession().getScheduledAt();
            responses.add(new AttendanceCalendarResponse.AttendanceCalendarItem(
                    scheduledAt.toLocalDate(),
                    item.getSession().getId(),
                    item.getStatus(),
                    item.getExcuseStatus(),
                    item.getCheckType(),
                    scheduledAt
            ));
        }
        return new AttendanceCalendarResponse(year, month, responses);
    }

    public AttendanceResponse submitExcuse(Long studyId, Long sessionId, Long userId, AttendanceExcuseRequest request) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateMember(studyId, userId);
        Attendance attendance = getOrCreateAttendance(session, userId);
        attendance.setExcuseReason(request.reason());
        attendance.setExcuseStatus(AttendanceExcuseStatus.PENDING);
        if (attendance.getStatus() == null || attendance.getStatus() == AttendanceStatus.PRESENT) {
            attendance.setStatus(AttendanceStatus.ABSENT);
        }
        Attendance saved = attendanceRepository.save(attendance);

        try {
            Study study = getStudyOrThrow(studyId);
            User submitter = getUserOrThrow(userId);
            Long leaderId = study.getLeaderId();
            if (leaderId != null && !leaderId.equals(userId)) {
                String title = "결석 소명이 제출되었습니다";
                String sessionLabel = session.getTitle() != null && !session.getTitle().isBlank()
                        ? session.getTitle()
                        : session.getSessionNumber() + "회차 세션";
                String submitterName = submitter.getNickname() != null ? submitter.getNickname() : submitter.getName();
                String content = String.format("'%s' %s에 %s님이 결석 소명을 제출했습니다.",
                        study.getName(), sessionLabel, submitterName);
                notificationService.createNotification(
                        leaderId,
                        NotificationType.ATTENDANCE,
                        title,
                        content,
                        "STUDY_EXCUSE",
                        studyId
                );
            }
        } catch (Exception e) {
            // 알림 실패는 출결 처리에 영향 주지 않음
        }

        return AttendanceResponse.from(saved);
    }

    public AttendanceResponse decideExcuse(
            Long studyId,
            Long sessionId,
            Long leaderId,
            Long targetUserId,
            AttendanceExcuseDecisionRequest request
    ) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateLeader(studyId, leaderId);
        Attendance attendance = getOrCreateAttendance(session, targetUserId);
        if (request.status() == AttendanceExcuseStatus.APPROVED) {
            attendance.setStatus(AttendanceStatus.PRESENT);
            attendance.setExcuseStatus(AttendanceExcuseStatus.APPROVED);
        } else if (request.status() == AttendanceExcuseStatus.REJECTED) {
            attendance.setStatus(AttendanceStatus.ABSENT);
            attendance.setExcuseStatus(AttendanceExcuseStatus.REJECTED);
        }
        attendance.setCheckedBy(getUserOrThrow(leaderId));
        Attendance saved = attendanceRepository.save(attendance);

        // 소명 승인 시 게이미피케이션 이벤트 발행
        if (request.status() == AttendanceExcuseStatus.APPROVED) {
            publishAttendanceEvent(saved, studyId);
        }

        try {
            Study study = getStudyOrThrow(studyId);
            String title = request.status() == AttendanceExcuseStatus.APPROVED
                    ? "결석 소명이 승인되었습니다"
                    : "결석 소명이 거절되었습니다";
            String sessionLabel = session.getTitle() != null && !session.getTitle().isBlank()
                    ? session.getTitle()
                    : session.getSessionNumber() + "회차 세션";
            String content = String.format("'%s' %s의 결석 소명이 %s되었습니다.",
                    study.getName(),
                    sessionLabel,
                    request.status() == AttendanceExcuseStatus.APPROVED ? "승인" : "거절");
            notificationService.createNotification(
                    targetUserId,
                    NotificationType.ATTENDANCE,
                    title,
                    content,
                    "STUDY_SESSION",
                    studyId
            );
        } catch (Exception e) {
            // 알림 실패는 출결 처리에 영향 주지 않음
        }

        return AttendanceResponse.from(saved);
    }

    public void initializeAttendanceRows(StudySession session) {
        List<StudyMember> members = studyMemberRepository.findByStudyIdAndStatus(
                session.getStudyId(),
                MemberStatus.APPROVED
        );
        for (StudyMember member : members) {
            attendanceRepository.findBySessionIdAndUserId(session.getId(), member.getUserId())
                    .orElseGet(() -> {
                        Attendance created = Attendance.builder()
                                .session(session)
                                .user(getUserOrThrow(member.getUserId()))
                                .status(AttendanceStatus.ABSENT)
                                .checkType(AttendanceCheckType.SELF)
                                .build();
                        return attendanceRepository.save(created);
                    });
        }
    }

    public void finalizeAttendanceForSession(StudySession session) {
        initializeAttendanceRows(session);
        List<Attendance> items = attendanceRepository.findBySessionId(session.getId());
        for (Attendance item : items) {
            if (item.getStatus() == null) {
                item.setStatus(AttendanceStatus.ABSENT);
            }
        }
    }

    private void updateCheckInfo(Attendance attendance, AttendanceCheckType checkType, StudySession session) {
        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckType(checkType);
        attendance.setCheckedAt(now);
        LocalDateTime lateThreshold = session.getScheduledAt().plusMinutes(LATE_THRESHOLD_MINUTES);
        if (now.isAfter(lateThreshold)) {
            attendance.setStatus(AttendanceStatus.LATE);
        } else {
            attendance.setStatus(AttendanceStatus.PRESENT);
        }
    }

    private Attendance getOrCreateAttendance(StudySession session, Long userId) {
        return attendanceRepository.findBySessionIdAndUserId(session.getId(), userId)
                .orElseGet(() -> Attendance.builder()
                        .session(session)
                        .user(getUserOrThrow(userId))
                        .status(AttendanceStatus.ABSENT)
                        .checkType(AttendanceCheckType.SELF)
                        .build());
    }

    private void validateSelfAttendanceWindow(StudySession session, Long studyId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime openAt = session.getScheduledAt().plusMinutes(SELF_ATTENDANCE_DELAY_MINUTES);
        if (now.isBefore(openAt)) {
            throw new IllegalStateException("셀프 출석 가능 시간이 아닙니다.");
        }
        Study study = getStudyOrThrow(studyId);
        Attendance leaderAttendance = attendanceRepository
                .findBySessionIdAndUserId(session.getId(), study.getLeaderId())
                .orElse(null);
        if (leaderAttendance != null && leaderAttendance.getStatus() != AttendanceStatus.ABSENT) {
            throw new IllegalStateException("스터디장이 이미 출석했습니다.");
        }
    }

    private void validateAutoAttendanceSession(StudySession session) {
        if (session.getIsOnline() == null || !session.getIsOnline()) {
            throw new IllegalStateException("오프라인 세션은 자동 출석을 처리할 수 없습니다.");
        }
    }

    /**
     * 세션 출석 가능 시간 검증
     */
    private void validateAttendanceTimeWindow(StudySession session) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionStart = session.getScheduledAt();
        int duration = session.getDurationMinutes() != null ? session.getDurationMinutes() : 60;
        LocalDateTime sessionEnd = sessionStart.plusMinutes(duration);

        LocalDateTime windowStart = sessionStart.minusMinutes(ATTENDANCE_WINDOW_BEFORE_MINUTES);
        LocalDateTime windowEnd = sessionEnd.plusMinutes(ATTENDANCE_WINDOW_AFTER_MINUTES);

        if (now.isBefore(windowStart)) {
            throw new IllegalStateException(
                    String.format("출석 가능 시간이 아닙니다. %s부터 출석 가능합니다.",
                            windowStart.toLocalTime().toString().substring(0, 5))
            );
        }

        if (now.isAfter(windowEnd)) {
            throw new IllegalStateException(
                    String.format("출석 가능 시간이 종료되었습니다. (세션 종료: %s)",
                            sessionEnd.toLocalTime().toString().substring(0, 5))
            );
        }
    }

    private StudySession getSessionOrThrow(Long sessionId, Long studyId) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다."));
        if (!session.getStudyId().equals(studyId)) {
            throw new NotFoundException("SESSION_NOT_FOUND", "해당 스터디의 세션이 아닙니다.");
        }
        return session;
    }

    private Study getStudyOrThrow(Long studyId) {
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new NotFoundException("STUDY_NOT_FOUND", "스터디를 찾을 수 없습니다."));
    }

    private void validateMember(Long studyId, Long userId) {
        StudyMember member = studyMemberRepository.findByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> new NotFoundException("STUDY_MEMBER_NOT_FOUND", "스터디 멤버가 아닙니다."));
        if (member.getStatus() != MemberStatus.APPROVED) {
            throw new IllegalStateException("승인된 스터디 멤버가 아닙니다.");
        }
    }

    private void validateLeader(Long studyId, Long userId) {
        StudyMember member = studyMemberRepository.findByStudyIdAndUserId(studyId, userId)
                .orElseThrow(() -> new NotFoundException("STUDY_MEMBER_NOT_FOUND", "스터디 멤버가 아닙니다."));
        if (member.getStatus() != MemberStatus.APPROVED) {
            throw new IllegalStateException("승인된 스터디 멤버가 아닙니다.");
        }
        if (member.getRole() != MemberRole.LEADER) {
            throw new IllegalStateException("스터디장이 아닙니다.");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
    }

    private User getStudyLeaderUser(Long studyId) {
        Study study = getStudyOrThrow(studyId);
        return getUserOrThrow(study.getLeaderId());
    }
}
