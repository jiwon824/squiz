package com.ssafy.domain.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.exception.BusinessException;
import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.SectionLockedException;
import com.ssafy.domain.gamification.entity.Badge;
import com.ssafy.domain.gamification.repository.BadgeRepository;
import com.ssafy.domain.quiz.dto.request.SaveAnswerRequest;
import com.ssafy.domain.quiz.dto.response.AttemptResultResponse;
import com.ssafy.domain.quiz.dto.response.SectionAttemptResponse;
import com.ssafy.domain.quiz.entity.*;
import com.ssafy.domain.quiz.entity.enums.AttemptStatus;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import com.ssafy.domain.quiz.repository.*;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

/**
 * QuizSectionAttemptService 테스트 클래스
 *
 * 이 클래스는 퀴즈 섹션 시도 서비스의 모든 주요 기능을 테스트합니다.
 * - 시도 시작/재개
 * - 답안 임시 저장
 * - 시도 제출 및 채점
 * - 시도 포기
 *
 * @ExtendWith(MockitoExtension.class)를 사용하여 Mockito 지원을 활성화합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuizSectionAttemptService 테스트")
class QuizSectionAttemptServiceTest {

        // ========== Mock 객체 ==========
        // 실제 의존성을 대체할 Mock 객체들

        @Mock
        private UserRepository userRepository;

        @Mock
        private QuizCourseSectionRepository sectionRepository;

        @Mock
        private QuizCourseRepository courseRepository;

        @Mock
        private UserSectionAttemptRepository attemptRepository;

        @Mock
        private UserSectionAttemptQuestionRepository attemptQuestionRepository;

        @Mock
        private UserCourseProgressRepository progressRepository;

        @Mock
        private BadgeRepository badgeRepository;

        // 기존 코드
        // @Mock
        // private ObjectMapper objectMapper;
        // [원인]
        // QuizSectionAttemptServiceTest에서 ObjectMapper가 @Mock이라
        // parseOptions()의 objectMapper.readValue()가 null 반환
        // → optionMaps.stream() NPE
        // [해결]
        // @Spy로 실제 ObjectMapper 동작 보장 → 테스트 데이터 JSON이 정상 파싱됨.
        @Spy
        private ObjectMapper objectMapper;

        // ========== 테스트 대상 ==========
        // Mock이 주입된 실제 서비스 인스턴스

        @InjectMocks
        private QuizSectionAttemptService quizSectionAttemptService;

        // ========== 테스트 데이터 ==========
        // 여러 테스트에서 공통으로 사용할 테스트 데이터

        private User testUser;
        private QuizCourse testCourse;
        private QuizCourseSection testSection;
        private List<QuizCourseQuestion> testQuestions;
        private UserSectionAttempt testAttempt;

        /**
         * 각 테스트 실행 전에 공통 테스트 데이터를 초기화합니다.
         *
         * @BeforeEach: 각 테스트 메서드 실행 전에 자동으로 실행됩니다.
         */
        @BeforeEach
        void setUp() {
                // questionsPerAttempt 설정값 주입 (기본값: 30)
                ReflectionTestUtils.setField(quizSectionAttemptService, "questionsPerAttempt", 30);

                // 테스트용 사용자 생성
                testUser = User.builder()
                                .userId("testUser")
                                .build();
                // BaseEntity에 Builder가 없으므로 ReflectionTestUtils.setField 이용
                ReflectionTestUtils.setField(testUser, "id", 1L);

                // 테스트용 코스 생성 (활성화 상태, 총 5개 섹션)
                testCourse = QuizCourse.builder()
                                .name("Test Course")
                                .isActive(true)
                                .totalSections(5)
                                .badgeCode("BADGE_001")
                                .build();

                ReflectionTestUtils.setField(testCourse, "id", 1L);

                // 테스트용 섹션 생성 (섹션 번호 1, 통과 점수 70점)
                testSection = QuizCourseSection.builder()
                                .quizCourseId(1L)
                                .sectionNumber(1)
                                .name("Section 1")
                                .passScore(70)
                                .course(testCourse)
                                .build();

                // 테스트용 문제 10개 생성
                testQuestions = new ArrayList<>();
                for (int i = 1; i <= 10; i++) {
                        QuizCourseSection mockSection = QuizCourseSection.builder()
                                        .quizCourseId(1L) // 임의 PK 값
                                        .sectionNumber(1)
                                        .name("Test Section")
                                        .build(); // course null OK (테스트에서 question.section만 사용)

                        QuizCourseQuestion question = QuizCourseQuestion.builder()
                                        .questionNumber(i) // 실제 필드 사용
                                        .questionText("Question " + i)
                                        .questionType(QuestionType.MULTIPLE_CHOICE)
                                        .options("[{\"id\":\"1\",\"text\":\"Option 1\"}]")
                                        .correctAnswer("1")
                                        .explanation("Explanation " + i)
                                        .section(mockSection) // mock 또는 fixture
                                        .build();

                        // 아래 id 설정 줄이 없어서 에러 발생함
                        // Duplicate key null" from Collectors at
                        // QuizSectionAttemptService.saveAnswers:142
                        ReflectionTestUtils.setField(question, "id", (long) i);
                        testQuestions.add(question);
                        // repository.save(question) 후 assert question.getId() != null
                }

                // 섹션에 문제 할당
                ReflectionTestUtils.setField(testSection, "questions", testQuestions);

                // 테스트용 시도 생성
                testAttempt = UserSectionAttempt.builder()
                                // .id(1L)
                                .user(testUser)
                                .section(testSection)
                                .totalQuestions(10)
                                .build();

                ReflectionTestUtils.setField(testAttempt, "id", 1L);

        }

        // ==========================================================
        // startOrResumeAttempt() 테스트
        // ==========================================================

        /**
         * startOrResumeAttempt() 메서드에 대한 중첩 테스트 클래스
         *
         * @Nested를 사용하여 관련 테스트를 그룹화하고 계층 구조를 만듭니다.
         */
        @Nested
        @DisplayName("startOrResumeAttempt 메서드")
        class StartOrResumeAttemptTest {

                /**
                 * 성공 케이스: 첫 섹션(섹션 1)을 시작할 때
                 *
                 * 검증 사항:
                 * - 섹션 1은 항상 해금되어 있어야 함
                 * - 새로운 시도가 생성되어야 함
                 * - 문제가 섞여서 할당되어야 함
                 */
                @Test
                @DisplayName("성공: 첫 섹션(섹션 1) 시작 - 항상 해금됨")
                void startFirstSection_Success() {
                        // Given
                        Long courseId = 1L;
                        Integer sectionNumber = 1;
                        Long userId = 1L;

                        given(sectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(testSection));

                        // [수정] findInProgressAttemptWithQuestions ->
                        // findAllInProgressAttemptsWithQuestions
                        // [수정] Optional.empty() -> Collections.emptyList()
                        given(attemptRepository.findAllInProgressAttemptsWithQuestions(userId, courseId, sectionNumber))
                                        .willReturn(Collections.emptyList());

                        given(userRepository.getReferenceById(userId))
                                        .willReturn(testUser);

                        given(attemptRepository.save(any(UserSectionAttempt.class)))
                                        .willAnswer(invocation -> {
                                                UserSectionAttempt savedAttempt = invocation.getArgument(0);
                                                ReflectionTestUtils.setField(savedAttempt, "id", 1L);
                                                return savedAttempt;
                                        });

                        // When
                        SectionAttemptResponse response = quizSectionAttemptService
                                        .startOrResumeAttempt(courseId, sectionNumber, userId);

                        // Then
                        assertThat(response).isNotNull();
                        then(attemptRepository).should().save(any(UserSectionAttempt.class));
                }

                /**
                 * 성공 케이스: 이미 진행 중인 시도가 있을 때 재개
                 *
                 * 검증 사항:
                 * - 기존 시도를 그대로 반환해야 함
                 * - 새로운 시도를 생성하지 않아야 함
                 * - 기존에 저장된 답안이 유지되어야 함
                 */
                @Test
                @DisplayName("성공: 진행 중인 시도 재개 - 기존 답안 유지")
                void resumeExistingAttempt_Success() {
                        // Given
                        Long courseId = 1L;
                        Integer sectionNumber = 1;
                        Long userId = 1L;

                        List<UserSectionAttemptQuestion> attemptQuestions = new ArrayList<>();
                        for (int i = 0; i < 3; i++) {
                                UserSectionAttemptQuestion aq = UserSectionAttemptQuestion.builder()
                                                .question(testQuestions.get(i))
                                                .orderIndex(i + 1)
                                                .build();
                                if (i == 0)
                                        aq.saveAnswer("1");
                                attemptQuestions.add(aq);
                        }
                        ReflectionTestUtils.setField(testAttempt, "attemptQuestions", attemptQuestions);

                        given(sectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(testSection));

                        // [수정] findAllInProgressAttemptsWithQuestions 사용 및 List 반환
                        given(attemptRepository.findAllInProgressAttemptsWithQuestions(userId, courseId, sectionNumber))
                                        .willReturn(List.of(testAttempt));

                        // When
                        SectionAttemptResponse response = quizSectionAttemptService
                                        .startOrResumeAttempt(courseId, sectionNumber, userId);

                        // Then
                        assertThat(response.attemptId()).isEqualTo(testAttempt.getId());
                        then(attemptRepository).should(never()).save(any(UserSectionAttempt.class));
                }

                /**
                 * 실패 케이스: 존재하지 않는 섹션
                 *
                 * 예상 동작:
                 * - NotFoundException이 발생해야 함
                 */
                @Test
                @DisplayName("실패: 존재하지 않는 섹션 - NotFoundException 발생")
                void sectionNotFound_ThrowsException() {
                        // Given: 존재하지 않는 섹션
                        Long courseId = 999L;
                        Integer sectionNumber = 999;
                        Long userId = 1L;

                        // Mock 동작 정의: 섹션을 찾을 수 없음
                        given(sectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.empty());

                        // When & Then: 예외 발생 확인
                        assertThatThrownBy(() -> quizSectionAttemptService.startOrResumeAttempt(courseId, sectionNumber,
                                        userId))
                                        .isInstanceOf(NotFoundException.class);
                }

                /**
                 * 실패 케이스: 비활성화된 코스
                 *
                 * 예상 동작:
                 * - NotFoundException이 발생해야 함
                 * - 비활성화된 코스는 접근할 수 없음
                 */
                @Test
                @DisplayName("실패: 비활성화된 코스 - NotFoundException 발생")
                void inactiveCourse_ThrowsException() {
                        // Given: 비활성화된 코스
                        Long courseId = 1L;
                        Integer sectionNumber = 1;
                        Long userId = 1L;

                        // 코스를 비활성화 상태로 변경
                        QuizCourse inactiveCourse = QuizCourse.builder()
                                        // .id(1L)
                                        .name("Inactive Course")
                                        .isActive(false) // 비활성화
                                        .build();
                        ReflectionTestUtils.setField(inactiveCourse, "id", 1L);

                        QuizCourseSection sectionWithInactiveCourse = QuizCourseSection.builder()
                                        .quizCourseId(1L)
                                        .sectionNumber(1)
                                        .course(inactiveCourse)
                                        .build();

                        // Mock 동작 정의
                        given(sectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(sectionWithInactiveCourse));

                        // When & Then: 예외 발생 확인
                        assertThatThrownBy(() -> quizSectionAttemptService.startOrResumeAttempt(courseId, sectionNumber,
                                        userId))
                                        .isInstanceOf(NotFoundException.class);
                }

                /**
                 * 실패 케이스: 잠긴 섹션 (이전 섹션 미완료)
                 *
                 * 예상 동작:
                 * - SectionLockedException이 발생해야 함
                 * - 이전 섹션을 통과하지 못하면 다음 섹션에 접근할 수 없음
                 */
                @Test
                @DisplayName("실패: 잠긴 섹션 (이전 섹션 미완료) - SectionLockedException 발생")
                void lockedSection_ThrowsException() {
                        // Given: 섹션 3에 접근 시도 (섹션 2까지만 해금됨)
                        Long courseId = 1L;
                        Integer sectionNumber = 3; // 섹션 3
                        Long userId = 1L;

                        QuizCourseSection section3 = QuizCourseSection.builder()
                                        .quizCourseId(1L)
                                        .sectionNumber(3)
                                        .course(testCourse)
                                        .build();

                        // Mock 동작 정의
                        given(sectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(section3));

                        // 현재 섹션 2까지만 해금된 진행 상황
                        UserCourseProgress progress = UserCourseProgress.builder()
                                        .userId(userId)
                                        .courseId(courseId)
                                        .currentSection(2) // 섹션 2까지만 해금
                                        .build();

                        given(progressRepository.findByUserIdAndCourseId(userId, courseId))
                                        .willReturn(Optional.of(progress));

                        // When & Then: 예외 발생 확인
                        assertThatThrownBy(() -> quizSectionAttemptService.startOrResumeAttempt(courseId, sectionNumber,
                                        userId))
                                        .isInstanceOf(SectionLockedException.class);
                }

                /**
                 * 성공 케이스: 문제 수가 30개를 초과할 때
                 *
                 * 검증 사항:
                 * - 30개만 선택되어야 함 (questionsPerAttempt 설정값)
                 * - 문제가 랜덤하게 섞여야 함
                 */
                @Test
                @DisplayName("성공: 문제가 30개 초과 시 30개만 선택")
                void moreThan30Questions_Select30Only() {
                        // Given
                        List<QuizCourseQuestion> manyQuestions = new ArrayList<>();
                        for (int i = 1; i <= 50; i++) {
                                QuizCourseQuestion question = QuizCourseQuestion.builder().build();
                                ReflectionTestUtils.setField(question, "id", (long) i);
                                manyQuestions.add(question);
                        }

                        QuizCourseSection largeSection = QuizCourseSection.builder()
                                        .course(testCourse).build();
                        ReflectionTestUtils.setField(largeSection, "questions", manyQuestions);

                        given(sectionRepository.findByIdWithCourseAndQuestions(1L, 1))
                                        .willReturn(Optional.of(largeSection));

                        // [수정] 호출되지 않는 이전 메서드 stubbing 제거 및 새 메서드 적용
                        given(attemptRepository.findAllInProgressAttemptsWithQuestions(1L, 1L, 1))
                                        .willReturn(Collections.emptyList());

                        given(userRepository.getReferenceById(1L))
                                        .willReturn(testUser);
                        given(attemptRepository.save(any(UserSectionAttempt.class)))
                                        .willAnswer(invocation -> invocation.getArgument(0));

                        // When
                        SectionAttemptResponse response = quizSectionAttemptService
                                        .startOrResumeAttempt(1L, 1, 1L);

                        // Then
                        assertThat(response.totalQuestions()).isEqualTo(30);
                }

                /**
                 * 성공 케이스: 진행 중인 시도가 여러 개일 때 (중복 시도 자동 복구)
                 *
                 * [리팩토링 반영] selectBestAttempt() 로직:
                 * - 1차 기준: 답변한 문제 수가 가장 많은 시도
                 * - 2차 기준 (동점): 가장 최근 생성된 시도
                 *
                 * 검증 사항:
                 * - NonUniqueResultException이 발생하지 않아야 함
                 * - 답변 수가 많은 시도가 반환되어야 함 (createdAt보다 우선)
                 * - 나머지 중복 시도는 포기(ABANDONED) 처리되어야 함
                 */
                @Test
                @DisplayName("성공: 진행 중인 시도가 여러 개일 때 - 답변 수 기준으로 선택")
                void multipleInProgressAttempts_SelectByAnswerCount() {
                        // Given: 진행 중인 시도가 2개 존재 (Race condition 시뮬레이션)
                        Long courseId = 1L;
                        Integer sectionNumber = 1;
                        Long userId = 1L;

                        // 시도 1 (오래된 시도이지만 답변이 저장됨 - 보존되어야 함)
                        UserSectionAttempt oldAttemptWithAnswers = UserSectionAttempt.builder()
                                        .user(testUser)
                                        .section(testSection)
                                        .totalQuestions(10)
                                        .build();
                        ReflectionTestUtils.setField(oldAttemptWithAnswers, "id", 1L);
                        ReflectionTestUtils.setField(oldAttemptWithAnswers, "createdAt",
                                        java.time.LocalDateTime.now().minusHours(1));

                        // 오래된 시도에 답변 2개 추가
                        List<UserSectionAttemptQuestion> oldAttemptQuestions = new ArrayList<>();
                        for (int i = 0; i < 2; i++) {
                                UserSectionAttemptQuestion aq = UserSectionAttemptQuestion.builder()
                                                .question(testQuestions.get(i))
                                                .orderIndex(i + 1)
                                                .build();
                                aq.saveAnswer("answer" + i); // 답변 저장됨
                                oldAttemptQuestions.add(aq);
                        }
                        ReflectionTestUtils.setField(oldAttemptWithAnswers, "attemptQuestions", oldAttemptQuestions);

                        // 시도 2 (최신 시도이지만 답변 없음 - Race condition으로 생성된 빈 시도)
                        UserSectionAttempt newEmptyAttempt = UserSectionAttempt.builder()
                                        .user(testUser)
                                        .section(testSection)
                                        .totalQuestions(10)
                                        .build();
                        ReflectionTestUtils.setField(newEmptyAttempt, "id", 2L);
                        ReflectionTestUtils.setField(newEmptyAttempt, "createdAt", java.time.LocalDateTime.now());
                        ReflectionTestUtils.setField(newEmptyAttempt, "attemptQuestions", new ArrayList<>()); // 빈 리스트

                        // Repository가 리스트 반환 (최신순 정렬 - 하지만 선택은 답변 수 기준)
                        List<UserSectionAttempt> attempts = new ArrayList<>();
                        attempts.add(newEmptyAttempt); // 최신이지만 빈 시도
                        attempts.add(oldAttemptWithAnswers); // 오래됐지만 답변 있음

                        given(sectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(testSection));

                        given(attemptRepository.findAllInProgressAttemptsWithQuestions(userId, courseId, sectionNumber))
                                        .willReturn(attempts);

                        // When: 시도 시작/재개
                        SectionAttemptResponse response = quizSectionAttemptService
                                        .startOrResumeAttempt(courseId, sectionNumber, userId);

                        // Then: 답변이 있는 오래된 시도가 선택되었는지 확인 (데이터 유실 방지)
                        assertThat(response.attemptId()).isEqualTo(oldAttemptWithAnswers.getId());

                        // 빈 최신 시도는 ABANDONED 상태로 변경되었는지 확인
                        assertThat(newEmptyAttempt.getStatus()).isEqualTo(AttemptStatus.ABANDONED);

                        // 답변이 있는 시도는 여전히 IN_PROGRESS 상태인지 확인
                        assertThat(oldAttemptWithAnswers.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
                }

                /**
                 * 성공 케이스: 답변 수가 동일할 때 최신 시도 선택
                 *
                 * 검증 사항:
                 * - 답변 수가 같으면 createdAt이 최신인 시도가 선택되어야 함
                 */
                @Test
                @DisplayName("성공: 답변 수 동일 시 최신 시도 선택 (Tie-breaker)")
                void multipleInProgressAttempts_TieBreaker() {
                        // Given: 두 시도 모두 답변 수가 동일
                        Long courseId = 1L;
                        Integer sectionNumber = 1;
                        Long userId = 1L;

                        // 시도 1 (오래된 시도, 답변 1개)
                        UserSectionAttempt oldAttempt = UserSectionAttempt.builder()
                                        .user(testUser)
                                        .section(testSection)
                                        .totalQuestions(10)
                                        .build();
                        ReflectionTestUtils.setField(oldAttempt, "id", 1L);
                        ReflectionTestUtils.setField(oldAttempt, "createdAt",
                                        java.time.LocalDateTime.now().minusHours(1));

                        List<UserSectionAttemptQuestion> oldQuestions = new ArrayList<>();
                        UserSectionAttemptQuestion aq1 = UserSectionAttemptQuestion.builder()
                                        .question(testQuestions.get(0))
                                        .orderIndex(1)
                                        .build();
                        aq1.saveAnswer("answer1");
                        oldQuestions.add(aq1);
                        ReflectionTestUtils.setField(oldAttempt, "attemptQuestions", oldQuestions);

                        // 시도 2 (최신 시도, 답변 1개 - 동일한 답변 수)
                        UserSectionAttempt newAttempt = UserSectionAttempt.builder()
                                        .user(testUser)
                                        .section(testSection)
                                        .totalQuestions(10)
                                        .build();
                        ReflectionTestUtils.setField(newAttempt, "id", 2L);
                        ReflectionTestUtils.setField(newAttempt, "createdAt", java.time.LocalDateTime.now());

                        List<UserSectionAttemptQuestion> newQuestions = new ArrayList<>();
                        UserSectionAttemptQuestion aq2 = UserSectionAttemptQuestion.builder()
                                        .question(testQuestions.get(0))
                                        .orderIndex(1)
                                        .build();
                        aq2.saveAnswer("answer2");
                        newQuestions.add(aq2);
                        ReflectionTestUtils.setField(newAttempt, "attemptQuestions", newQuestions);

                        List<UserSectionAttempt> attempts = List.of(newAttempt, oldAttempt);

                        given(sectionRepository.findByIdWithCourseAndQuestions(courseId, sectionNumber))
                                        .willReturn(Optional.of(testSection));

                        given(attemptRepository.findAllInProgressAttemptsWithQuestions(userId, courseId, sectionNumber))
                                        .willReturn(attempts);

                        // When
                        SectionAttemptResponse response = quizSectionAttemptService
                                        .startOrResumeAttempt(courseId, sectionNumber, userId);

                        // Then: 답변 수 동일하므로 최신 시도가 선택됨
                        assertThat(response.attemptId()).isEqualTo(newAttempt.getId());
                        assertThat(oldAttempt.getStatus()).isEqualTo(AttemptStatus.ABANDONED);
                        assertThat(newAttempt.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
                }
        }

        // ==========================================================
        // resumeAttempt() 테스트 (명시적 attemptId 사용 재개)
        // ==========================================================

        /**
         * resumeAttempt() 메서드에 대한 중첩 테스트 클래스
         *
         * 클라이언트가 이미 알고 있는 attemptId를 사용하여 특정 시도를 재개하는 기능 테스트
         */
        @Nested
        @DisplayName("resumeAttempt 메서드 (명시적 attemptId 사용 재개)")
        class ResumeAttemptTest {

                @Test
                @DisplayName("성공: 본인의 진행 중인 시도 재개")
                void successResumeOwnInProgressAttempt() {
                        // Given
                        Long attemptId = 1L;
                        Long userId = 1L;

                        // 진행 중인 시도 설정
                        UserSectionAttempt attempt = UserSectionAttempt.builder()
                                        .user(testUser)
                                        .section(testSection)
                                        .totalQuestions(10)
                                        .build();
                        ReflectionTestUtils.setField(attempt, "id", attemptId);
                        ReflectionTestUtils.setField(attempt, "status", AttemptStatus.IN_PROGRESS);

                        // 문제 설정
                        List<UserSectionAttemptQuestion> attemptQuestions = new ArrayList<>();
                        UserSectionAttemptQuestion aq = UserSectionAttemptQuestion.builder()
                                        .question(testQuestions.get(0))
                                        .orderIndex(1)
                                        .build();
                        ReflectionTestUtils.setField(aq, "attempt", attempt);
                        aq.saveAnswer("A");
                        attemptQuestions.add(aq);
                        ReflectionTestUtils.setField(attempt, "attemptQuestions", attemptQuestions);

                        given(attemptRepository.findByIdWithQuestions(attemptId))
                                        .willReturn(Optional.of(attempt));

                        // When
                        SectionAttemptResponse response = quizSectionAttemptService.resumeAttempt(attemptId, userId);

                        // Then
                        assertThat(response.attemptId()).isEqualTo(attemptId);
                        assertThat(response.questions()).hasSize(1);
                        assertThat(response.questions().get(0).userAnswer()).isEqualTo("A");
                }

                @Test
                @DisplayName("실패: 존재하지 않는 시도")
                void failWhenAttemptNotFound() {
                        // Given
                        Long attemptId = 999L;
                        Long userId = 1L;

                        given(attemptRepository.findByIdWithQuestions(attemptId))
                                        .willReturn(Optional.empty());

                        // When & Then
                        assertThatThrownBy(() -> quizSectionAttemptService.resumeAttempt(attemptId, userId))
                                        .isInstanceOf(NotFoundException.class);
                }

                @Test
                @DisplayName("실패: 본인 시도가 아닐 때")
                void failWhenNotOwner() {
                        // Given
                        Long attemptId = 1L;
                        Long otherUserId = 999L;

                        UserSectionAttempt attempt = UserSectionAttempt.builder()
                                        .user(testUser) // userId = 1
                                        .section(testSection)
                                        .totalQuestions(10)
                                        .build();
                        ReflectionTestUtils.setField(attempt, "id", attemptId);
                        ReflectionTestUtils.setField(attempt, "status", AttemptStatus.IN_PROGRESS);
                        ReflectionTestUtils.setField(attempt, "attemptQuestions", new ArrayList<>());

                        given(attemptRepository.findByIdWithQuestions(attemptId))
                                        .willReturn(Optional.of(attempt));

                        // When & Then
                        assertThatThrownBy(() -> quizSectionAttemptService.resumeAttempt(attemptId, otherUserId))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("본인의 시도만 재개할 수 있습니다");
                }

                @Test
                @DisplayName("실패: 이미 완료된 시도")
                void failWhenAlreadyCompleted() {
                        // Given
                        Long attemptId = 1L;
                        Long userId = 1L;

                        UserSectionAttempt attempt = UserSectionAttempt.builder()
                                        .user(testUser)
                                        .section(testSection)
                                        .totalQuestions(10)
                                        .build();
                        ReflectionTestUtils.setField(attempt, "id", attemptId);
                        ReflectionTestUtils.setField(attempt, "status", AttemptStatus.SUBMITTED);
                        ReflectionTestUtils.setField(attempt, "attemptQuestions", new ArrayList<>());

                        given(attemptRepository.findByIdWithQuestions(attemptId))
                                        .willReturn(Optional.of(attempt));

                        // When & Then
                        assertThatThrownBy(() -> quizSectionAttemptService.resumeAttempt(attemptId, userId))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("이미 완료된 시도는 재개할 수 없습니다");
                }
        }

        // ==========================================================
        // saveAnswer() 테스트 (단일 답안 실시간 저장)
        // ==========================================================

        /**
         * saveAnswer() 메서드에 대한 중첩 테스트 클래스
         *
         * [리팩토링 반영]
         * - 서비스가 attemptRepository.findById() 대신
         *   attemptQuestionRepository.findByAttemptIdAndQuestionId() 호출
         * - UserSectionAttemptQuestion에 attempt 필드 설정 필요 (권한 체크용)
         */
        @Nested
        @DisplayName("saveAnswer 메서드 (단일 답안 실시간 저장)")
        class SaveAnswerTest {

                /**
                 * 성공 케이스: 단일 답안 정상 저장
                 *
                 * 검증 사항:
                 * - 사용자가 제출한 단일 답안이 정확히 저장되어야 함
                 * - 문제 풀이 중 "다음" 버튼 클릭 시 호출되는 시나리오
                 */
                @Test
                @DisplayName("성공: 단일 답안 정상 저장")
                void saveAnswer_Success() {
                        // Given: 시도와 단일 답안 요청 준비
                        Long attemptId = 1L;
                        Long userId = 1L;
                        Long questionId = 1L;

                        // UserSectionAttemptQuestion 생성 및 attempt 연결 설정
                        UserSectionAttemptQuestion aq1 = UserSectionAttemptQuestion.builder()
                                        .question(testQuestions.get(0))
                                        .orderIndex(1)
                                        .build();
                        // [중요] attempt 필드 설정 (권한 체크에 필요)
                        ReflectionTestUtils.setField(aq1, "attempt", testAttempt);

                        // 단일 답안 요청 데이터 (문제 1번에 대한 답안)
                        SaveAnswerRequest request = new SaveAnswerRequest(
                                        new SaveAnswerRequest.AnswerItem(questionId, "1"));

                        // [수정] attemptQuestionRepository.findByAttemptIdAndQuestionId() Mock
                        given(attemptQuestionRepository.findByAttemptIdAndQuestionId(attemptId, questionId))
                                        .willReturn(Optional.of(aq1));

                        // When: 단일 답안 저장
                        quizSectionAttemptService.saveAnswer(attemptId, request, userId);

                        // Then: 해당 문제의 답안만 저장되었는지 확인
                        assertThat(aq1.getUserAnswer()).isEqualTo("1");
                        then(attemptQuestionRepository).should().save(aq1);
                }

                /**
                 * 성공 케이스: 동일 문제 답안 덮어쓰기 (멱등성 테스트)
                 *
                 * 검증 사항:
                 * - 같은 문제에 여러 번 답안을 저장해도 마지막 값으로 덮어씀
                 * - 멱등성이 보장되어야 함
                 */
                @Test
                @DisplayName("성공: 동일 문제 답안 덮어쓰기 (멱등성 보장)")
                void saveAnswer_Idempotent() {
                        // Given: 이미 답안이 저장된 상태
                        Long attemptId = 1L;
                        Long userId = 1L;
                        Long questionId = 1L;

                        UserSectionAttemptQuestion aq1 = UserSectionAttemptQuestion.builder()
                                        .question(testQuestions.get(0))
                                        .orderIndex(1)
                                        .build();
                        ReflectionTestUtils.setField(aq1, "userAnswer", "OLD_ANSWER"); // 기존 답안
                        ReflectionTestUtils.setField(aq1, "attempt", testAttempt);

                        // 새로운 답안으로 덮어쓰기
                        SaveAnswerRequest request = new SaveAnswerRequest(
                                        new SaveAnswerRequest.AnswerItem(questionId, "NEW_ANSWER"));

                        // [수정] attemptQuestionRepository Mock
                        given(attemptQuestionRepository.findByAttemptIdAndQuestionId(attemptId, questionId))
                                        .willReturn(Optional.of(aq1));

                        // When: 답안 재저장
                        quizSectionAttemptService.saveAnswer(attemptId, request, userId);

                        // Then: 새로운 답안으로 덮어쓰기 확인
                        assertThat(aq1.getUserAnswer()).isEqualTo("NEW_ANSWER");
                }

                /**
                 * 실패 케이스: 존재하지 않는 문제
                 *
                 * [수정] 서비스 리팩토링 후 동작 변경:
                 * - 기존: attemptRepository.findById() 실패 → NotFoundException
                 * - 변경: attemptQuestionRepository.findByAttemptIdAndQuestionId() 실패
                 *        → BusinessException("QUESTION_NOT_FOUND")
                 */
                @Test
                @DisplayName("실패: 존재하지 않는 문제 - BusinessException(QUESTION_NOT_FOUND) 발생")
                void questionNotFound_ThrowsException() {
                        // Given: 존재하지 않는 문제 ID
                        Long attemptId = 1L;
                        Long userId = 1L;
                        Long nonExistentQuestionId = 999L;

                        SaveAnswerRequest request = new SaveAnswerRequest(
                                        new SaveAnswerRequest.AnswerItem(nonExistentQuestionId, "1"));

                        // [수정] attemptQuestionRepository Mock - 문제를 찾을 수 없음
                        given(attemptQuestionRepository.findByAttemptIdAndQuestionId(attemptId, nonExistentQuestionId))
                                        .willReturn(Optional.empty());

                        // When & Then: BusinessException 발생 확인
                        assertThatThrownBy(() -> quizSectionAttemptService.saveAnswer(attemptId, request, userId))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("해당 문제를 찾을 수 없습니다");
                }

                /**
                 * 실패 케이스: 다른 사용자의 시도
                 *
                 * 예상 동작:
                 * - BusinessException(FORBIDDEN)이 발생해야 함
                 * - 본인의 시도만 수정할 수 있음
                 */
                @Test
                @DisplayName("실패: 다른 사용자의 시도 - BusinessException(FORBIDDEN) 발생")
                void notOwner_ThrowsException() {
                        // Given: 다른 사용자가 시도에 접근
                        Long attemptId = 1L;
                        Long otherUserId = 999L; // 다른 사용자
                        Long questionId = 1L;

                        UserSectionAttemptQuestion aq1 = UserSectionAttemptQuestion.builder()
                                        .question(testQuestions.get(0))
                                        .orderIndex(1)
                                        .build();
                        // testAttempt는 userId=1L 소유
                        ReflectionTestUtils.setField(aq1, "attempt", testAttempt);

                        SaveAnswerRequest request = new SaveAnswerRequest(
                                        new SaveAnswerRequest.AnswerItem(questionId, "1"));

                        // [수정] attemptQuestionRepository Mock
                        given(attemptQuestionRepository.findByAttemptIdAndQuestionId(attemptId, questionId))
                                        .willReturn(Optional.of(aq1));

                        // When & Then: 예외 발생 확인
                        assertThatThrownBy(() -> quizSectionAttemptService.saveAnswer(attemptId, request, otherUserId))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("본인의 시도만 수정할 수 있습니다");
                }

                /**
                 * 실패 케이스: 이미 완료된 시도
                 *
                 * 예상 동작:
                 * - BusinessException(BAD_REQUEST)이 발생해야 함
                 * - 완료된 시도는 수정할 수 없음
                 */
                @Test
                @DisplayName("실패: 완료된 시도 수정 불가 - BusinessException(BAD_REQUEST) 발생")
                void completedAttempt_ThrowsException() {
                        // Given: 완료된 시도
                        Long attemptId = 1L;
                        Long userId = 1L;
                        Long questionId = 1L;

                        // 시도를 완료 상태로 변경
                        testAttempt.complete(7, 70); // 7개 정답, 통과 점수 70

                        UserSectionAttemptQuestion aq1 = UserSectionAttemptQuestion.builder()
                                        .question(testQuestions.get(0))
                                        .orderIndex(1)
                                        .build();
                        ReflectionTestUtils.setField(aq1, "attempt", testAttempt);

                        SaveAnswerRequest request = new SaveAnswerRequest(
                                        new SaveAnswerRequest.AnswerItem(questionId, "1"));

                        // [수정] attemptQuestionRepository Mock
                        given(attemptQuestionRepository.findByAttemptIdAndQuestionId(attemptId, questionId))
                                        .willReturn(Optional.of(aq1));

                        // When & Then: 예외 발생 확인
                        assertThatThrownBy(() -> quizSectionAttemptService.saveAnswer(attemptId, request, userId))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("완료된 시도는 수정할 수 없습니다");
                }
        }

        // ==========================================================
        // submitAttempt() 테스트
        // ==========================================================

        @Nested
        @DisplayName("submitAttempt 메서드")
        class SubmitAttemptTest {

                /**
                 * 성공 케이스: 시험 통과 (다음 섹션 해금)
                 *
                 * 검증 사항:
                 * - 채점이 정확히 수행되어야 함
                 * - 통과 시 다음 섹션이 해금되어야 함
                 * - 진행 상황이 업데이트되어야 함
                 */
                @Test
                @DisplayName("성공: 시험 통과 - 다음 섹션 해금")
                void submitAndPass_UnlocksNextSection() {
                        // Given: 통과 가능한 답안
                        Long attemptId = 1L;
                        Long userId = 1L;

                        // 10문제 중 8문제 정답 (80점, 통과 점수 70점)
                        List<UserSectionAttemptQuestion> results = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                                UserSectionAttemptQuestion aq = UserSectionAttemptQuestion.builder()
                                                .orderIndex(i + 1)
                                                .question(testQuestions.get(i))
                                                .build();

                                // submitAndPass: 8개 정답
                                if (i < 8) {
                                        ReflectionTestUtils.setField(aq, "userAnswer",
                                                        testQuestions.get(i).getCorrectAnswer());
                                } else {
                                        ReflectionTestUtils.setField(aq, "userAnswer", "WRONG_ANSWER");
                                }
                                // isCorrect는 Service에서 계산됨

                                results.add(aq);
                        }
                        ReflectionTestUtils.setField(testAttempt, "attemptQuestions", results);

                        // Mock 동작 정의
                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.of(testAttempt));

                        // 채점 후 8개 정답
                        // gradeAllByAttemptId 및 countCorrectByAttemptId 호출 제거됨 (내부 로직으로 변경)
                        // 대신
                        // attemptQuestionRepository.findByAttemptIdWithQuestionOrderByOrderIndex(attemptId)가
                        // 호출될 때 results가 반환되고, 여기서 calculate logic이 수행됨.

                        // 진행 상황 업데이트를 위한 Mock
                        given(progressRepository.findByUserIdAndCourseId(userId, 1L))
                                        .willReturn(Optional.empty()); // 첫 섹션이므로 진행 기록 없음
                        given(userRepository.getReferenceById(userId))
                                        .willReturn(testUser);
                        given(courseRepository.getReferenceById(1L))
                                        .willReturn(testCourse);
                        given(progressRepository.save(any(UserCourseProgress.class)))
                                        .willAnswer(invocation -> invocation.getArgument(0));

                        // 코스 미완료 (섹션 1만 통과)
                        given(attemptRepository.countPassedSections(userId, 1L))
                                        .willReturn(1);

                        // 1. 문제 목록 조회 (submitAttempt 초반에 호출됨)
                        given(attemptQuestionRepository.findByAttemptIdWithQuestionOrderByOrderIndex(attemptId))
                                        .willReturn(results);

                        // When: 제출
                        AttemptResultResponse response = quizSectionAttemptService
                                        .submitAttempt(attemptId, userId);

                        // Then: 통과 확인
                        assertThat(response.isPassed()).isTrue();
                        assertThat(response.score()).isEqualTo(80); // 80점
                        assertThat(response.correctCount()).isEqualTo(8);
                        assertThat(response.totalQuestions()).isEqualTo(10);
                        assertThat(response.isNextSectionUnlocked()).isTrue(); // 다음 섹션 해금됨

                        // 진행 상황 저장 확인
                        ArgumentCaptor<UserCourseProgress> progressCaptor = ArgumentCaptor
                                        .forClass(UserCourseProgress.class);
                        then(progressRepository).should().save(progressCaptor.capture());
                        UserCourseProgress savedProgress = progressCaptor.getValue();
                        assertThat(savedProgress.getCurrentSection()).isEqualTo(2); // 섹션 2로 진행
                }

                /**
                 * 성공 케이스: 시험 불합격 (다음 섹션 미해금)
                 *
                 * 검증 사항:
                 * - 통과 점수 미달 시 불합격 처리
                 * - 다음 섹션이 해금되지 않아야 함
                 * - 진행 상황이 업데이트되지 않아야 함
                 */
                @Test
                @DisplayName("성공: 시험 불합격 - 다음 섹션 미해금")
                void submitAndFail_DoesNotUnlockNextSection() {
                        // Given: 불합격 답안 (10문제 중 5문제만 정답 - 50점)
                        Long attemptId = 1L;
                        Long userId = 1L;

                        List<UserSectionAttemptQuestion> results = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                                UserSectionAttemptQuestion aq = UserSectionAttemptQuestion.builder()
                                                .orderIndex(i + 1)
                                                .question(testQuestions.get(i))
                                                .build();

                                // submitAndFail: 5개 정답
                                if (i < 5) {
                                        ReflectionTestUtils.setField(aq, "userAnswer",
                                                        testQuestions.get(i).getCorrectAnswer());
                                } else {
                                        ReflectionTestUtils.setField(aq, "userAnswer", "WRONG_ANSWER");
                                }

                                results.add(aq);
                        }

                        // Mock 동작 정의

                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.of(testAttempt));

                        // gradeAllByAttemptId, countCorrectByAttemptId 제거됨

                        given(attemptQuestionRepository.findByAttemptIdWithQuestionOrderByOrderIndex(attemptId))
                                        .willReturn(results);

                        // When: 제출
                        AttemptResultResponse response = quizSectionAttemptService
                                        .submitAttempt(attemptId, userId);

                        // Then: 불합격 확인
                        assertThat(response.isPassed()).isFalse(); // 불합격
                        assertThat(response.score()).isEqualTo(50); // 50점
                        assertThat(response.correctCount()).isEqualTo(5);
                        assertThat(response.isNextSectionUnlocked()).isFalse(); // 해금 안됨

                        // 진행 상황이 저장되지 않았는지 확인
                        then(progressRepository).should(never()).save(any(UserCourseProgress.class));
                }

                /**
                 * 성공 케이스: 코스 완료 시 배지 수여
                 *
                 * 검증 사항:
                 * - 모든 섹션 통과 시 코스 완료로 인정
                 * - 배지가 수여되어야 함
                 */
                @Test
                @DisplayName("성공: 코스 완료 시 배지 수여")
                void completeCourse_AwardsBadge() {
                        // Given: 마지막 섹션(섹션 5) 통과
                        Long attemptId = 1L;
                        Long userId = 1L;

                        // 섹션 5로 변경
                        QuizCourseSection section5 = QuizCourseSection.builder()
                                        .quizCourseId(1L)
                                        .sectionNumber(5) // 마지막 섹션
                                        .passScore(70)
                                        .course(testCourse)
                                        .build();
                        ReflectionTestUtils.setField(testAttempt, "section", section5);

                        List<UserSectionAttemptQuestion> results = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                                UserSectionAttemptQuestion aq = UserSectionAttemptQuestion.builder()
                                                .orderIndex(i + 1)
                                                .question(testQuestions.get(i))
                                                .build();

                                // 내부 로직 채점을 위해 userAnswer 설정
                                if (i < 8) {
                                        ReflectionTestUtils.setField(aq, "userAnswer",
                                                        testQuestions.get(i).getCorrectAnswer());
                                } else {
                                        ReflectionTestUtils.setField(aq, "userAnswer", "WRONG_ANSWER");
                                }

                                results.add(aq);
                        }

                        // Mock 동작 정의
                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.of(testAttempt));

                        // gradeAllByAttemptId, countCorrectByAttemptId 제거됨

                        // 이미 섹션 4까지 완료된 상태
                        UserCourseProgress existingProgress = UserCourseProgress.builder()
                                        .userId(userId)
                                        .courseId(1L)
                                        .currentSection(5) // 섹션 5까지 해금됨
                                        .build();
                        given(progressRepository.findByUserIdAndCourseId(userId, 1L))
                                        .willReturn(Optional.of(existingProgress));
                        given(progressRepository.save(any(UserCourseProgress.class)))
                                        .willAnswer(invocation -> invocation.getArgument(0));

                        // 모든 섹션 통과 (코스 완료)
                        given(attemptRepository.countPassedSections(userId, 1L))
                                        .willReturn(5); // 총 5개 섹션 모두 통과

                        // 배지 정보
                        Badge badge = Badge.builder()
                                        .code("BADGE_001")
                                        .name("Test Badge")
                                        .description("Complete Test Course")
                                        .build();
                        given(badgeRepository.findByCode("BADGE_001"))
                                        .willReturn(Optional.of(badge));

                        // 1. 문제 목록 조회 (submitAttempt 초반에 호출됨)
                        given(attemptQuestionRepository.findByAttemptIdWithQuestionOrderByOrderIndex(attemptId))
                                        .willReturn(results);

                        // When: 제출
                        AttemptResultResponse response = quizSectionAttemptService
                                        .submitAttempt(attemptId, userId);

                        // Then: 배지 수여 확인
                        assertThat(response.isPassed()).isTrue();
                        assertThat(response.earnedBadge()).isNotNull();
                        assertThat(response.earnedBadge().code()).isEqualTo("BADGE_001");
                        assertThat(response.earnedBadge().name()).isEqualTo("Test Badge");
                }

                /**
                 * 실패 케이스: 존재하지 않는 시도 제출
                 *
                 * 예상 동작:
                 * - NotFoundException이 발생해야 함
                 */
                @Test
                @DisplayName("실패: 존재하지 않는 시도 제출 - NotFoundException 발생")
                void submitNonExistentAttempt_ThrowsException() {
                        // Given
                        Long attemptId = 999L;
                        Long userId = 1L;

                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.empty());

                        // When & Then
                        assertThatThrownBy(() -> quizSectionAttemptService.submitAttempt(attemptId, userId))
                                        .isInstanceOf(NotFoundException.class);
                }

                /**
                 * 실패 케이스: 다른 사용자의 시도 제출
                 *
                 * 예상 동작:
                 * - BusinessException(FORBIDDEN)이 발생해야 함
                 */
                @Test
                @DisplayName("실패: 다른 사용자의 시도 제출 - BusinessException(FORBIDDEN) 발생")
                void submitOthersAttempt_ThrowsException() {
                        // Given
                        Long attemptId = 1L;
                        Long otherUserId = 999L;

                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.of(testAttempt));

                        // When & Then
                        assertThatThrownBy(() -> quizSectionAttemptService.submitAttempt(attemptId, otherUserId))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("본인의 시도만 제출할 수 있습니다");
                }

                /**
                 * 실패 케이스: 이미 완료된 시도 재제출
                 *
                 * 예상 동작:
                 * - BusinessException(BAD_REQUEST)이 발생해야 함
                 */
                @Test
                @DisplayName("실패: 이미 완료된 시도 재제출 - BusinessException(BAD_REQUEST) 발생")
                void submitCompletedAttempt_ThrowsException() {
                        // Given: 이미 완료된 시도
                        Long attemptId = 1L;
                        Long userId = 1L;

                        testAttempt.complete(8, 70); // 완료 처리

                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.of(testAttempt));

                        // When & Then
                        assertThatThrownBy(() -> quizSectionAttemptService.submitAttempt(attemptId, userId))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("이미 완료된 시도입니다");
                }

                /**
                 * 성공 케이스: 이미 다음 섹션이 해금된 상태에서 재도전 통과
                 *
                 * 검증 사항:
                 * - 이미 해금된 섹션은 다시 해금되지 않아야 함
                 * - isNextSectionUnlocked = false 반환
                 */
                @Test
                @DisplayName("성공: 이미 해금된 섹션 - 재해금하지 않음")
                void alreadyUnlockedSection_DoesNotUnlockAgain() {
                        // Given: 섹션 1 재도전 (이미 섹션 3까지 해금됨)
                        Long attemptId = 1L;
                        Long userId = 1L;

                        List<UserSectionAttemptQuestion> results = new ArrayList<>();
                        for (int i = 0; i < 10; i++) {
                                UserSectionAttemptQuestion aq = UserSectionAttemptQuestion.builder()
                                                .orderIndex(i + 1)
                                                .question(testQuestions.get(i))
                                                .build();

                                // 내부 로직 채점을 위해 userAnswer 설정
                                if (i < 8) {
                                        ReflectionTestUtils.setField(aq, "userAnswer",
                                                        testQuestions.get(i).getCorrectAnswer());
                                } else {
                                        ReflectionTestUtils.setField(aq, "userAnswer", "WRONG_ANSWER");
                                }
                                results.add(aq);
                        }

                        // Mock 동작 정의
                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.of(testAttempt));

                        // gradeAllByAttemptId, countCorrectByAttemptId 제거됨

                        // 이미 섹션 3까지 해금된 상태
                        UserCourseProgress existingProgress = UserCourseProgress.builder()
                                        .userId(userId)
                                        .courseId(1L)
                                        .currentSection(3) // 섹션 3까지 해금됨
                                        .build();
                        given(progressRepository.findByUserIdAndCourseId(userId, 1L))
                                        .willReturn(Optional.of(existingProgress));

                        given(attemptRepository.countPassedSections(userId, 1L))
                                        .willReturn(1);
                        // 1. 문제 목록 조회
                        given(attemptQuestionRepository.findByAttemptIdWithQuestionOrderByOrderIndex(attemptId))
                                        .willReturn(results);

                        // When
                        AttemptResultResponse response = quizSectionAttemptService
                                        .submitAttempt(attemptId, userId);

                        // Then: 다음 섹션이 새로 해금되지 않음
                        assertThat(response.isPassed()).isTrue();
                        assertThat(response.isNextSectionUnlocked()).isFalse(); // 이미 해금됨

                        // 진행 상황이 저장되지 않았는지 확인
                        then(progressRepository).should(never()).save(any(UserCourseProgress.class));
                }
        }

        // ==========================================================
        // abandonAttempt() 테스트
        // ==========================================================

        @Nested
        @DisplayName("abandonAttempt 메서드")
        class AbandonAttemptTest {

                /**
                 * 성공 케이스: 시도 포기
                 *
                 * 검증 사항:
                 * - 진행 중인 시도를 포기할 수 있어야 함
                 * - 시도 상태가 ABANDONED로 변경되어야 함
                 */
                @Test
                @DisplayName("성공: 진행 중인 시도 포기")
                void abandonAttempt_Success() {
                        // Given
                        Long attemptId = 1L;
                        Long userId = 1L;

                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.of(testAttempt));

                        // When
                        quizSectionAttemptService.abandonAttempt(attemptId, userId);

                        // Then: 포기 상태로 변경되었는지 확인
                        assertThat(testAttempt.getStatus()).isEqualTo(AttemptStatus.ABANDONED);
                }

                /**
                 * 실패 케이스: 존재하지 않는 시도 포기
                 *
                 * 예상 동작:
                 * - NotFoundException이 발생해야 함
                 */
                @Test
                @DisplayName("실패: 존재하지 않는 시도 포기 - NotFoundException 발생")
                void abandonNonExistentAttempt_ThrowsException() {
                        // Given
                        Long attemptId = 999L;
                        Long userId = 1L;

                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.empty());

                        // When & Then
                        assertThatThrownBy(() -> quizSectionAttemptService.abandonAttempt(attemptId, userId))
                                        .isInstanceOf(NotFoundException.class);
                }

                /**
                 * 실패 케이스: 다른 사용자의 시도 포기
                 *
                 * 예상 동작:
                 * - BusinessException(FORBIDDEN)이 발생해야 함
                 */
                @Test
                @DisplayName("실패: 다른 사용자의 시도 포기 - BusinessException(FORBIDDEN) 발생")
                void abandonOthersAttempt_ThrowsException() {
                        // Given
                        Long attemptId = 1L;
                        Long otherUserId = 999L;

                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.of(testAttempt));

                        // When & Then
                        assertThatThrownBy(() -> quizSectionAttemptService.abandonAttempt(attemptId, otherUserId))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("본인의 시도만 포기할 수 있습니다");
                }

                /**
                 * 실패 케이스: 이미 완료된 시도 포기
                 *
                 * 예상 동작:
                 * - BusinessException(BAD_REQUEST)이 발생해야 함
                 * - 완료된 시도는 포기할 수 없음
                 */
                @Test
                @DisplayName("실패: 완료된 시도 포기 불가 - BusinessException(BAD_REQUEST) 발생")
                void abandonCompletedAttempt_ThrowsException() {
                        // Given: 완료된 시도
                        Long attemptId = 1L;
                        Long userId = 1L;

                        testAttempt.complete(8, 70); // 완료 처리

                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.of(testAttempt));

                        // When & Then
                        assertThatThrownBy(() -> quizSectionAttemptService.abandonAttempt(attemptId, userId))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("이미 완료된 시도입니다");
                }

                /**
                 * 실패 케이스: 이미 포기한 시도 재포기
                 *
                 * 예상 동작:
                 * - BusinessException(BAD_REQUEST)이 발생해야 함
                 */
                @Test
                @DisplayName("실패: 이미 포기한 시도 재포기 - BusinessException(BAD_REQUEST) 발생")
                void abandonAlreadyAbandonedAttempt_ThrowsException() {
                        // Given: 이미 포기한 시도
                        Long attemptId = 1L;
                        Long userId = 1L;

                        testAttempt.abandon(); // 포기 처리

                        given(attemptRepository.findById(attemptId))
                                        .willReturn(Optional.of(testAttempt));

                        // When & Then
                        assertThatThrownBy(() -> quizSectionAttemptService.abandonAttempt(attemptId, userId))
                                        .isInstanceOf(BusinessException.class)
                                        .hasMessageContaining("이미 완료된 시도입니다");
                }
        }
}