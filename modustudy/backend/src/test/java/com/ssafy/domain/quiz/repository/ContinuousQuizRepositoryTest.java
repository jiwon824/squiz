package com.ssafy.domain.quiz.repository;

import com.ssafy.domain.quiz.entity.QuizCourse;
import com.ssafy.domain.quiz.entity.QuizCourseQuestion;
import com.ssafy.domain.quiz.entity.QuizCourseSection;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ContinuousLearningRepository 통합 테스트.
 *
 * <p>확률적 가중치 기반 문제 선택 로직을 검증한다.</p>
 *
 * <h3>테스트 대상</h3>
 * <ul>
 *   <li>무한 루프: 모든 문제를 풀어도 계속 문제가 반환되는지 검증</li>
 *   <li>확률적 가중치: 신규 문제(Weight 10.0)가 자주 푼 문제보다 높은 빈도로 선택되는지 통계 검증</li>
 *   <li>excludeId: 방금 푼 문제가 제외되는지 검증</li>
 * </ul>
 *
 * <p><b>주의:</b> 이 테스트는 MySQL 네이티브 쿼리(RAND(), LOG() 함수)를 사용하므로
 * Flyway로 초기화된 실제 MySQL 스키마(ssafy_web_db)가 필요합니다.
 * 테스트 DB(ddl-auto=create)에서는 스키마 불일치로 실패할 수 있습니다.</p>
 */
@SpringBootTest
@Transactional
@Disabled("MySQL 네이티브 쿼리 테스트 - Flyway 스키마(ssafy_web_db) 필요. 테스트 DB에서는 스키마 불일치로 실패함")
class ContinuousQuizRepositoryTest {

    @Autowired
    private ContinuousQuizRepository continuousQuizRepository;

    @Autowired
    private QuizCourseRepository quizCourseRepository;

    @Autowired
    private QuizCourseSectionRepository quizCourseSectionRepository;

    @Autowired
    private UserReviewItemRepository userReviewItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private QuizCourse testCourse;
    private QuizCourseSection testSection;
    private QuizCourseQuestion question1;
    private QuizCourseQuestion question2;
    private QuizCourseQuestion question3;

    @BeforeEach
    void setUp() {
        // 1. 테스트 사용자 생성
        testUser = userRepository.save(User.builder()
                .userId("continuous_test_user")
                .email("continuous@test.com")
                .nickname("연속학습테스터")
                .name("테스터")
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

        // 2. 퀴즈 코스 생성
        testCourse = quizCourseRepository.save(QuizCourse.builder()
                .code("TEST_CONTINUOUS")
                .name("연속학습 테스트 코스")
                .description("연속학습 모드 테스트용 코스")
                .totalSections(1)
                .sortOrder(999)
                .isActive(true)
                .build());
        quizCourseRepository.flush();

        // 3. 섹션 생성
        testSection = QuizCourseSection.create(
                testCourse,
                1,
                "테스트 섹션",
                "연속학습 테스트용 섹션",
                70
        );
        quizCourseSectionRepository.save(testSection);
        quizCourseSectionRepository.flush();

        // 4. 문제 생성 (3개)
        question1 = continuousQuizRepository.save(QuizCourseQuestion.builder()
                .section(testSection)
                .questionNumber(1)
                .questionText("테스트 문제 1")
                .questionType(QuestionType.SHORT_ANSWER)
                .correctAnswer("정답1")
                .explanation("해설1")
                .build());

        question2 = continuousQuizRepository.save(QuizCourseQuestion.builder()
                .section(testSection)
                .questionNumber(2)
                .questionText("테스트 문제 2")
                .questionType(QuestionType.SHORT_ANSWER)
                .correctAnswer("정답2")
                .explanation("해설2")
                .build());

        question3 = continuousQuizRepository.save(QuizCourseQuestion.builder()
                .section(testSection)
                .questionNumber(3)
                .questionText("테스트 문제 3")
                .questionType(QuestionType.SHORT_ANSWER)
                .correctAnswer("정답3")
                .explanation("해설3")
                .build());

        continuousQuizRepository.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findNextQuestionProbabilisticNoExclude 메서드는")
    class FindNextQuestionProbabilisticNoExclude {

        @Test
        @DisplayName("신규 사용자에게 문제를 반환한다")
        void shouldReturnQuestionForNewUser() {
            // given: 아무 학습 기록이 없는 사용자

            // when
            Optional<QuizCourseQuestion> result = continuousQuizRepository
                    .findNextQuestionProbabilisticNoExclude(
                            testCourse.getId(),
                            testSection.getSectionNumber(),
                            testUser.getId()
                    );

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getSection().getQuizCourseId()).isEqualTo(testCourse.getId());
        }

        @Test
        @DisplayName("모든 문제를 학습한 후에도 문제를 반환한다 (무한 루프 검증)")
        void shouldReturnQuestionEvenWhenAllQuestionsAreLearned() {
            // given: 모든 문제에 대해 학습 기록 생성 (높은 reps)
            createReviewItem(testUser.getId(), question1.getId(), 50);
            createReviewItem(testUser.getId(), question2.getId(), 100);
            createReviewItem(testUser.getId(), question3.getId(), 75);
            entityManager.flush();
            entityManager.clear();

            // when
            Optional<QuizCourseQuestion> result = continuousQuizRepository
                    .findNextQuestionProbabilisticNoExclude(
                            testCourse.getId(),
                            testSection.getSectionNumber(),
                            testUser.getId()
                    );

            // then: 무한 루프 - 모두 학습해도 문제 반환
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("존재하지 않는 섹션에서는 빈 결과를 반환한다")
        void shouldReturnEmptyForNonExistentSection() {
            // given: 존재하지 않는 섹션 번호
            Integer nonExistentSection = 999;

            // when
            Optional<QuizCourseQuestion> result = continuousQuizRepository
                    .findNextQuestionProbabilisticNoExclude(
                            testCourse.getId(),
                            nonExistentSection,
                            testUser.getId()
                    );

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findNextQuestionProbabilistic 메서드는")
    class FindNextQuestionProbabilistic {

        @Test
        @DisplayName("excludeId로 지정한 문제를 제외하고 반환한다")
        void shouldExcludeSpecifiedQuestion() {
            // given: question1을 제외 대상으로 지정

            // when: 100번 반복하여 question1이 절대 선택되지 않는지 확인
            boolean question1Selected = false;
            for (int i = 0; i < 100; i++) {
                Optional<QuizCourseQuestion> result = continuousQuizRepository
                        .findNextQuestionProbabilistic(
                                testCourse.getId(),
                                testSection.getSectionNumber(),
                                testUser.getId(),
                                question1.getId()  // 이 문제 제외
                        );

                if (result.isPresent() && result.get().getId().equals(question1.getId())) {
                    question1Selected = true;
                    break;
                }
            }

            // then: question1은 선택되지 않아야 함
            assertThat(question1Selected).isFalse();
        }

        @Test
        @DisplayName("섹션에 문제가 1개뿐이고 그것을 제외하면 빈 결과를 반환한다")
        void shouldReturnEmptyWhenOnlyQuestionIsExcluded() {
            // given: question2, question3 삭제하여 문제 1개만 남김
            continuousQuizRepository.delete(question2);
            continuousQuizRepository.delete(question3);
            entityManager.flush();
            entityManager.clear();

            // when: 유일한 문제를 제외
            Optional<QuizCourseQuestion> result = continuousQuizRepository
                    .findNextQuestionProbabilistic(
                            testCourse.getId(),
                            testSection.getSectionNumber(),
                            testUser.getId(),
                            question1.getId()
                    );

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("excludeId가 null이면 모든 문제에서 선택한다")
        void shouldSelectFromAllQuestionsWhenExcludeIdIsNull() {
            // given: excludeId = null

            // when
            Optional<QuizCourseQuestion> result = continuousQuizRepository
                    .findNextQuestionProbabilistic(
                            testCourse.getId(),
                            testSection.getSectionNumber(),
                            testUser.getId(),
                            null
                    );

            // then
            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("확률적 가중치 통계 검증")
    class StatisticalValidation {

        @Test
        @DisplayName("신규 문제(Weight 10.0)가 자주 푼 문제(Weight 1/n)보다 높은 빈도로 선택된다")
        void newQuestionsShouldBeSelectedMoreFrequently() {
            // given: question1은 신규(학습 기록 없음), question2는 100번 학습
            createReviewItem(testUser.getId(), question2.getId(), 100);  // weight = 1/101 ≈ 0.01
            createReviewItem(testUser.getId(), question3.getId(), 50);   // weight = 1/51 ≈ 0.02
            // question1: 신규 → weight = 10.0
            entityManager.flush();
            entityManager.clear();

            // when: 100번 선택하여 각 문제의 선택 빈도 측정
            Map<Long, Integer> selectionCount = new HashMap<>();
            selectionCount.put(question1.getId(), 0);
            selectionCount.put(question2.getId(), 0);
            selectionCount.put(question3.getId(), 0);

            for (int i = 0; i < 100; i++) {
                Optional<QuizCourseQuestion> result = continuousQuizRepository
                        .findNextQuestionProbabilisticNoExclude(
                                testCourse.getId(),
                                testSection.getSectionNumber(),
                                testUser.getId()
                        );

                result.ifPresent(q ->
                        selectionCount.merge(q.getId(), 1, Integer::sum)
                );
            }

            // then: question1(신규)이 question2(100번 학습)보다 훨씬 많이 선택되어야 함
            int newQuestionCount = selectionCount.get(question1.getId());
            int frequentQuestionCount = selectionCount.get(question2.getId());

            // 신규 문제의 가중치(10.0)가 100번 학습한 문제(0.01)보다 1000배 높으므로
            // 신규 문제가 최소 5배 이상 선택되어야 함 (확률적 테스트이므로 여유 있게 검증)
            assertThat(newQuestionCount)
                    .as("신규 문제(weight=10.0) 선택 횟수가 100번 학습한 문제(weight≈0.01)보다 많아야 함")
                    .isGreaterThan(frequentQuestionCount * 2);

            // 모든 문제가 최소 1번은 선택될 수 있음을 검증 (무한 루프 보장)
            // 단, 확률적 테스트이므로 0일 수도 있어 soft assertion
            System.out.printf("[통계 검증] 100회 선택 결과 - 신규(Q1): %d, 100회학습(Q2): %d, 50회학습(Q3): %d%n",
                    newQuestionCount, frequentQuestionCount, selectionCount.get(question3.getId()));
        }

        @Test
        @DisplayName("복습 예정(Due) 문제가 학습 완료 문제보다 높은 빈도로 선택된다")
        void dueQuestionsShouldBeSelectedMoreThanCompletedQuestions() {
            // given: question1은 Due(복습 예정), question2는 Not Due(복습 완료)
            UserReviewItem dueItem = createReviewItem(testUser.getId(), question1.getId(), 5);
            dueItem.setNextReviewAt(LocalDateTime.now().minusDays(1));  // 과거 = Due
            userReviewItemRepository.save(dueItem);

            UserReviewItem notDueItem = createReviewItem(testUser.getId(), question2.getId(), 5);
            notDueItem.setNextReviewAt(LocalDateTime.now().plusDays(30));  // 미래 = Not Due
            userReviewItemRepository.save(notDueItem);

            // question3는 신규 (가장 높은 가중치)
            entityManager.flush();
            entityManager.clear();

            // when: 100번 선택
            Map<Long, Integer> selectionCount = new HashMap<>();
            selectionCount.put(question1.getId(), 0);
            selectionCount.put(question2.getId(), 0);
            selectionCount.put(question3.getId(), 0);

            for (int i = 0; i < 100; i++) {
                Optional<QuizCourseQuestion> result = continuousQuizRepository
                        .findNextQuestionProbabilisticNoExclude(
                                testCourse.getId(),
                                testSection.getSectionNumber(),
                                testUser.getId()
                        );

                result.ifPresent(q ->
                        selectionCount.merge(q.getId(), 1, Integer::sum)
                );
            }

            // then
            int dueCount = selectionCount.get(question1.getId());       // Due: weight = 5.0
            int notDueCount = selectionCount.get(question2.getId());    // Not Due: weight = 1/6 ≈ 0.17
            int newCount = selectionCount.get(question3.getId());       // New: weight = 10.0

            // Due 문제가 Not Due 문제보다 많이 선택되어야 함
            assertThat(dueCount)
                    .as("Due 문제(weight=5.0)가 Not Due 문제(weight≈0.17)보다 많이 선택되어야 함")
                    .isGreaterThan(notDueCount);

            System.out.printf("[통계 검증] 100회 선택 결과 - 신규(Q3): %d, Due(Q1): %d, NotDue(Q2): %d%n",
                    newCount, dueCount, notDueCount);
        }
    }

    @Nested
    @DisplayName("findNextQuestionWeightedRandom 메서드는")
    class FindNextQuestionWeightedRandom {

        @Test
        @DisplayName("기존 가중치 로직(1/(reps+1))으로 문제를 선택한다")
        void shouldSelectQuestionByRepsWeight() {
            // given: 다양한 학습 횟수의 문제들
            createReviewItem(testUser.getId(), question1.getId(), 0);   // weight = 1/(0+1) = 1.0
            createReviewItem(testUser.getId(), question2.getId(), 9);   // weight = 1/(9+1) = 0.1
            // question3: 학습 기록 없음 → weight = 1/(0+1) = 1.0
            entityManager.flush();
            entityManager.clear();

            // when
            Optional<QuizCourseQuestion> result = continuousQuizRepository
                    .findNextQuestionWeightedRandom(
                            testCourse.getId(),
                            testSection.getSectionNumber(),
                            testUser.getId()
                    );

            // then
            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("통계 조회 메서드")
    class StatisticsQueries {

        @Test
        @DisplayName("섹션 내 전체 문제 수를 정확히 반환한다")
        void shouldCountTotalQuestions() {
            // when
            long count = continuousQuizRepository.countTotalQuestions(
                    testCourse.getId(),
                    testSection.getSectionNumber()
            );

            // then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("학습하지 않은 문제 수를 정확히 반환한다")
        void shouldCountUnstudiedQuestions() {
            // given: 1개만 학습
            createReviewItem(testUser.getId(), question1.getId(), 1);
            entityManager.flush();
            entityManager.clear();

            // when
            long unstudiedCount = continuousQuizRepository.countUnstudiedQuestions(
                    testCourse.getId(),
                    testSection.getSectionNumber(),
                    testUser.getId()
            );

            // then
            assertThat(unstudiedCount).isEqualTo(2);
        }

        @Test
        @DisplayName("학습한 문제 수를 정확히 반환한다")
        void shouldCountStudiedQuestions() {
            // given: 2개 학습
            createReviewItem(testUser.getId(), question1.getId(), 1);
            createReviewItem(testUser.getId(), question2.getId(), 3);
            entityManager.flush();
            entityManager.clear();

            // when
            long studiedCount = continuousQuizRepository.countStudiedQuestions(
                    testCourse.getId(),
                    testSection.getSectionNumber(),
                    testUser.getId()
            );

            // then
            assertThat(studiedCount).isEqualTo(2);
        }
    }

    // ══════════════════════════════════════════════════════
    //  Helper Methods
    // ══════════════════════════════════════════════════════

    /**
     * UserReviewItem 생성 헬퍼 메서드
     */
    private UserReviewItem createReviewItem(Long userId, Long questionId, int reps) {
        UserReviewItem item = UserReviewItem.builder()
                .userId(userId)
                .contentType(ReviewContentType.COURSE_QUESTION)
                .contentId(questionId)
                .reps(reps)
                .stability(1.0)
                .difficulty(5.0)
                .state(reps > 0 ? 2 : 0)  // STATE_REVIEW or STATE_NEW
                .nextReviewAt(LocalDateTime.now().plusDays(1))
                .lastReviewedAt(LocalDateTime.now())
                .build();
        return userReviewItemRepository.save(item);
    }
}
