package com.ssafy.domain.study.service;

import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.StudyException;
import com.ssafy.domain.study.dto.request.StudySessionCreateRequest;
import com.ssafy.domain.study.dto.request.StudySessionUpdateRequest;
import com.ssafy.domain.study.dto.response.StudySessionResponse;
import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
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

/**
 * StudySessionService 통합 테스트
 */
 @SpringBootTest
 @Transactional
 class StudySessionServiceTest {

    @Autowired
    private StudySessionService studySessionService;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private EntityManager entityManager;

    private User leader;
    private User otherUser;
    private Study study;
    private Topic topic;
    private Format format;

    @BeforeEach
    void setUp() {
        // 1. Topic 생성
        topic = topicRepository.save(Topic.builder()
                .name("Java")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        // 2. Format 생성
        format = formatRepository.save(Format.builder()
                .name("코드 리뷰")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        // 3. 스터디장 생성
        leader = userRepository.save(User.builder()
                .userId("leader")
                .email("leader@test.com")
                .nickname("스터디장")
                .name("리더")
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

        // 4. 다른 사용자 생성
        otherUser = userRepository.save(User.builder()
                .userId("other")
                .email("other@test.com")
                .nickname("다른유저")
                .name("다른")
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

        // 5. 스터디 생성
        study = studyRepository.save(Study.builder()
                .leaderId(leader.getId())
                .name("테스트 스터디")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();
    }

    @Nested
    @DisplayName("세션 생성")
    class CreateSession {

        @Test
        @DisplayName("성공 - 스터디장이 세션 생성")
        void createSession_Success() {
            // given
            StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                    .title("1회차: OT")
                    .description("오리엔테이션")
                    .scheduledAt(LocalDateTime.now().plusDays(7))
                    .durationMinutes(90)
                    .location("Zoom")
                    .isOnline(true)
                    .build();

            // when
            StudySessionResponse response = studySessionService.createSession(
                    study.getId(), leader.getId(), request);

            // then
            assertThat(response.getId()).isNotNull();
            assertThat(response.getStudyId()).isEqualTo(study.getId());
            assertThat(response.getSessionNumber()).isEqualTo(1);
            assertThat(response.getTitle()).isEqualTo("1회차: OT");
            assertThat(response.getDurationMinutes()).isEqualTo(90);
            assertThat(response.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
        }

        @Test
        @DisplayName("성공 - 회차 번호 자동 증가")
        void createSession_AutoIncrementSessionNumber() {
            // given - 기존 세션 2개 생성
            createTestSession(study.getId(), 1, "1회차");
            createTestSession(study.getId(), 2, "2회차");
            studySessionRepository.flush();

            StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                    .title("3회차: 복습")
                    .scheduledAt(LocalDateTime.now().plusDays(21))
                    .build();

            // when
            StudySessionResponse response = studySessionService.createSession(
                    study.getId(), leader.getId(), request);

            // then
            assertThat(response.getSessionNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("성공 - 기본값 적용")
        void createSession_DefaultValues() {
            // given
            StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                    .scheduledAt(LocalDateTime.now().plusDays(7))
                    .build();

            // when
            StudySessionResponse response = studySessionService.createSession(
                    study.getId(), leader.getId(), request);

            // then
            assertThat(response.getDurationMinutes()).isEqualTo(60);  // 기본값
            assertThat(response.getIsOnline()).isTrue();  // 기본값
        }

        @Test
        @DisplayName("실패 - 스터디장이 아닌 사용자")
        void createSession_NotLeader_ThrowsException() {
            // given
            StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                    .title("1회차")
                    .scheduledAt(LocalDateTime.now().plusDays(7))
                    .build();

            // when & then
            assertThatThrownBy(() -> studySessionService.createSession(
                    study.getId(), otherUser.getId(), request))
                    .isInstanceOf(StudyException.NotStudyLeaderException.class)
                    .hasMessageContaining("스터디장만");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 스터디")
        void createSession_StudyNotFound_ThrowsException() {
            // given
            StudySessionCreateRequest request = StudySessionCreateRequest.builder()
                    .title("1회차")
                    .scheduledAt(LocalDateTime.now().plusDays(7))
                    .build();

            // when & then
            assertThatThrownBy(() -> studySessionService.createSession(
                    999L, leader.getId(), request))
                    .isInstanceOf(StudyException.StudyNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("세션 조회")
    class GetSession {

        @Test
        @DisplayName("성공 - 세션 ID로 조회")
        void getSession_Success() {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            studySessionRepository.flush();

            // when
            StudySessionResponse response = studySessionService.getSession(study.getId(), session.getId());

            // then
            assertThat(response.getId()).isEqualTo(session.getId());
            assertThat(response.getTitle()).isEqualTo("1회차");
        }

        @Test
        @DisplayName("성공 - 회차 번호로 조회")
        void getSessionByNumber_Success() {
            // given
            createTestSession(study.getId(), 1, "1회차");
            createTestSession(study.getId(), 2, "2회차");
            studySessionRepository.flush();

            // when
            StudySessionResponse response = studySessionService.getSessionByNumber(study.getId(), 2);

            // then
            assertThat(response.getSessionNumber()).isEqualTo(2);
            assertThat(response.getTitle()).isEqualTo("2회차");
        }

        @Test
        @DisplayName("실패 - 다른 스터디의 세션 조회")
        void getSession_WrongStudy_ThrowsException() {
            // given
            Topic anotherTopic = topicRepository.save(Topic.builder()
                    .name("Python")
                    .sortOrder(2)
                    .build());
            topicRepository.flush();

            Study anotherStudy = studyRepository.save(Study.builder()
                    .leaderId(leader.getId())
                    .name("다른 스터디")
                    .topic(anotherTopic)
                    .format(format)
                    .studyType(StudyType.PLANNED)
                    .build());
            studyRepository.flush();

            StudySession session = createTestSession(anotherStudy.getId(), 1, "다른 스터디 세션");
            studySessionRepository.flush();

            // when & then
            assertThatThrownBy(() -> studySessionService.getSession(study.getId(), session.getId()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("해당 스터디의 세션이 아닙니다");
        }
    }

    @Nested
    @DisplayName("세션 목록 조회")
    class GetSessions {

        @Test
        @DisplayName("성공 - 스터디별 세션 목록 (회차 순)")
        void getSessionsByStudyId_Success() {
            // given
            createTestSession(study.getId(), 3, "3회차");
            createTestSession(study.getId(), 1, "1회차");
            createTestSession(study.getId(), 2, "2회차");
            studySessionRepository.flush();

            // when
            List<StudySessionResponse> sessions = studySessionService.getSessionsByStudyId(study.getId());

            // then
            assertThat(sessions).hasSize(3);
            assertThat(sessions.get(0).getSessionNumber()).isEqualTo(1);
            assertThat(sessions.get(1).getSessionNumber()).isEqualTo(2);
            assertThat(sessions.get(2).getSessionNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("성공 - 상태별 세션 목록")
        void getSessionsByStatus_Success() {
            // given
            StudySession session1 = createTestSession(study.getId(), 1, "1회차");
            createTestSession(study.getId(), 2, "2회차");
            createTestSession(study.getId(), 3, "3회차");
            studySessionRepository.flush();

            // session1 완료 처리
            session1.start();
            session1.complete();
            studySessionRepository.flush();

            // when
            List<StudySessionResponse> scheduledSessions =
                    studySessionService.getSessionsByStatus(study.getId(), SessionStatus.SCHEDULED);

            // then
            assertThat(scheduledSessions).hasSize(2);
        }

        @Test
        @DisplayName("성공 - 다음 예정 세션 조회")
        void getNextSession_Success() {
            // given
            LocalDateTime now = LocalDateTime.now();
            createTestSessionWithScheduledAt(study.getId(), 1, "1회차", now.plusDays(7));
            createTestSessionWithScheduledAt(study.getId(), 2, "2회차", now.plusDays(14));
            studySessionRepository.flush();

            // when
            StudySessionResponse response = studySessionService.getNextSession(study.getId());

            // then
            assertThat(response.getSessionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("실패 - 예정된 세션 없음")
        void getNextSession_NotFound_ThrowsException() {
            // given - 세션 없음

            // when & then
            assertThatThrownBy(() -> studySessionService.getNextSession(study.getId()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("예정된 세션이 없습니다");
        }
    }

    @Nested
    @DisplayName("세션 수정")
    class UpdateSession {

        @Test
        @DisplayName("성공 - 세션 정보 수정")
        void updateSession_Success() {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            studySessionRepository.flush();

            StudySessionUpdateRequest request = StudySessionUpdateRequest.builder()
                    .title("1회차: OT (수정)")
                    .description("수정된 설명")
                    .durationMinutes(120)
                    .build();

            // when
            StudySessionResponse response = studySessionService.updateSession(
                    study.getId(), session.getId(), leader.getId(), request);

            // then
            assertThat(response.getTitle()).isEqualTo("1회차: OT (수정)");
            assertThat(response.getDescription()).isEqualTo("수정된 설명");
            assertThat(response.getDurationMinutes()).isEqualTo(120);
        }

        @Test
        @DisplayName("실패 - 진행 중인 세션 수정 불가")
        void updateSession_InProgress_ThrowsException() {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            session.start();  // 진행 중으로 변경
            studySessionRepository.flush();

            StudySessionUpdateRequest request = StudySessionUpdateRequest.builder()
                    .title("수정 시도")
                    .build();

            // when & then
            assertThatThrownBy(() -> studySessionService.updateSession(
                    study.getId(), session.getId(), leader.getId(), request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("예정된 세션만 수정");
        }

        @Test
        @DisplayName("실패 - 스터디장이 아닌 사용자")
        void updateSession_NotLeader_ThrowsException() {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            studySessionRepository.flush();

            StudySessionUpdateRequest request = StudySessionUpdateRequest.builder()
                    .title("수정 시도")
                    .build();

            // when & then
            assertThatThrownBy(() -> studySessionService.updateSession(
                    study.getId(), session.getId(), otherUser.getId(), request))
                    .isInstanceOf(StudyException.NotStudyLeaderException.class);
        }
    }

    @Nested
    @DisplayName("세션 삭제")
    class DeleteSession {

        @Test
        @DisplayName("성공 - 예정된 세션 삭제")
        void deleteSession_Success() {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            studySessionRepository.flush();
            Long sessionId = session.getId();

            // when
            studySessionService.deleteSession(study.getId(), sessionId, leader.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(studySessionRepository.findById(sessionId)).isEmpty();
        }

        @Test
        @DisplayName("실패 - 완료된 세션 삭제 불가")
        void deleteSession_Completed_ThrowsException() {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            session.start();
            session.complete();
            studySessionRepository.flush();

            // when & then
            assertThatThrownBy(() -> studySessionService.deleteSession(
                    study.getId(), session.getId(), leader.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("완료된 세션은 삭제할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("세션 상태 변경")
    class ChangeSessionStatus {

        @Test
        @DisplayName("성공 - 세션 시작")
        void startSession_Success() {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            studySessionRepository.flush();

            // when
            StudySessionResponse response = studySessionService.startSession(
                    study.getId(), session.getId(), leader.getId());

            // then
            assertThat(response.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("성공 - 세션 완료")
        void completeSession_Success() {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            session.start();
            studySessionRepository.flush();

            // when
            StudySessionResponse response = studySessionService.completeSession(
                    study.getId(), session.getId(), leader.getId());

            // then
            assertThat(response.getStatus()).isEqualTo(SessionStatus.COMPLETED);
            assertThat(response.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("성공 - 세션 취소")
        void cancelSession_Success() {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            studySessionRepository.flush();

            // when
            StudySessionResponse response = studySessionService.cancelSession(
                    study.getId(), session.getId(), leader.getId());

            // then
            assertThat(response.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        }

        @Test
        @DisplayName("실패 - 완료된 세션 취소 불가")
        void cancelSession_Completed_ThrowsException() {
            // given
            StudySession session = createTestSession(study.getId(), 1, "1회차");
            session.start();
            session.complete();
            studySessionRepository.flush();

            // when & then
            assertThatThrownBy(() -> studySessionService.cancelSession(
                    study.getId(), session.getId(), leader.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("완료된 세션은 취소할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("세션 통계")
    class SessionStatistics {

        @Test
        @DisplayName("성공 - 세션 통계 조회")
        void getSessionStatistics_Success() {
            // given
            StudySession session1 = createTestSession(study.getId(), 1, "1회차");
            StudySession session2 = createTestSession(study.getId(), 2, "2회차");
            createTestSession(study.getId(), 3, "3회차");
            StudySession session4 = createTestSession(study.getId(), 4, "4회차");
            studySessionRepository.flush();

            // session1, session2 완료
            session1.start();
            session1.complete();
            session2.start();
            session2.complete();
            // session4 취소
            session4.cancel();
            studySessionRepository.flush();

            // when
            StudySessionService.SessionStatistics stats =
                    studySessionService.getSessionStatistics(study.getId());

            // then
            assertThat(stats.totalCount()).isEqualTo(4);
            assertThat(stats.completedCount()).isEqualTo(2);
            assertThat(stats.scheduledCount()).isEqualTo(1);
            assertThat(stats.cancelledCount()).isEqualTo(1);
            assertThat(stats.getCompletionRate()).isEqualTo(50.0);
        }
    }

    // ==================== Helper Methods ====================

    private StudySession createTestSession(Long studyId, int sessionNumber, String title) {
        return studySessionRepository.save(StudySession.builder()
                .studyId(studyId)
                .sessionNumber(sessionNumber)
                .title(title)
                .description(title + " 설명")
                .scheduledAt(LocalDateTime.now().plusDays(sessionNumber * 7L))
                .durationMinutes(60)
                .isOnline(true)
                .build());
    }

    private StudySession createTestSessionWithScheduledAt(Long studyId, int sessionNumber,
                                                          String title, LocalDateTime scheduledAt) {
        return studySessionRepository.save(StudySession.builder()
                .studyId(studyId)
                .sessionNumber(sessionNumber)
                .title(title)
                .description(title + " 설명")
                .scheduledAt(scheduledAt)
                .durationMinutes(60)
                .isOnline(true)
                .build());
    }

    @Nested
    @DisplayName("내 스터디 세션 조회")
    class GetMyStudySessions {

        @Autowired
        private com.ssafy.domain.study.repository.StudyMemberRepository studyMemberRepository;

        @Test
        @DisplayName("성공 - 참여한 스터디의 세션 조회")
        void getMyStudySessions_Success() {
            // given
            // 사용자를 스터디 멤버로 추가
            studyMemberRepository.save(StudyMember.builder()
                    .studyId(study.getId())
                    .userId(otherUser.getId())
                    .status(MemberStatus.APPROVED)
                    .role(MemberRole.MEMBER)
                    .build());
            studyMemberRepository.flush();

            // 세션 생성
            LocalDateTime now = LocalDateTime.now();
            createTestSessionWithScheduledAt(study.getId(), 1, "1회차", now.plusDays(1));
            createTestSessionWithScheduledAt(study.getId(), 2, "2회차", now.plusDays(3));
            studySessionRepository.flush();

            // when
            List<StudySessionResponse> sessions = studySessionService.getMyStudySessions(
                    otherUser.getId(),
                    now,
                    now.plusDays(7)
            );

            // then
            assertThat(sessions).hasSize(2);
        }

        @Test
        @DisplayName("성공 - 참여한 스터디가 없으면 빈 목록 반환")
        void getMyStudySessions_NoStudies_ReturnsEmpty() {
            // given
            LocalDateTime now = LocalDateTime.now();

            // when
            List<StudySessionResponse> sessions = studySessionService.getMyStudySessions(
                    otherUser.getId(),
                    now,
                    now.plusDays(7)
            );

            // then
            assertThat(sessions).isEmpty();
        }

        @Test
        @DisplayName("성공 - 기간 내 세션만 조회")
        void getMyStudySessions_FilterByDateRange() {
            // given
            studyMemberRepository.save(StudyMember.builder()
                    .studyId(study.getId())
                    .userId(otherUser.getId())
                    .status(MemberStatus.APPROVED)
                    .role(MemberRole.MEMBER)
                    .build());
            studyMemberRepository.flush();

            LocalDateTime now = LocalDateTime.now();
            createTestSessionWithScheduledAt(study.getId(), 1, "1회차", now.plusDays(1));
            createTestSessionWithScheduledAt(study.getId(), 2, "2회차", now.plusDays(10)); // 범위 외
            studySessionRepository.flush();

            // when
            List<StudySessionResponse> sessions = studySessionService.getMyStudySessions(
                    otherUser.getId(),
                    now,
                    now.plusDays(7)
            );

            // then
            assertThat(sessions).hasSize(1);
            assertThat(sessions.get(0).getTitle()).isEqualTo("1회차");
        }
    }

    @Nested
    @DisplayName("세션 번호 재정렬")
    class SessionReordering {

        @Test
        @DisplayName("성공 - 새 세션 생성 시 날짜 순으로 sessionNumber 재정렬")
        void createSession_ReordersSessionsByDate() {
            // given - 4월 10일, 4월 20일 세션 존재
            LocalDateTime april10 = LocalDateTime.of(2025, 4, 10, 10, 0);
            LocalDateTime april20 = LocalDateTime.of(2025, 4, 20, 10, 0);
            LocalDateTime april5 = LocalDateTime.of(2025, 4, 5, 10, 0);

            StudySessionCreateRequest request1 = StudySessionCreateRequest.builder()
                    .title("4월 10일 세션")
                    .scheduledAt(april10)
                    .build();
            StudySessionCreateRequest request2 = StudySessionCreateRequest.builder()
                    .title("4월 20일 세션")
                    .scheduledAt(april20)
                    .build();

            studySessionService.createSession(study.getId(), leader.getId(), request1);
            studySessionService.createSession(study.getId(), leader.getId(), request2);
            studySessionRepository.flush();

            // when - 4월 5일 (가장 이른 날짜) 세션 추가
            StudySessionCreateRequest request3 = StudySessionCreateRequest.builder()
                    .title("4월 5일 세션")
                    .scheduledAt(april5)
                    .build();
            studySessionService.createSession(study.getId(), leader.getId(), request3);
            entityManager.flush();
            entityManager.clear();

            // then - 날짜 순으로 sessionNumber 재정렬됨
            List<StudySessionResponse> sessions = studySessionService.getSessionsByStudyId(study.getId());

            assertThat(sessions).hasSize(3);
            // 4월 5일 → 1회차
            assertThat(sessions.get(0).getSessionNumber()).isEqualTo(1);
            assertThat(sessions.get(0).getTitle()).isEqualTo("4월 5일 세션");
            // 4월 10일 → 2회차
            assertThat(sessions.get(1).getSessionNumber()).isEqualTo(2);
            assertThat(sessions.get(1).getTitle()).isEqualTo("4월 10일 세션");
            // 4월 20일 → 3회차
            assertThat(sessions.get(2).getSessionNumber()).isEqualTo(3);
            assertThat(sessions.get(2).getTitle()).isEqualTo("4월 20일 세션");
        }

        @Test
        @DisplayName("성공 - 세션 날짜 수정 시 sessionNumber 재정렬")
        void updateSession_ReordersWhenDateChanges() {
            // given - 1회차(4/5), 2회차(4/10), 3회차(4/20)
            LocalDateTime april5 = LocalDateTime.of(2025, 4, 5, 10, 0);
            LocalDateTime april10 = LocalDateTime.of(2025, 4, 10, 10, 0);
            LocalDateTime april20 = LocalDateTime.of(2025, 4, 20, 10, 0);

            StudySession session1 = createTestSessionWithScheduledAt(study.getId(), 1, "1회차", april5);
            StudySession session2 = createTestSessionWithScheduledAt(study.getId(), 2, "2회차", april10);
            StudySession session3 = createTestSessionWithScheduledAt(study.getId(), 3, "3회차", april20);
            studySessionRepository.flush();

            // when - 1회차 날짜를 4월 15일로 변경 (2회차와 3회차 사이)
            LocalDateTime april15 = LocalDateTime.of(2025, 4, 15, 10, 0);
            StudySessionUpdateRequest updateRequest = StudySessionUpdateRequest.builder()
                    .scheduledAt(april15)
                    .build();
            studySessionService.updateSession(study.getId(), session1.getId(), leader.getId(), updateRequest);
            entityManager.flush();
            entityManager.clear();

            // then - 날짜 순으로 sessionNumber 재정렬됨
            List<StudySessionResponse> sessions = studySessionService.getSessionsByStudyId(study.getId());

            assertThat(sessions).hasSize(3);
            // 4월 10일 → 1회차
            assertThat(sessions.get(0).getSessionNumber()).isEqualTo(1);
            assertThat(sessions.get(0).getTitle()).isEqualTo("2회차");
            // 4월 15일 → 2회차 (원래 1회차였던 것)
            assertThat(sessions.get(1).getSessionNumber()).isEqualTo(2);
            assertThat(sessions.get(1).getTitle()).isEqualTo("1회차");
            // 4월 20일 → 3회차
            assertThat(sessions.get(2).getSessionNumber()).isEqualTo(3);
            assertThat(sessions.get(2).getTitle()).isEqualTo("3회차");
        }

        @Test
        @DisplayName("성공 - 세션 삭제 시 sessionNumber 재정렬")
        void deleteSession_ReordersAfterDelete() {
            // given - 1회차(4/5), 2회차(4/10), 3회차(4/20)
            LocalDateTime april5 = LocalDateTime.of(2025, 4, 5, 10, 0);
            LocalDateTime april10 = LocalDateTime.of(2025, 4, 10, 10, 0);
            LocalDateTime april20 = LocalDateTime.of(2025, 4, 20, 10, 0);

            StudySession session1 = createTestSessionWithScheduledAt(study.getId(), 1, "1회차", april5);
            StudySession session2 = createTestSessionWithScheduledAt(study.getId(), 2, "2회차", april10);
            StudySession session3 = createTestSessionWithScheduledAt(study.getId(), 3, "3회차", april20);
            studySessionRepository.flush();

            // when - 2회차 삭제
            studySessionService.deleteSession(study.getId(), session2.getId(), leader.getId());
            entityManager.flush();
            entityManager.clear();

            // then - sessionNumber가 연속으로 재정렬됨
            List<StudySessionResponse> sessions = studySessionService.getSessionsByStudyId(study.getId());

            assertThat(sessions).hasSize(2);
            // 4월 5일 → 1회차
            assertThat(sessions.get(0).getSessionNumber()).isEqualTo(1);
            assertThat(sessions.get(0).getTitle()).isEqualTo("1회차");
            // 4월 20일 → 2회차 (원래 3회차였던 것)
            assertThat(sessions.get(1).getSessionNumber()).isEqualTo(2);
            assertThat(sessions.get(1).getTitle()).isEqualTo("3회차");
        }

        @Test
        @DisplayName("성공 - 일괄 생성 시 날짜 순으로 sessionNumber 정렬")
        void createSessionsBulk_SortsSessionsByDate() {
            // given - 순서 섞인 날짜로 일괄 생성 요청
            LocalDateTime april20 = LocalDateTime.of(2025, 4, 20, 10, 0);
            LocalDateTime april5 = LocalDateTime.of(2025, 4, 5, 10, 0);
            LocalDateTime april15 = LocalDateTime.of(2025, 4, 15, 10, 0);

            List<StudySessionCreateRequest> requests = List.of(
                    StudySessionCreateRequest.builder().title("4월 20일").scheduledAt(april20).build(),
                    StudySessionCreateRequest.builder().title("4월 5일").scheduledAt(april5).build(),
                    StudySessionCreateRequest.builder().title("4월 15일").scheduledAt(april15).build()
            );

            // when
            List<StudySessionResponse> responses = studySessionService.createSessionsBulk(
                    study.getId(), leader.getId(), requests);
            entityManager.flush();
            entityManager.clear();

            // then - 날짜 순으로 정렬
            List<StudySessionResponse> sessions = studySessionService.getSessionsByStudyId(study.getId());

            assertThat(sessions).hasSize(3);
            assertThat(sessions.get(0).getSessionNumber()).isEqualTo(1);
            assertThat(sessions.get(0).getTitle()).isEqualTo("4월 5일");
            assertThat(sessions.get(1).getSessionNumber()).isEqualTo(2);
            assertThat(sessions.get(1).getTitle()).isEqualTo("4월 15일");
            assertThat(sessions.get(2).getSessionNumber()).isEqualTo(3);
            assertThat(sessions.get(2).getTitle()).isEqualTo("4월 20일");
        }
    }
}
