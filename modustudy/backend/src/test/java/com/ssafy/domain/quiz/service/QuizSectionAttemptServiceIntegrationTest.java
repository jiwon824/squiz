package com.ssafy.domain.quiz.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.common.exception.NotFoundException;
import com.ssafy.common.exception.SectionLockedException;
import com.ssafy.domain.gamification.entity.Badge;
import com.ssafy.domain.gamification.entity.BadgeCategory;
import com.ssafy.domain.gamification.repository.BadgeRepository;
import com.ssafy.domain.quiz.dto.request.SaveAnswerRequest;
import com.ssafy.domain.quiz.dto.response.AttemptResultResponse;
import com.ssafy.domain.quiz.dto.response.SectionAttemptResponse;
import com.ssafy.domain.quiz.entity.*;
import com.ssafy.domain.quiz.entity.enums.AttemptStatus;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import com.ssafy.domain.quiz.repository.QuizCourseRepository;
import com.ssafy.domain.quiz.repository.QuizCourseSectionRepository;
import com.ssafy.domain.quiz.repository.UserCourseProgressRepository;
import com.ssafy.domain.quiz.repository.UserSectionAttemptQuestionRepository;
import com.ssafy.domain.quiz.repository.UserSectionAttemptRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * QuizSectionAttemptService 통합 테스트.
 *
 * <p>이 테스트는 실제 데이터베이스와 상호작용하여 서비스 로직을 검증합니다.</p>
 *
 * <h3>테스트 규칙:</h3>
 * <ul>
 *   <li>하드코딩된 ID 금지 - entity.getId() 사용</li>
 *   <li>부모 엔티티 저장 + flush() 후 자식 엔티티 저장</li>
 *   <li>벌크 삭제 후 entityManager.flush() + entityManager.clear()</li>
 *   <li>User 생성 시 모든 필수 필드 포함</li>
 * </ul>
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("QuizSectionAttemptService 통합 테스트")
class QuizSectionAttemptServiceIntegrationTest {

    // ========== 의존성 주입 ==========

    @Autowired
    private QuizSectionAttemptService quizSectionAttemptService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuizCourseRepository courseRepository;

    @Autowired
    private QuizCourseSectionRepository sectionRepository;

    @Autowired
    private UserSectionAttemptRepository attemptRepository;

    @Autowired
    private UserSectionAttemptQuestionRepository attemptQuestionRepository;

    @Autowired
    private UserCourseProgressRepository progressRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private EntityManager entityManager;

    // ========== 테스트 데이터 (ID는 저장 후 동적으로 할당) ==========

    private User testUser;
    private QuizCourse testCourse;
    private QuizCourseSection testSection;
    private List<QuizCourseQuestion> testQuestions;
    private Badge testBadge;

    // ========== 셋업 및 정리 ==========

    /**
     * 각 테스트 전에 테스트 데이터를 생성합니다.
     *
     * <p>부모 엔티티를 먼저 저장하고 flush() 한 후 자식 엔티티를 저장합니다.</p>
     */
    @BeforeEach
    void setUp() {
        // 1. User 생성 (모든 필수 필드 포함)
        testUser = createUserWithAllRequiredFields("testUser", "test@example.com", "TestNick");
        testUser = userRepository.saveAndFlush(testUser);
        entityManager.flush();

        // 2. Badge 생성 (Course보다 먼저 - badgeCode 참조용)
        testBadge = Badge.builder()
                .code("TEST_BADGE")
                .name("Test Badge")
                .description("테스트 배지")
                .icon("🏆")
                .category(BadgeCategory.MASTER)
                .conditionType("COURSE_COMPLETE")
                .conditionValue(1)
                .sortOrder(1)
                .build();
        testBadge = badgeRepository.saveAndFlush(testBadge);
        entityManager.flush();

        // 3. QuizCourse 생성 (부모)
        testCourse = QuizCourse.builder()
                .code("TEST_COURSE")
                .name("Test Course")
                .description("테스트 코스")
                .badgeCode("TEST_BADGE")
                .totalSections(5)
                .isActive(true)
                .sortOrder(1)
                .build();
        testCourse = courseRepository.saveAndFlush(testCourse);
        entityManager.flush();

        // 4. QuizCourseSection 생성
        testSection = QuizCourseSection.create(
                testCourse,
                1,
                "Section 1",
                "테스트 섹션",
                70
        );
        testSection = sectionRepository.saveAndFlush(testSection);
        entityManager.flush();

        // 5. QuizCourseQuestion 생성 (섹션의 questions 리스트에 추가 - Cascade로 저장)
        testQuestions = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            QuizCourseQuestion question = QuizCourseQuestion.builder()
                    .section(testSection)
                    .questionNumber(i)
                    .questionText("Question " + i)
                    .questionType(QuestionType.MULTIPLE_CHOICE)
                    .options("[{\"id\":\"1\",\"text\":\"Option 1\"},{\"id\":\"2\",\"text\":\"Option 2\"}]")
                    .correctAnswer("1")
                    .explanation("Explanation " + i)
                    .build();
            testQuestions.add(question);
            // 양방향 관계 설정: Section의 questions 리스트에 추가
            testSection.getQuestions().add(question);
        }

        // Cascade로 Questions도 함께 저장됨
        testSection = sectionRepository.saveAndFlush(testSection);
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * 각 테스트 후에 데이터를 정리합니다.
     *
     * <p>벌크 삭제 후 반드시 flush() + clear() 호출</p>
     */
    @AfterEach
    void tearDown() {
        // 자식부터 삭제 (FK 제약 조건 고려)
        // Questions는 Section의 cascade로 함께 삭제됨 (orphanRemoval = true)
        attemptQuestionRepository.deleteAll();
        attemptRepository.deleteAll();
        progressRepository.deleteAll();
        sectionRepository.deleteAll();  // Questions도 cascade로 삭제됨
        courseRepository.deleteAll();
        badgeRepository.deleteAll();
        userRepository.deleteAll();

        // 벌크 삭제 후 반드시 flush + clear
        entityManager.flush();
        entityManager.clear();
    }

    // ========== Helper Methods ==========

    /**
     * 모든 필수 필드를 포함한 User를 생성합니다.
     *
     * @param userId   사용자 ID
     * @param email    이메일
     * @param nickname 닉네임
     * @return 생성된 User 엔티티
     */
    private User createUserWithAllRequiredFields(String userId, String email, String nickname) {
        return User.builder()
                .userId(userId)
                .email(email)
                .nickname(nickname)
                .name("Test User")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build();
    }

    /**
     * 다른 사용자를 생성합니다.
     */
    private User createOtherUser() {
        User otherUser = createUserWithAllRequiredFields(
                "otherUser",
                "other@example.com",
                "OtherNick"
        );
        return userRepository.saveAndFlush(otherUser);
    }

    // ==========================================================
    // startOrResumeAttempt() 테스트
    // ==========================================================

    @Nested
    @DisplayName("startOrResumeAttempt 메서드")
    class StartOrResumeAttemptTest {

        @Test
        @DisplayName("성공: 첫 섹션(섹션 1) 시작 - 항상 해금됨")
        void startFirstSection_Success() {
            // Given: 저장된 엔티티의 ID 사용 (하드코딩 금지)
            Long courseId = testCourse.getId();
            Integer sectionNumber = testSection.getSectionNumber();
            Long userId = testUser.getId();

            // When
            SectionAttemptResponse response = quizSectionAttemptService
                    .startOrResumeAttempt(courseId, sectionNumber, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.attemptId()).isNotNull();
            assertThat(response.sectionNumber()).isEqualTo(sectionNumber);
            assertThat(response.sectionName()).isEqualTo("Section 1");
            assertThat(response.status()).isEqualTo(AttemptStatus.IN_PROGRESS);
            assertThat(response.totalQuestions()).isEqualTo(10);
            assertThat(response.passScore()).isEqualTo(70);
        }

        @Test
        @DisplayName("성공: 진행 중인 시도 재개 - 기존 답안 유지")
        void resumeExistingAttempt_Success() {
            // Given: 시도 시작
            Long courseId = testCourse.getId();
            Integer sectionNumber = testSection.getSectionNumber();
            Long userId = testUser.getId();

            SectionAttemptResponse firstResponse = quizSectionAttemptService
                    .startOrResumeAttempt(courseId, sectionNumber, userId);
            Long attemptId = firstResponse.attemptId();

            // 답안 저장
            Long questionId = firstResponse.questions().get(0).questionId();
            SaveAnswerRequest request = new SaveAnswerRequest(
                    new SaveAnswerRequest.AnswerItem(questionId, "1")
            );
            quizSectionAttemptService.saveAnswer(attemptId, request, userId);

            entityManager.flush();
            entityManager.clear();

            // When: 같은 섹션에 다시 시도
            SectionAttemptResponse resumeResponse = quizSectionAttemptService
                    .startOrResumeAttempt(courseId, sectionNumber, userId);

            // Then: 기존 시도가 반환됨
            assertThat(resumeResponse.attemptId()).isEqualTo(attemptId);
            assertThat(resumeResponse.answeredCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공: Self-healing - 여러 IN_PROGRESS 시도 중 답변이 가장 많은 시도 선택 및 나머지 정리")
        void multipleInProgressAttempts_IntegratedSelfHealing() {
            // 1. Given: 답변 수가 다른 3개의 시도를 직접 DB에 삽입
            Long courseId = testCourse.getId();
            Integer sectionNumber = testSection.getSectionNumber();
            Long userId = testUser.getId();

            // 시도 1: 답변 3개 (가장 높은 진행도 - 선택되어야 함)
            UserSectionAttempt bestAttempt = UserSectionAttempt.builder()
                    .user(testUser).section(testSection).totalQuestions(10).build();
            attemptRepository.save(bestAttempt);
            for (int i = 0; i < 3; i++) {
                UserSectionAttemptQuestion aq = UserSectionAttemptQuestion.builder()
                        .attempt(bestAttempt).question(testQuestions.get(i)).orderIndex(i + 1).build();
                aq.saveAnswer("answer" + i); // 답변 저장
                attemptQuestionRepository.save(aq);
            }

            // 시도 2: 답변 1개 (중복 - 포기 대상)
            UserSectionAttempt redundantAttempt1 = UserSectionAttempt.builder()
                    .user(testUser).section(testSection).totalQuestions(10).build();
            attemptRepository.save(redundantAttempt1);
            UserSectionAttemptQuestion aqRedundant = UserSectionAttemptQuestion.builder()
                    .attempt(redundantAttempt1).question(testQuestions.get(0)).orderIndex(1).build();
            aqRedundant.saveAnswer("answer");
            attemptQuestionRepository.save(aqRedundant);

            // 시도 3: 답변 0개 (중복 - 포기 대상)
            UserSectionAttempt redundantAttempt2 = UserSectionAttempt.builder()
                    .user(testUser).section(testSection).totalQuestions(10).build();
            attemptRepository.save(redundantAttempt2);

            entityManager.flush();
            entityManager.clear();

            // ID 추출 (ID가 동적으로 생성되므로 변수에 할당)
            Long expectedId = bestAttempt.getId();
            Long id1 = redundantAttempt1.getId();
            Long id2 = redundantAttempt2.getId();

            // 2. When: 서비스 호출 (셀프 힐링 트리거)
            SectionAttemptResponse response = quizSectionAttemptService
                    .startOrResumeAttempt(courseId, sectionNumber, userId);

            // 3. Then: 답변 3개인 시도가 반환되었는지 확인
            assertThat(response.attemptId()).isEqualTo(expectedId);

            // 4. DB 상태 검증: 나머지 시도들이 실제로 ABANDONED가 되었는지 확인
            UserSectionAttempt dbRedundant1 = attemptRepository.findById(id1).orElseThrow();
            UserSectionAttempt dbRedundant2 = attemptRepository.findById(id2).orElseThrow();
            assertThat(dbRedundant1.getStatus()).isEqualTo(AttemptStatus.ABANDONED);
            assertThat(dbRedundant2.getStatus()).isEqualTo(AttemptStatus.ABANDONED);
        }

        @Test
        @DisplayName("성공: 답안 저장 시 @Version 컬럼 값 증가 확인")
        void saveAnswer_IncrementsVersion() {
            // 1. Given: 초기 시도 생성
            SectionAttemptResponse resp = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(), testUser.getId());
            Long attemptId = resp.attemptId();
            Long questionId = resp.questions().get(0).questionId();

            entityManager.flush();
            entityManager.clear();

            // 초기 버전 확인 (0)
            UserSectionAttemptQuestion initialAq = attemptQuestionRepository
                    .findByAttemptIdAndQuestionId(attemptId, questionId).orElseThrow();
            Long initialVersion = initialAq.getVersion();

            // 2. When: 답안 저장
            SaveAnswerRequest request = new SaveAnswerRequest(
                    new SaveAnswerRequest.AnswerItem(questionId, "New Answer")
            );
            quizSectionAttemptService.saveAnswer(attemptId, request, testUser.getId());

            entityManager.flush();
            entityManager.clear(); // 영속성 컨텍스트 비우기

            // 3. Then: 버전이 증가했는지 확인
            UserSectionAttemptQuestion updatedAq = attemptQuestionRepository
                    .findByAttemptIdAndQuestionId(attemptId, questionId).orElseThrow();
            assertThat(updatedAq.getVersion()).isEqualTo(initialVersion + 1);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 섹션 - NotFoundException 발생")
        void sectionNotFound_ThrowsException() {
            // Given: 존재하지 않는 courseId/sectionNumber
            Long invalidCourseId = 999999L;
            Integer invalidSectionNumber = 999;
            Long userId = testUser.getId();

            // When & Then
            assertThatThrownBy(() -> quizSectionAttemptService
                    .startOrResumeAttempt(invalidCourseId, invalidSectionNumber, userId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("실패: 비활성화된 코스 - NotFoundException 발생")
        void inactiveCourse_ThrowsException() {
            // Given: 비활성화된 코스 생성
            QuizCourse inactiveCourse = QuizCourse.builder()
                    .code("INACTIVE_COURSE")
                    .name("Inactive Course")
                    .description("비활성 코스")
                    .isActive(false)
                    .totalSections(1)
                    .sortOrder(99)
                    .build();
            inactiveCourse = courseRepository.saveAndFlush(inactiveCourse);
            entityManager.flush();

            QuizCourseSection inactiveSection = QuizCourseSection.create(
                    inactiveCourse, 1, "Inactive Section", "비활성 섹션", 70
            );
            inactiveSection = sectionRepository.saveAndFlush(inactiveSection);
            entityManager.flush();

            Long courseId = inactiveCourse.getId();
            Integer sectionNumber = inactiveSection.getSectionNumber();
            Long userId = testUser.getId();

            // When & Then
            assertThatThrownBy(() -> quizSectionAttemptService
                    .startOrResumeAttempt(courseId, sectionNumber, userId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("실패: 잠긴 섹션 (이전 섹션 미완료) - SectionLockedException 발생")
        void lockedSection_ThrowsException() {
            // Given: 섹션 3 생성 (섹션 1만 존재하고 통과하지 않은 상태)
            QuizCourseSection section3 = QuizCourseSection.create(
                    testCourse, 3, "Section 3", "섹션 3", 70
            );
            section3 = sectionRepository.saveAndFlush(section3);
            entityManager.flush();

            // 진행 상황: 섹션 1까지만 해금
            UserCourseProgress progress = UserCourseProgress.builder()
                    .userId(testUser.getId())
                    .courseId(testCourse.getId())
                    .currentSection(1)
                    .completedSections(0)
                    .isCompleted(false)
                    .build();
            progressRepository.saveAndFlush(progress);
            entityManager.flush();

            Long courseId = testCourse.getId();
            Integer sectionNumber = section3.getSectionNumber();
            Long userId = testUser.getId();

            // When & Then
            assertThatThrownBy(() -> quizSectionAttemptService
                    .startOrResumeAttempt(courseId, sectionNumber, userId))
                    .isInstanceOf(SectionLockedException.class);
        }
    }

    // ==========================================================
    // saveAnswer() 테스트
    // ==========================================================

    @Nested
    @DisplayName("saveAnswer 메서드 (단일 답안 실시간 저장)")
    class SaveAnswerTest {

        @Test
        @DisplayName("성공: 단일 답안 정상 저장")
        void saveAnswer_Success() {
            // Given: 시도 시작
            Long courseId = testCourse.getId();
            Integer sectionNumber = testSection.getSectionNumber();
            Long userId = testUser.getId();

            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(courseId, sectionNumber, userId);
            Long attemptId = attemptResponse.attemptId();
            Long questionId = attemptResponse.questions().get(0).questionId();

            // When
            SaveAnswerRequest request = new SaveAnswerRequest(
                    new SaveAnswerRequest.AnswerItem(questionId, "1")
            );
            quizSectionAttemptService.saveAnswer(attemptId, request, userId);

            entityManager.flush();
            entityManager.clear();

            // Then: 답안이 저장되었는지 확인
            SectionAttemptResponse refreshed = quizSectionAttemptService
                    .startOrResumeAttempt(courseId, sectionNumber, userId);
            assertThat(refreshed.answeredCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공: 동일 문제 답안 덮어쓰기 (멱등성 보장)")
        void saveAnswer_Idempotent() {
            // Given: 시도 시작 및 첫 답안 저장
            Long courseId = testCourse.getId();
            Integer sectionNumber = testSection.getSectionNumber();
            Long userId = testUser.getId();

            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(courseId, sectionNumber, userId);
            Long attemptId = attemptResponse.attemptId();
            Long questionId = attemptResponse.questions().get(0).questionId();

            // 첫 번째 답안 저장
            quizSectionAttemptService.saveAnswer(attemptId,
                    new SaveAnswerRequest(new SaveAnswerRequest.AnswerItem(questionId, "OLD")),
                    userId);

            // When: 같은 문제에 새로운 답안 저장
            quizSectionAttemptService.saveAnswer(attemptId,
                    new SaveAnswerRequest(new SaveAnswerRequest.AnswerItem(questionId, "NEW")),
                    userId);

            entityManager.flush();
            entityManager.clear();

            // Then: 새로운 답안으로 덮어쓰기됨 (제출 시 확인)
            AttemptResultResponse result = quizSectionAttemptService
                    .submitAttempt(attemptId, userId);

            // 정답이 "1"이므로 "NEW"는 오답
            assertThat(result.results().get(0).userAnswer()).isEqualTo("NEW");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 시도 - BusinessException(QUESTION_NOT_FOUND) 발생")
        void attemptNotFound_ThrowsException() {
            // Given
            Long invalidAttemptId = 999999L;
            Long userId = testUser.getId();
            SaveAnswerRequest request = new SaveAnswerRequest(
                    new SaveAnswerRequest.AnswerItem(1L, "1")
            );

            // When & Then: NotFoundException 대신 BusinessException을 기대함
            assertThatThrownBy(() -> quizSectionAttemptService
                    .saveAnswer(invalidAttemptId, request, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("해당 문제를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("실패: 다른 사용자의 시도 - BusinessException(FORBIDDEN) 발생")
        void notOwner_ThrowsException() {
            // Given: 다른 사용자 생성
            User otherUser = createOtherUser();
            entityManager.flush();

            // testUser로 시도 시작
            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(),
                            testUser.getId());
            Long attemptId = attemptResponse.attemptId();
            Long questionId = attemptResponse.questions().get(0).questionId();

            // When & Then: otherUser로 답안 저장 시도
            SaveAnswerRequest request = new SaveAnswerRequest(
                    new SaveAnswerRequest.AnswerItem(questionId, "1")
            );

            assertThatThrownBy(() -> quizSectionAttemptService
                    .saveAnswer(attemptId, request, otherUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("본인의 시도만 수정할 수 있습니다");
        }

        @Test
        @DisplayName("실패: 완료된 시도 수정 불가 - BusinessException(BAD_REQUEST) 발생")
        void completedAttempt_ThrowsException() {
            // Given: 시도 시작 후 제출(완료)
            Long userId = testUser.getId();
            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(), userId);
            Long attemptId = attemptResponse.attemptId();
            Long questionId = attemptResponse.questions().get(0).questionId();

            // 제출하여 완료 처리
            quizSectionAttemptService.submitAttempt(attemptId, userId);
            entityManager.flush();
            entityManager.clear();

            // When & Then: 완료된 시도에 답안 저장 시도
            SaveAnswerRequest request = new SaveAnswerRequest(
                    new SaveAnswerRequest.AnswerItem(questionId, "1")
            );

            assertThatThrownBy(() -> quizSectionAttemptService
                    .saveAnswer(attemptId, request, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("완료된 시도는 수정할 수 없습니다");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 questionId - BusinessException(QUESTION_NOT_FOUND) 발생")
        void questionNotFound_ThrowsException() {
            // Given
            Long userId = testUser.getId();
            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(), userId);
            Long attemptId = attemptResponse.attemptId();

            // When: 존재하지 않는 questionId로 답안 저장 시도
            SaveAnswerRequest request = new SaveAnswerRequest(
                    new SaveAnswerRequest.AnswerItem(999999L, "1")
            );

            // Then: 이제 무시되지 않고 BusinessException이 발생해야 함
            assertThatThrownBy(() -> quizSectionAttemptService
                    .saveAnswer(attemptId, request, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("해당 문제를 찾을 수 없습니다");
        }
    }

    // ==========================================================
    // submitAttempt() 테스트
    // ==========================================================

    @Nested
    @DisplayName("submitAttempt 메서드")
    class SubmitAttemptTest {

        @Test
        @DisplayName("성공: 시험 통과 - 다음 섹션 해금")
        void submitAndPass_UnlocksNextSection() {
            // Given: 시도 시작
            Long userId = testUser.getId();
            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(), userId);
            Long attemptId = attemptResponse.attemptId();

            // 8개 문제에 정답 제출 (80% 통과)
            for (int i = 0; i < 8; i++) {
                Long questionId = attemptResponse.questions().get(i).questionId();
                quizSectionAttemptService.saveAnswer(attemptId,
                        new SaveAnswerRequest(new SaveAnswerRequest.AnswerItem(questionId, "1")),
                        userId);
            }

            entityManager.flush();
            entityManager.clear();

            // When
            AttemptResultResponse result = quizSectionAttemptService
                    .submitAttempt(attemptId, userId);

            // Then
            assertThat(result.isPassed()).isTrue();
            assertThat(result.score()).isEqualTo(80);
            assertThat(result.correctCount()).isEqualTo(8);
            assertThat(result.totalQuestions()).isEqualTo(10);
            assertThat(result.isNextSectionUnlocked()).isTrue();
        }

        @Test
        @DisplayName("성공: 시험 불합격 - 다음 섹션 미해금")
        void submitAndFail_DoesNotUnlockNextSection() {
            // Given: 시도 시작
            Long userId = testUser.getId();
            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(), userId);
            Long attemptId = attemptResponse.attemptId();

            // 5개 문제에 정답 제출 (50% 불합격)
            for (int i = 0; i < 5; i++) {
                Long questionId = attemptResponse.questions().get(i).questionId();
                quizSectionAttemptService.saveAnswer(attemptId,
                        new SaveAnswerRequest(new SaveAnswerRequest.AnswerItem(questionId, "1")),
                        userId);
            }

            // 나머지 5개에 오답 제출
            for (int i = 5; i < 10; i++) {
                Long questionId = attemptResponse.questions().get(i).questionId();
                quizSectionAttemptService.saveAnswer(attemptId,
                        new SaveAnswerRequest(new SaveAnswerRequest.AnswerItem(questionId, "WRONG")),
                        userId);
            }

            entityManager.flush();
            entityManager.clear();

            // When
            AttemptResultResponse result = quizSectionAttemptService
                    .submitAttempt(attemptId, userId);

            // Then
            assertThat(result.isPassed()).isFalse();
            assertThat(result.score()).isEqualTo(50);
            assertThat(result.correctCount()).isEqualTo(5);
            assertThat(result.isNextSectionUnlocked()).isFalse();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 시도 제출 - NotFoundException 발생")
        void submitNonExistentAttempt_ThrowsException() {
            // Given
            Long invalidAttemptId = 999999L;
            Long userId = testUser.getId();

            // When & Then
            assertThatThrownBy(() -> quizSectionAttemptService
                    .submitAttempt(invalidAttemptId, userId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 시도 제출 - BusinessException(FORBIDDEN) 발생")
        void submitOthersAttempt_ThrowsException() {
            // Given
            User otherUser = createOtherUser();
            entityManager.flush();

            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(),
                            testUser.getId());
            Long attemptId = attemptResponse.attemptId();

            // When & Then
            assertThatThrownBy(() -> quizSectionAttemptService
                    .submitAttempt(attemptId, otherUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("본인의 시도만 제출할 수 있습니다");
        }

        @Test
        @DisplayName("실패: 이미 완료된 시도 재제출 - BusinessException(BAD_REQUEST) 발생")
        void submitCompletedAttempt_ThrowsException() {
            // Given
            Long userId = testUser.getId();
            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(), userId);
            Long attemptId = attemptResponse.attemptId();

            // 첫 번째 제출
            quizSectionAttemptService.submitAttempt(attemptId, userId);
            entityManager.flush();
            entityManager.clear();

            // When & Then: 재제출 시도
            assertThatThrownBy(() -> quizSectionAttemptService
                    .submitAttempt(attemptId, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 완료된 시도입니다");
        }
    }

    // ==========================================================
    // abandonAttempt() 테스트
    // ==========================================================

    @Nested
    @DisplayName("abandonAttempt 메서드")
    class AbandonAttemptTest {

        @Test
        @DisplayName("성공: 진행 중인 시도 포기")
        void abandonAttempt_Success() {
            // Given
            Long userId = testUser.getId();
            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(), userId);
            Long attemptId = attemptResponse.attemptId();

            // When
            quizSectionAttemptService.abandonAttempt(attemptId, userId);

            entityManager.flush();
            entityManager.clear();

            // Then: 포기 후 새로운 시도 시작 가능
            SectionAttemptResponse newAttempt = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(), userId);

            assertThat(newAttempt.attemptId()).isNotEqualTo(attemptId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 시도 포기 - NotFoundException 발생")
        void abandonNonExistentAttempt_ThrowsException() {
            // Given
            Long invalidAttemptId = 999999L;
            Long userId = testUser.getId();

            // When & Then
            assertThatThrownBy(() -> quizSectionAttemptService
                    .abandonAttempt(invalidAttemptId, userId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 시도 포기 - BusinessException(FORBIDDEN) 발생")
        void abandonOthersAttempt_ThrowsException() {
            // Given
            User otherUser = createOtherUser();
            entityManager.flush();

            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(),
                            testUser.getId());
            Long attemptId = attemptResponse.attemptId();

            // When & Then
            assertThatThrownBy(() -> quizSectionAttemptService
                    .abandonAttempt(attemptId, otherUser.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("본인의 시도만 포기할 수 있습니다");
        }

        @Test
        @DisplayName("실패: 완료된 시도 포기 불가 - BusinessException(BAD_REQUEST) 발생")
        void abandonCompletedAttempt_ThrowsException() {
            // Given
            Long userId = testUser.getId();
            SectionAttemptResponse attemptResponse = quizSectionAttemptService
                    .startOrResumeAttempt(testCourse.getId(), testSection.getSectionNumber(), userId);
            Long attemptId = attemptResponse.attemptId();

            quizSectionAttemptService.submitAttempt(attemptId, userId);
            entityManager.flush();
            entityManager.clear();

            // When & Then
            assertThatThrownBy(() -> quizSectionAttemptService
                    .abandonAttempt(attemptId, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 완료된 시도입니다");
        }
    }
}
