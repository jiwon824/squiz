package com.ssafy.domain.attendance.service;

import com.ssafy.domain.attendance.dto.response.AttendanceCalendarResponse;
import com.ssafy.domain.attendance.dto.response.AttendanceResponse;
import com.ssafy.domain.attendance.entity.Attendance;
import com.ssafy.domain.attendance.entity.AttendanceCheckType;
import com.ssafy.domain.attendance.entity.AttendanceStatus;
import com.ssafy.domain.attendance.repository.AttendanceRepository;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AttendanceServiceTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    private User leader;
    private User member;
    private Study study;
    private StudySession session;

    @BeforeEach
    void setUp() {
        Topic topic = topicRepository.save(Topic.builder()
                .name("Java")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        Format format = formatRepository.save(Format.builder()
                .name("Code Review")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        leader = userRepository.save(User.builder()
                .userId("leader")
                .email("leader@test.com")
                .nickname("leader")
                .name("Leader")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        member = userRepository.save(User.builder()
                .userId("member")
                .email("member@test.com")
                .nickname("member")
                .name("Member")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        study = studyRepository.save(Study.builder()
                .leaderId(leader.getId())
                .name("Attendance Study")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .status(Status.IN_PROGRESS)
                .isPublic(true)
                .build());
        studyRepository.flush();

        studyMemberRepository.save(StudyMember.builder()
                .studyId(study.getId())
                .userId(leader.getId())
                .role(MemberRole.LEADER)
                .status(MemberStatus.APPROVED)
                .isProbation(false)
                .build());
        studyMemberRepository.save(StudyMember.builder()
                .studyId(study.getId())
                .userId(member.getId())
                .role(MemberRole.MEMBER)
                .status(MemberStatus.APPROVED)
                .isProbation(false)
                .build());
        studyMemberRepository.flush();

        session = studySessionRepository.save(StudySession.builder()
                .studyId(study.getId())
                .sessionNumber(1)
                .title("Session 1")
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .durationMinutes(60)
                .isOnline(true)
                .build());
        studySessionRepository.flush();
    }

    @Nested
    @DisplayName("BLE 출석")
    class BleAttendanceTests {

        @Test
        @DisplayName("스터디장이 BLE 출석을 시작하면 출석 row가 생성된다")
        void startBleAttendance_createsRows() {
            attendanceService.startBleAttendance(study.getId(), session.getId(), leader.getId());

            List<Attendance> items = attendanceRepository.findBySessionId(session.getId());
            assertThat(items).hasSize(2);
        }

        @Test
        @DisplayName("BLE 출석 체크 시 출석이 PRESENT로 기록된다")
        void checkBleAttendance_present() {
            AttendanceResponse response = attendanceService.checkAttendanceBle(
                    study.getId(),
                    session.getId(),
                    member.getId()
            );

            assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT);
            assertThat(response.checkType()).isEqualTo(AttendanceCheckType.BLE);
            assertThat(response.checkedBy()).isEqualTo(leader.getId());
            assertThat(response.checkedAt()).isNotNull();
        }

        @Test
        @DisplayName("BLE 출석 체크가 10분 이후면 LATE로 기록된다")
        void checkBleAttendance_late() {
            StudySession lateSession = studySessionRepository.save(StudySession.builder()
                    .studyId(study.getId())
                    .sessionNumber(2)
                    .title("Session 2")
                    .scheduledAt(LocalDateTime.now().minusMinutes(20))
                    .durationMinutes(60)
                    .isOnline(true)
                    .build());
            studySessionRepository.flush();

            AttendanceResponse response = attendanceService.checkAttendanceBle(
                    study.getId(),
                    lateSession.getId(),
                    member.getId()
            );

            assertThat(response.status()).isEqualTo(AttendanceStatus.LATE);
        }
    }

    @Nested
    @DisplayName("셀프 출석")
    class SelfAttendanceTests {

        @Test
        @DisplayName("세션 시작 15분 이전이면 셀프 출석이 거절된다")
        void selfAttendance_rejectedBeforeWindow() {
            StudySession earlySession = studySessionRepository.save(StudySession.builder()
                    .studyId(study.getId())
                    .sessionNumber(3)
                    .title("Session 3")
                    .scheduledAt(LocalDateTime.now().minusMinutes(5))
                    .durationMinutes(60)
                    .isOnline(true)
                    .build());
            studySessionRepository.flush();

            assertThatThrownBy(() -> attendanceService.checkAttendanceSelf(
                    study.getId(),
                    earlySession.getId(),
                    member.getId()
            )).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("온라인 자동 출석")
    class AutoAttendanceTests {

        @Test
        @DisplayName("온라인 세션 시작 10분 이내면 자동 출석이 기록된다")
        void autoAttendance_success() {
            AttendanceResponse response = attendanceService.checkAttendanceAutoOnline(
                    study.getId(),
                    session.getId(),
                    member.getId()
            );

            assertThat(response.checkType()).isEqualTo(AttendanceCheckType.AUTO);
            assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT);
        }

        @Test
        @DisplayName("온라인 세션 시작 10분 이후면 LATE로 기록된다")
        void autoAttendance_lateAfterWindow() {
            StudySession lateSession = studySessionRepository.save(StudySession.builder()
                    .studyId(study.getId())
                    .sessionNumber(4)
                    .title("Session 4")
                    .scheduledAt(LocalDateTime.now().minusMinutes(20))
                    .durationMinutes(60)
                    .isOnline(true)
                    .build());
            studySessionRepository.flush();

            AttendanceResponse response = attendanceService.checkAttendanceAutoOnline(
                    study.getId(),
                    lateSession.getId(),
                    member.getId()
            );

            assertThat(response.checkType()).isEqualTo(AttendanceCheckType.AUTO);
            assertThat(response.status()).isEqualTo(AttendanceStatus.LATE);
        }
    }

    @Test
    @DisplayName("스터디장은 출석 상태를 수동 변경할 수 있다")
    void manualUpdateByLeader() {
        AttendanceResponse response = attendanceService.updateAttendanceStatus(
                study.getId(),
                session.getId(),
                leader.getId(),
                member.getId(),
                new com.ssafy.domain.attendance.dto.request.AttendanceManualUpdateRequest(
                        AttendanceStatus.PRESENT,
                        "BLE issue"
                )
        );

        assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(response.checkedBy()).isEqualTo(leader.getId());
    }

    @Test
    @DisplayName("월별 캘린더 조회는 해당 월 출석 기록을 반환한다")
    void monthlyCalendar_returnsItems() {
        attendanceRepository.save(Attendance.builder()
                .session(session)
                .user(member)
                .checkType(AttendanceCheckType.BLE)
                .status(AttendanceStatus.PRESENT)
                .checkedAt(LocalDateTime.now())
                .build());

        AttendanceCalendarResponse response = attendanceService.getMonthlyCalendar(
                study.getId(),
                member.getId(),
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue()
        );

        assertThat(response.items()).isNotEmpty();
    }
}
