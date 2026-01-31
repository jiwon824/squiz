package com.ssafy.domain.attendance.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.response.MessageResponse;
import com.ssafy.domain.attendance.dto.request.AttendanceExcuseDecisionRequest;
import com.ssafy.domain.attendance.dto.request.AttendanceExcuseRequest;
import com.ssafy.domain.attendance.dto.request.AttendanceManualUpdateRequest;
import com.ssafy.domain.attendance.dto.response.AttendanceCalendarResponse;
import com.ssafy.domain.attendance.dto.response.AttendanceResponse;
import com.ssafy.domain.attendance.entity.Attendance;
import com.ssafy.domain.attendance.entity.AttendanceCheckType;
import com.ssafy.domain.attendance.entity.AttendanceExcuseStatus;
import com.ssafy.domain.attendance.entity.AttendanceStatus;
import com.ssafy.domain.attendance.repository.AttendanceRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {
    private static final int SELF_ATTENDANCE_DELAY_MINUTES = 15;
    private static final int LATE_THRESHOLD_MINUTES = 10;

    private final AttendanceRepository attendanceRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;

    public MessageResponse startBleAttendance(Long studyId, Long sessionId, Long leaderId) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateLeader(studyId, leaderId);
        initializeAttendanceRows(session);
        return new MessageResponse("BLE 출석이 시작되었습니다.");
    }

    public AttendanceResponse checkAttendanceBle(Long studyId, Long sessionId, Long userId) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateMember(studyId, userId);
        Attendance attendance = getOrCreateAttendance(session, userId);
        updateCheckInfo(attendance, AttendanceCheckType.BLE, session);
        attendance.setCheckedBy(getStudyLeaderUser(studyId));
        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    public AttendanceResponse checkAttendanceSelf(Long studyId, Long sessionId, Long userId) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateMember(studyId, userId);
        validateSelfAttendanceWindow(session, studyId);
        Attendance attendance = getOrCreateAttendance(session, userId);
        updateCheckInfo(attendance, AttendanceCheckType.SELF, session);
        attendance.setCheckedBy(null);
        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    @Transactional
    public AttendanceResponse checkAttendanceAutoOnline(Long studyId, Long sessionId, Long userId) {
        StudySession session = getSessionOrThrow(sessionId, studyId);
        validateMember(studyId, userId);
        validateAutoAttendanceSession(session);
        Attendance attendance = getOrCreateAttendance(session, userId);
        updateCheckInfo(attendance, AttendanceCheckType.AUTO, session);
        attendance.setCheckedBy(null);
        return AttendanceResponse.from(attendanceRepository.save(attendance));
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
        attendance.setStatus(request.status());
        attendance.setCheckType(AttendanceCheckType.SELF);
        attendance.setCheckedBy(getUserOrThrow(leaderId));
        if (request.reason() != null && !request.reason().isBlank()) {
            attendance.setExcuseReason(request.reason());
        }
        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getSessionAttendance(Long studyId, Long sessionId, Long requesterId) {
        validateLeader(studyId, requesterId);
        getSessionOrThrow(sessionId, studyId);
        List<Attendance> items = attendanceRepository.findBySessionId(sessionId);
        List<AttendanceResponse> responses = new ArrayList<>();
        for (Attendance item : items) {
            responses.add(AttendanceResponse.from(item));
        }
        return responses;
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
        return AttendanceResponse.from(attendanceRepository.save(attendance));
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
        return AttendanceResponse.from(attendanceRepository.save(attendance));
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
        // 10분 이후 입장해도 LATE로 처리되도록 예외 던지지 않음
        // updateCheckInfo()에서 시간에 따라 PRESENT/LATE 자동 판정
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
