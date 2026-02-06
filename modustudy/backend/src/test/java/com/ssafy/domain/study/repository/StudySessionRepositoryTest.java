package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.*;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StudySessionRepository 통합 테스트
 */
 @SpringBootTest
 @Transactional
 class StudySessionRepositoryTest {

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

    private User user;
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

        // 3. User 엔티티 생성
        user = userRepository.save(User.builder()
                .userId("testuser")
                .email("test@test.com")
                .nickname("테스트유저")
                .name("테스트")
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

        // 4. Study 엔티티 생성
        study = studyRepository.save(Study.builder()
                .leaderId(user.getId())
                .name("테스트 스터디")
                .topic(topic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();
    }

    @Test
    @DisplayName("세션 생성 및 조회 성공")
    void save_Success() {
        // given
        StudySession session = StudySession.builder()
                .studyId(study.getId())
                .sessionNumber(1)
                .title("1회차: OT")
                .description("오리엔테이션 진행")
                .scheduledAt(LocalDateTime.now().plusDays(7))
                .durationMinutes(60)
                .location("온라인 (Zoom)")
                .isOnline(true)
                .build();

        // when
        StudySession saved = studySessionRepository.save(session);
        studySessionRepository.flush();

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStudyId()).isEqualTo(study.getId());
        assertThat(saved.getSessionNumber()).isEqualTo(1);
        assertThat(saved.getTitle()).isEqualTo("1회차: OT");
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
    }

    @Test
    @DisplayName("스터디별 세션 목록 조회")
    void findByStudyId_Success() {
        // given
        createSession(study.getId(), 3, "3회차");
        createSession(study.getId(), 1, "1회차");
        createSession(study.getId(), 2, "2회차");
        studySessionRepository.flush();

        // when
        List<StudySession> sessions = studySessionRepository.findByStudyId(study.getId());

        // then
        assertThat(sessions).hasSize(3);
    }

    @Test
    @DisplayName("스터디 ID와 회차로 세션 조회")
    void findByStudyIdAndSessionNumber_Success() {
        // given
        createSession(study.getId(), 1, "1회차");
        createSession(study.getId(), 2, "2회차");
        studySessionRepository.flush();

        // when
        Optional<StudySession> result = studySessionRepository.findByStudyIdAndSessionNumber(study.getId(), 2);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("2회차");
    }

    @Test
    @DisplayName("스터디 ID와 회차로 세션 조회 - 없는 경우")
    void findByStudyIdAndSessionNumber_NotFound() {
        // given
        createSession(study.getId(), 1, "1회차");
        studySessionRepository.flush();

        // when
        Optional<StudySession> result = studySessionRepository.findByStudyIdAndSessionNumber(study.getId(), 999);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 상태의 세션 목록 조회")
    void findByStudyIdAndStatusOrderBySessionNumberAsc_Success() {
        // given
        StudySession session1 = createSession(study.getId(), 1, "1회차");
        StudySession session2 = createSession(study.getId(), 2, "2회차");
        StudySession session3 = createSession(study.getId(), 3, "3회차");
        studySessionRepository.flush();

        // session1 완료 처리
        session1.start();
        session1.complete();
        studySessionRepository.flush();

        // when
        List<StudySession> scheduledSessions = studySessionRepository
                .findByStudyIdAndStatus(study.getId(), SessionStatus.SCHEDULED);

        // then
        assertThat(scheduledSessions).hasSize(2);
    }

    @Test
    @DisplayName("스터디별 세션 개수 조회")
    void countByStudyId_Success() {
        // given
        createSession(study.getId(), 1, "1회차");
        createSession(study.getId(), 2, "2회차");
        createSession(study.getId(), 3, "3회차");
        studySessionRepository.flush();

        // when
        long count = studySessionRepository.countByStudyId(study.getId());

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("스터디별 특정 상태 세션 개수 조회")
    void countByStudyIdAndStatus_Success() {
        // given
        StudySession session1 = createSession(study.getId(), 1, "1회차");
        createSession(study.getId(), 2, "2회차");
        createSession(study.getId(), 3, "3회차");
        studySessionRepository.flush();

        // session1 완료 처리
        session1.start();
        session1.complete();
        studySessionRepository.flush();

        // when
        long completedCount = studySessionRepository.countByStudyIdAndStatus(study.getId(), SessionStatus.COMPLETED);
        long scheduledCount = studySessionRepository.countByStudyIdAndStatus(study.getId(), SessionStatus.SCHEDULED);

        // then
        assertThat(completedCount).isEqualTo(1);
        assertThat(scheduledCount).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 기간 내 세션 조회")
    void findByStudyIdAndScheduledAtBetween_Success() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 1주 후 세션
        StudySession session1 = createSessionWithScheduledAt(study.getId(), 1, "1회차", now.plusDays(7));
        // 2주 후 세션
        StudySession session2 = createSessionWithScheduledAt(study.getId(), 2, "2회차", now.plusDays(14));
        // 4주 후 세션
        StudySession session3 = createSessionWithScheduledAt(study.getId(), 3, "3회차", now.plusDays(28));
        studySessionRepository.flush();

        // when - 1주~3주 사이 세션 조회
        List<StudySession> sessions = studySessionRepository.findByStudyIdAndScheduledAtBetween(
                study.getId(),
                now.plusDays(5),
                now.plusDays(20));

        // then
        assertThat(sessions).hasSize(2);
        assertThat(sessions.get(0).getSessionNumber()).isEqualTo(1);
        assertThat(sessions.get(1).getSessionNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("다음 예정된 세션 조회")
    void findNextScheduledSession_Success() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 과거 세션 (완료됨)
        StudySession pastSession = createSessionWithScheduledAt(study.getId(), 1, "1회차", now.minusDays(7));
        pastSession.start();
        pastSession.complete();

        // 미래 세션들
        createSessionWithScheduledAt(study.getId(), 2, "2회차", now.plusDays(7));
        createSessionWithScheduledAt(study.getId(), 3, "3회차", now.plusDays(14));
        studySessionRepository.flush();

        // when
        Optional<StudySession> nextSession = studySessionRepository.findNextScheduledSession(study.getId(), now);

        // then
        assertThat(nextSession).isPresent();
        assertThat(nextSession.get().getSessionNumber()).isEqualTo(2);
        assertThat(nextSession.get().getTitle()).isEqualTo("2회차");
    }

    @Test
    @DisplayName("다음 예정된 세션 조회 - 없는 경우")
    void findNextScheduledSession_NotFound() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 과거 세션만 존재 (완료됨)
        StudySession pastSession = createSessionWithScheduledAt(study.getId(), 1, "1회차", now.minusDays(7));
        pastSession.start();
        pastSession.complete();
        studySessionRepository.flush();

        // when
        Optional<StudySession> nextSession = studySessionRepository.findNextScheduledSession(study.getId(), now);

        // then
        assertThat(nextSession).isEmpty();
    }

    @Test
    @DisplayName("마지막 회차 번호 조회")
    void findMaxSessionNumberByStudyId_Success() {
        // given
        createSession(study.getId(), 1, "1회차");
        createSession(study.getId(), 5, "5회차");
        createSession(study.getId(), 3, "3회차");
        studySessionRepository.flush();

        // when
        Integer maxSessionNumber = studySessionRepository.findMaxSessionNumberByStudyId(study.getId());

        // then
        assertThat(maxSessionNumber).isEqualTo(5);
    }

    @Test
    @DisplayName("마지막 회차 번호 조회 - 세션 없는 경우")
    void findMaxSessionNumberByStudyId_NoSession() {
        // when
        Integer maxSessionNumber = studySessionRepository.findMaxSessionNumberByStudyId(study.getId());

        // then
        assertThat(maxSessionNumber).isEqualTo(0);
    }

    @Test
    @DisplayName("스터디별 모든 세션 삭제")
    void deleteAllByStudyId_Success() {
        // given
        createSession(study.getId(), 1, "1회차");
        createSession(study.getId(), 2, "2회차");
        createSession(study.getId(), 3, "3회차");
        studySessionRepository.flush();

        // when
        studySessionRepository.deleteAllByStudyId(study.getId());
        entityManager.flush();
        entityManager.clear();

        // then
        List<StudySession> sessions = studySessionRepository.findByStudyId(study.getId());
        assertThat(sessions).isEmpty();
    }

    @Test
    @DisplayName("특정 상태의 세션 존재 여부 확인")
    void existsByStudyIdAndStatus_Success() {
        // given
        StudySession session = createSession(study.getId(), 1, "1회차");
        studySessionRepository.flush();

        // when
        boolean existsScheduled = studySessionRepository.existsByStudyIdAndStatus(study.getId(), SessionStatus.SCHEDULED);
        boolean existsCompleted = studySessionRepository.existsByStudyIdAndStatus(study.getId(), SessionStatus.COMPLETED);

        // then
        assertThat(existsScheduled).isTrue();
        assertThat(existsCompleted).isFalse();

        // session 완료 처리
        session.start();
        session.complete();
        studySessionRepository.flush();

        // when
        existsCompleted = studySessionRepository.existsByStudyIdAndStatus(study.getId(), SessionStatus.COMPLETED);

        // then
        assertThat(existsCompleted).isTrue();
    }

    @Test
    @DisplayName("다른 스터디의 세션은 조회되지 않음")
    void findByStudyId_IsolationTest() {
        // given - 다른 스터디 생성
        Topic anotherTopic = topicRepository.save(Topic.builder()
                .name("Python")
                .sortOrder(2)
                .build());
        topicRepository.flush();

        Study anotherStudy = studyRepository.save(Study.builder()
                .leaderId(user.getId())
                .name("다른 스터디")
                .topic(anotherTopic)
                .format(format)
                .studyType(StudyType.PLANNED)
                .build());
        studyRepository.flush();

        // 각 스터디에 세션 생성
        createSession(study.getId(), 1, "스터디1 - 1회차");
        createSession(study.getId(), 2, "스터디1 - 2회차");
        createSession(anotherStudy.getId(), 1, "스터디2 - 1회차");
        studySessionRepository.flush();

        // when
        List<StudySession> study1Sessions = studySessionRepository.findByStudyId(study.getId());
        List<StudySession> study2Sessions = studySessionRepository.findByStudyId(anotherStudy.getId());

        // then
        assertThat(study1Sessions).hasSize(2);
        assertThat(study2Sessions).hasSize(1);
    }

    // ==================== Helper Methods ====================

    private StudySession createSession(Long studyId, int sessionNumber, String title) {
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

    private StudySession createSessionWithScheduledAt(Long studyId, int sessionNumber, String title, LocalDateTime scheduledAt) {
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
}
