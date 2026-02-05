package com.ssafy.domain.quiz.service;

import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.UserReviewLog;
import com.ssafy.domain.quiz.repository.UserReviewItemRepository;
import com.ssafy.domain.quiz.repository.UserReviewLogRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.quiz.dto.response.OptionItem;
import com.ssafy.domain.quiz.dto.response.ReviewCourseStatsResponse;
import com.ssafy.domain.quiz.dto.response.ReviewCourseStatsResponse.CourseStatDto;
import com.ssafy.domain.quiz.dto.response.ReviewCourseWeaknessResponse;
import com.ssafy.domain.quiz.dto.response.ReviewCourseWeaknessResponse.CourseWeaknessStatDto;
import com.ssafy.domain.quiz.dto.response.ReviewResult;
import com.ssafy.domain.quiz.dto.response.TodayReviewResponse.ReviewItemDto;
import com.ssafy.domain.quiz.entity.*;
import com.ssafy.domain.quiz.entity.enums.QuestionType;
import com.ssafy.domain.quiz.repository.ContinuousQuizRepository;
import com.ssafy.domain.quiz.repository.CourseQuestionStatsProjection;
import com.ssafy.domain.quiz.repository.QuizCourseRepository;
import com.ssafy.domain.quiz.repository.StudyQuizQuestionRepository;
import com.ssafy.domain.quiz.dto.response.ReviewStatsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * FsrsService 단위 테스트.
 *
 * <p>
 * FSRS v14 알고리즘 기반 간격 반복 복습 로직을 검증한다.
 * </p>
 *
 * <h3>테스트 대상</h3>
 * <ul>
 * <li>calculateRating: 정답 여부와 응답 시간 기반 자동 Rating 산출</li>
 * <li>updateFsrsState: 신규 카드 초기화 및 기존 카드 상태 갱신</li>
 * <li>processReview: 복습 처리 (Upsert + Log)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class FsrsServiceTest {

        @InjectMocks
        private FsrsService fsrsService;

        @Mock
        private UserReviewItemRepository reviewItemRepository;

        @Mock
        private UserReviewLogRepository reviewLogRepository;

        @Mock
        private ContinuousQuizRepository continuousQuizRepository;

        @Mock
        private ObjectMapper objectMapper;

        @Mock
        private QuizCourseRepository quizCourseRepository;

        @Mock
        private StudyQuizQuestionRepository studyQuizQuestionRepository;

        private static final Long TEST_USER_ID = 1L;
        private static final Long TEST_CONTENT_ID = 100L;

        // ══════════════════════════════════════════════════════
        // calculateRating 테스트
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("calculateRating 메서드는")
        class CalculateRating {

                @Test
                @DisplayName("오답 시 항상 Again(1)을 반환한다")
                void shouldReturnAgainForIncorrectAnswer() {
                        // given
                        boolean isCorrect = false;
                        long responseTimeMs = 1000L; // 빠른 응답이어도 오답이면 Again

                        // when
                        int rating = fsrsService.calculateRating(isCorrect, responseTimeMs);

                        // then
                        assertThat(rating).isEqualTo(FsrsConstants.RATING_AGAIN);
                }

                @Test
                @DisplayName("정답 & 2000ms 이하 → Easy(4)를 반환한다")
                void shouldReturnEasyForQuickCorrectAnswer() {
                        // given
                        boolean isCorrect = true;
                        long responseTimeMs = 1500L;

                        // when
                        int rating = fsrsService.calculateRating(isCorrect, responseTimeMs);

                        // then
                        assertThat(rating).isEqualTo(FsrsConstants.RATING_EASY);
                }

                @Test
                @DisplayName("정답 & 2000ms 경계값 → Easy(4)를 반환한다")
                void shouldReturnEasyForExactly2000ms() {
                        // given
                        boolean isCorrect = true;
                        long responseTimeMs = 2000L; // 경계값

                        // when
                        int rating = fsrsService.calculateRating(isCorrect, responseTimeMs);

                        // then
                        assertThat(rating).isEqualTo(FsrsConstants.RATING_EASY);
                }

                @Test
                @DisplayName("정답 & 2001ms~5000ms → Good(3)을 반환한다")
                void shouldReturnGoodForModerateCorrectAnswer() {
                        // given
                        boolean isCorrect = true;
                        long responseTimeMs = 3500L;

                        // when
                        int rating = fsrsService.calculateRating(isCorrect, responseTimeMs);

                        // then
                        assertThat(rating).isEqualTo(FsrsConstants.RATING_GOOD);
                }

                @Test
                @DisplayName("정답 & 5000ms 경계값 → Good(3)을 반환한다")
                void shouldReturnGoodForExactly5000ms() {
                        // given
                        boolean isCorrect = true;
                        long responseTimeMs = 5000L; // 경계값

                        // when
                        int rating = fsrsService.calculateRating(isCorrect, responseTimeMs);

                        // then
                        assertThat(rating).isEqualTo(FsrsConstants.RATING_GOOD);
                }

                @Test
                @DisplayName("정답 & 5001ms 이상 → Hard(2)를 반환한다")
                void shouldReturnHardForSlowCorrectAnswer() {
                        // given
                        boolean isCorrect = true;
                        long responseTimeMs = 6000L;

                        // when
                        int rating = fsrsService.calculateRating(isCorrect, responseTimeMs);

                        // then
                        assertThat(rating).isEqualTo(FsrsConstants.RATING_HARD);
                }

                @ParameterizedTest(name = "정답={0}, 응답시간={1}ms → Rating={2}")
                @CsvSource({
                                "false, 500, 1", // 오답 → Again
                                "false, 10000, 1", // 오답 → Again
                                "true, 500, 4", // 정답 & 빠름 → Easy
                                "true, 2000, 4", // 정답 & 경계 → Easy
                                "true, 2001, 3", // 정답 & 중간 → Good
                                "true, 5000, 3", // 정답 & 경계 → Good
                                "true, 5001, 2", // 정답 & 느림 → Hard
                                "true, 10000, 2" // 정답 & 매우 느림 → Hard
                })
                @DisplayName("다양한 입력에 대해 올바른 Rating을 반환한다")
                void shouldReturnCorrectRatingForVariousInputs(
                                boolean isCorrect, long responseTimeMs, int expectedRating) {
                        // when
                        int rating = fsrsService.calculateRating(isCorrect, responseTimeMs);

                        // then
                        assertThat(rating).isEqualTo(expectedRating);
                }
        }

        // ══════════════════════════════════════════════════════
        // updateFsrsState 테스트
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("updateFsrsState 메서드는")
        class UpdateFsrsState {

                @Test
                @DisplayName("신규 카드(state=0)에 Easy 평가 시 Review 상태로 전이한다")
                void shouldTransitionNewCardToReviewOnEasy() {
                        // given: 신규 카드
                        UserReviewItem newItem = UserReviewItem.builder()
                                        .userId(TEST_USER_ID)
                                        .contentType(ReviewContentType.COURSE_QUESTION)
                                        .contentId(TEST_CONTENT_ID)
                                        .state(FsrsConstants.STATE_NEW)
                                        .stability(0.0)
                                        .difficulty(5.0)
                                        .reps(0)
                                        .lapses(0)
                                        .build();

                        // when
                        fsrsService.updateFsrsState(newItem, FsrsConstants.RATING_EASY);

                        // then
                        assertThat(newItem.getState()).isEqualTo(FsrsConstants.STATE_REVIEW);
                        assertThat(newItem.getStability()).isGreaterThan(0); // 초기 안정성 설정됨
                        assertThat(newItem.getReps()).isEqualTo(1);
                        assertThat(newItem.getNextReviewAt()).isNotNull();
                }

                @Test
                @DisplayName("신규 카드(state=0)에 Again 평가 시 Learning 상태로 전이하고 lapses 증가")
                void shouldTransitionNewCardToLearningOnAgain() {
                        // given: 신규 카드
                        UserReviewItem newItem = UserReviewItem.builder()
                                        .userId(TEST_USER_ID)
                                        .contentType(ReviewContentType.COURSE_QUESTION)
                                        .contentId(TEST_CONTENT_ID)
                                        .state(FsrsConstants.STATE_NEW)
                                        .stability(0.0)
                                        .difficulty(5.0)
                                        .reps(0)
                                        .lapses(0)
                                        .build();

                        // when
                        fsrsService.updateFsrsState(newItem, FsrsConstants.RATING_AGAIN);

                        // then
                        assertThat(newItem.getState()).isEqualTo(FsrsConstants.STATE_LEARNING);
                        assertThat(newItem.getLapses()).isEqualTo(1);
                        assertThat(newItem.getReps()).isEqualTo(1);
                }

                @Test
                @DisplayName("기존 Review 상태 카드에 Good 평가 시 안정성이 증가한다")
                void shouldIncreaseStabilityOnGoodRating() {
                        // given: Review 상태의 기존 카드
                        UserReviewItem reviewItem = UserReviewItem.builder()
                                        .userId(TEST_USER_ID)
                                        .contentType(ReviewContentType.COURSE_QUESTION)
                                        .contentId(TEST_CONTENT_ID)
                                        .state(FsrsConstants.STATE_REVIEW)
                                        .stability(5.0)
                                        .difficulty(5.0)
                                        .reps(3)
                                        .lapses(0)
                                        .elapsedMinutes(3 * 1440)
                                        .lastReviewedAt(LocalDateTime.now().minusDays(3))
                                        .build();

                        double originalStability = reviewItem.getStability();

                        // when
                        fsrsService.updateFsrsState(reviewItem, FsrsConstants.RATING_GOOD);

                        // then
                        assertThat(reviewItem.getStability()).isGreaterThan(originalStability);
                        assertThat(reviewItem.getState()).isEqualTo(FsrsConstants.STATE_REVIEW);
                        assertThat(reviewItem.getReps()).isEqualTo(4);
                }

                @Test
                @DisplayName("기존 Review 상태 카드에 Again 평가 시 Relearning 상태로 전이")
                void shouldTransitionToRelearningOnAgain() {
                        // given: Review 상태의 기존 카드
                        UserReviewItem reviewItem = UserReviewItem.builder()
                                        .userId(TEST_USER_ID)
                                        .contentType(ReviewContentType.COURSE_QUESTION)
                                        .contentId(TEST_CONTENT_ID)
                                        .state(FsrsConstants.STATE_REVIEW)
                                        .stability(10.0)
                                        .difficulty(5.0)
                                        .reps(5)
                                        .lapses(0)
                                        .elapsedMinutes(5 * 1440)
                                        .lastReviewedAt(LocalDateTime.now().minusDays(5))
                                        .build();

                        // when
                        fsrsService.updateFsrsState(reviewItem, FsrsConstants.RATING_AGAIN);

                        // then
                        assertThat(reviewItem.getState()).isEqualTo(FsrsConstants.STATE_RELEARNING);
                        assertThat(reviewItem.getLapses()).isEqualTo(1);
                }

                @Test
                @DisplayName("scheduledMinutes는 최소 1분 이상이다 (데모용 축소 간격)")
                void shouldHaveMinimumScheduledMinutesOfOne() {
                        // given: 신규 카드 (낮은 안정성)
                        UserReviewItem newItem = UserReviewItem.builder()
                                        .userId(TEST_USER_ID)
                                        .contentType(ReviewContentType.COURSE_QUESTION)
                                        .contentId(TEST_CONTENT_ID)
                                        .state(FsrsConstants.STATE_NEW)
                                        .stability(0.0)
                                        .difficulty(5.0)
                                        .reps(0)
                                        .build();

                        // when
                        fsrsService.updateFsrsState(newItem, FsrsConstants.RATING_AGAIN);

                        // then: 데모용으로 최소 간격이 1분으로 축소됨
                        assertThat(newItem.getScheduledMinutes()).isGreaterThanOrEqualTo(1);
                }
        }

        // ══════════════════════════════════════════════════════
        // processReview 테스트
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("processReview 메서드는")
        class ProcessReview {

                @Test
                @DisplayName("신규 복습 항목을 생성하고 FSRS 상태를 초기화한다")
                void shouldCreateNewReviewItemAndInitializeFsrs() {
                        // given: 채점용 문제 mock
                        QuizCourseQuestion mockQuestion = QuizCourseQuestion.builder()
                                        .questionType(QuestionType.SHORT_ANSWER)
                                        .correctAnswer("answer")
                                        .build();
                        given(continuousQuizRepository.findById(TEST_CONTENT_ID))
                                        .willReturn(Optional.of(mockQuestion));

                        // given: 기존 복습 항목 없음
                        given(reviewItemRepository.findByUserIdAndContentTypeAndContentId(
                                        TEST_USER_ID, ReviewContentType.COURSE_QUESTION, TEST_CONTENT_ID))
                                        .willReturn(Optional.empty());
                        given(reviewItemRepository.save(any(UserReviewItem.class)))
                                        .willAnswer(inv -> inv.getArgument(0));
                        given(reviewLogRepository.save(any(UserReviewLog.class)))
                                        .willAnswer(inv -> inv.getArgument(0));

                        // when
                        ReviewResult result = fsrsService.processReview(
                                        TEST_USER_ID,
                                        ReviewContentType.COURSE_QUESTION,
                                        TEST_CONTENT_ID,
                                        "answer", // 정답에 해당하는 사용자 답안
                                        1500L // 1500ms → Easy
                        );

                        // then: 신규 항목이 생성되고 FSRS 상태가 초기화됨
                        assertThat(result).isNotNull();
                        assertThat(result.getItem().getUserId()).isEqualTo(TEST_USER_ID);
                        assertThat(result.getItem().getContentType()).isEqualTo(ReviewContentType.COURSE_QUESTION);
                        assertThat(result.getItem().getContentId()).isEqualTo(TEST_CONTENT_ID);
                        assertThat(result.getItem().getReps()).isEqualTo(1);
                        assertThat(result.getItem().getLastResponseTimeMs()).isEqualTo(1500L);
                        assertThat(result.isCorrect()).isTrue();

                        // 복습 로그가 저장되었는지 검증
                        verify(reviewLogRepository).save(any(UserReviewLog.class));
                }

                @Test
                @DisplayName("기존 복습 항목을 업데이트한다")
                void shouldUpdateExistingReviewItem() {
                        // given: 채점용 문제 mock
                        QuizCourseQuestion mockQuestion = QuizCourseQuestion.builder()
                                        .questionType(QuestionType.SHORT_ANSWER)
                                        .correctAnswer("answer")
                                        .build();
                        given(continuousQuizRepository.findById(TEST_CONTENT_ID))
                                        .willReturn(Optional.of(mockQuestion));

                        // given: 기존 복습 항목 존재
                        UserReviewItem existingItem = UserReviewItem.builder()
                                        .id(1L)
                                        .userId(TEST_USER_ID)
                                        .contentType(ReviewContentType.COURSE_QUESTION)
                                        .contentId(TEST_CONTENT_ID)
                                        .state(FsrsConstants.STATE_REVIEW)
                                        .stability(5.0)
                                        .difficulty(5.0)
                                        .reps(3)
                                        .lapses(0)
                                        .lastReviewedAt(LocalDateTime.now().minusDays(3))
                                        .build();

                        given(reviewItemRepository.findByUserIdAndContentTypeAndContentId(
                                        TEST_USER_ID, ReviewContentType.COURSE_QUESTION, TEST_CONTENT_ID))
                                        .willReturn(Optional.of(existingItem));
                        given(reviewItemRepository.save(any(UserReviewItem.class)))
                                        .willAnswer(inv -> inv.getArgument(0));
                        given(reviewLogRepository.save(any(UserReviewLog.class)))
                                        .willAnswer(inv -> inv.getArgument(0));

                        // when
                        ReviewResult result = fsrsService.processReview(
                                        TEST_USER_ID,
                                        ReviewContentType.COURSE_QUESTION,
                                        TEST_CONTENT_ID,
                                        "answer", // 정답에 해당하는 사용자 답안
                                        3000L // 3000ms → Good
                        );

                        // then
                        assertThat(result.getItem().getReps()).isEqualTo(4); // 기존 3 + 1
                        assertThat(result.getItem().getId()).isEqualTo(1L); // 같은 ID
                }

                @Test
                @DisplayName("복습 로그(UserReviewLog)를 생성한다")
                void shouldCreateReviewLog() {
                        // given: 채점용 문제 mock
                        QuizCourseQuestion mockQuestion = QuizCourseQuestion.builder()
                                        .questionType(QuestionType.SHORT_ANSWER)
                                        .correctAnswer("answer")
                                        .build();
                        given(continuousQuizRepository.findById(TEST_CONTENT_ID))
                                        .willReturn(Optional.of(mockQuestion));

                        given(reviewItemRepository.findByUserIdAndContentTypeAndContentId(
                                        anyLong(), any(), anyLong())).willReturn(Optional.empty());
                        given(reviewItemRepository.save(any(UserReviewItem.class)))
                                        .willAnswer(inv -> inv.getArgument(0));
                        given(reviewLogRepository.save(any(UserReviewLog.class)))
                                        .willAnswer(inv -> inv.getArgument(0));

                        // when
                        fsrsService.processReview(
                                        TEST_USER_ID,
                                        ReviewContentType.COURSE_QUESTION,
                                        TEST_CONTENT_ID,
                                        "answer", // 정답에 해당하는 사용자 답안
                                        2000L);

                        // then: 복습 로그 생성 검증
                        ArgumentCaptor<UserReviewLog> logCaptor = ArgumentCaptor.forClass(UserReviewLog.class);
                        verify(reviewLogRepository).save(logCaptor.capture());

                        UserReviewLog savedLog = logCaptor.getValue();
                        assertThat(savedLog.getIsCorrect()).isTrue();
                        assertThat(savedLog.getResponseTimeMs()).isEqualTo(2000L);
                        assertThat(savedLog.getStability()).isNotNull();
                        assertThat(savedLog.getDifficulty()).isNotNull();
                }

                @Test
                @DisplayName("오답 시 lapses가 증가하고 Relearning/Learning 상태로 전이")
                void shouldIncreaseLapsesOnIncorrectAnswer() {
                        // given: 채점용 문제 mock
                        QuizCourseQuestion mockQuestion = QuizCourseQuestion.builder()
                                        .questionType(QuestionType.SHORT_ANSWER)
                                        .correctAnswer("answer")
                                        .build();
                        given(continuousQuizRepository.findById(TEST_CONTENT_ID))
                                        .willReturn(Optional.of(mockQuestion));

                        // given: Review 상태의 기존 항목
                        UserReviewItem existingItem = UserReviewItem.builder()
                                        .id(1L)
                                        .userId(TEST_USER_ID)
                                        .contentType(ReviewContentType.COURSE_QUESTION)
                                        .contentId(TEST_CONTENT_ID)
                                        .state(FsrsConstants.STATE_REVIEW)
                                        .stability(10.0)
                                        .difficulty(5.0)
                                        .reps(5)
                                        .lapses(1)
                                        .lastReviewedAt(LocalDateTime.now().minusDays(5))
                                        .build();

                        given(reviewItemRepository.findByUserIdAndContentTypeAndContentId(
                                        TEST_USER_ID, ReviewContentType.COURSE_QUESTION, TEST_CONTENT_ID))
                                        .willReturn(Optional.of(existingItem));
                        given(reviewItemRepository.save(any(UserReviewItem.class)))
                                        .willAnswer(inv -> inv.getArgument(0));
                        given(reviewLogRepository.save(any(UserReviewLog.class)))
                                        .willAnswer(inv -> inv.getArgument(0));

                        // when
                        ReviewResult result = fsrsService.processReview(
                                        TEST_USER_ID,
                                        ReviewContentType.COURSE_QUESTION,
                                        TEST_CONTENT_ID,
                                        "wrong", // 오답에 해당하는 사용자 답안
                                        4000L);

                        // then
                        assertThat(result.getItem().getLapses()).isEqualTo(2); // 기존 1 + 1
                        assertThat(result.getItem().getState()).isEqualTo(FsrsConstants.STATE_RELEARNING);
                        assertThat(result.isCorrect()).isFalse();
                }
        }

        // ══════════════════════════════════════════════════════
        // Rating 임계값 상수 검증
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("Rating 임계값 상수는")
        class RatingThresholds {

                @Test
                @DisplayName("EASY_THRESHOLD_MS는 2000ms이다")
                void easyThresholdShouldBe2000ms() {
                        assertThat(FsrsConstants.EASY_THRESHOLD_MS).isEqualTo(2000L);
                }

                @Test
                @DisplayName("HARD_THRESHOLD_MS는 5000ms이다")
                void hardThresholdShouldBe5000ms() {
                        assertThat(FsrsConstants.HARD_THRESHOLD_MS).isEqualTo(5000L);
                }

                @Test
                @DisplayName("Rating 값은 1(Again), 2(Hard), 3(Good), 4(Easy)이다")
                void ratingValuesShouldBeCorrect() {
                        assertThat(FsrsConstants.RATING_AGAIN).isEqualTo(1);
                        assertThat(FsrsConstants.RATING_HARD).isEqualTo(2);
                        assertThat(FsrsConstants.RATING_GOOD).isEqualTo(3);
                        assertThat(FsrsConstants.RATING_EASY).isEqualTo(4);
                }
        }

        // ══════════════════════════════════════════════════════
        // enrichReviewItems 테스트 (getTodayReviewsWithQuestions 등)
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("문제 정보 조회 및 조합 (enrichReviewItems) 기능은")
        class Enrichment {

                @Test
                @DisplayName("오늘 복습할 항목과 문제 정보를 올바르게 조합하여 반환한다")
                void shouldEnrichDueItemsWithQuestionDetails() throws JsonProcessingException {
                        // given
                        LocalDateTime now = LocalDateTime.now();
                        UserReviewItem reviewItem = UserReviewItem.builder()
                                        .id(1L)
                                        .userId(TEST_USER_ID)
                                        .contentType(ReviewContentType.COURSE_QUESTION)
                                        .contentId(TEST_CONTENT_ID)
                                        .state(FsrsConstants.STATE_REVIEW)
                                        .nextReviewAt(now.minusHours(1))
                                        .build();

                        // Mock Question Entity structure
                        QuizCourse course = QuizCourse.builder().name("Java Basic").build();
                        ReflectionTestUtils.setField(course, "id", 10L);

                        QuizCourseSection section = QuizCourseSection.builder().course(course).build();
                        ReflectionTestUtils.setField(section, "quizCourseId", 10L); // Course ID와 일치시켜야 함
                        ReflectionTestUtils.setField(section, "sectionNumber", 1); // 섹션 번호 설정

                        QuizCourseQuestion question = QuizCourseQuestion.builder()
                                        .section(section)
                                        .questionText("Java Test Question")
                                        .questionType(QuestionType.MULTIPLE_CHOICE)
                                        .options("[{\"optionId\":1,\"text\":\"A\"}]")
                                        .correctAnswer("1")
                                        .explanation("Explanation")
                                        .build();
                        ReflectionTestUtils.setField(question, "id", TEST_CONTENT_ID);

                        given(reviewItemRepository.findDueItems(eq(TEST_USER_ID), any(LocalDateTime.class)))
                                        .willReturn(List.of(reviewItem));
                        given(continuousQuizRepository.findAllById(List.of(TEST_CONTENT_ID)))
                                        .willReturn(List.of(question));
                        given(objectMapper.readValue(anyString(), any(TypeReference.class)))
                                        .willReturn(List.of(new OptionItem("1", "A")));

                        // when
                        List<ReviewItemDto> result = fsrsService.getTodayReviewsWithQuestions(TEST_USER_ID);

                        // then
                        assertThat(result).hasSize(1);
                        ReviewItemDto dto = result.get(0);
                        assertThat(dto.reviewItemId()).isEqualTo(1L);
                        assertThat(dto.question()).isNotNull();
                        assertThat(dto.question().questionText()).isEqualTo("Java Test Question");
                        assertThat(dto.question().category()).isEqualTo("Java Basic"); // 코스명을 카테고리로 사용 확인
                        assertThat(dto.question().options()).hasSize(1);
                }

                @Test
                @DisplayName("문제 정보가 없는 경우(삭제됨 등)에도 에러 없이 항목을 반환하되 문제 정보는 null 또는 기본값 처리")
                void shouldHandleMissingQuestionGracefully() {
                        // given
                        UserReviewItem reviewItem = UserReviewItem.builder()
                                        .id(1L)
                                        .userId(TEST_USER_ID)
                                        .contentType(ReviewContentType.COURSE_QUESTION)
                                        .contentId(999L) // 존재하지 않는 문제 ID
                                        .build();

                        given(reviewItemRepository.findDueItems(eq(TEST_USER_ID), any(LocalDateTime.class)))
                                        .willReturn(List.of(reviewItem));
                        given(continuousQuizRepository.findAllById(List.of(999L)))
                                        .willReturn(List.of()); // 빈 리스트 반환

                        // when
                        List<ReviewItemDto> result = fsrsService.getTodayReviewsWithQuestions(TEST_USER_ID);

                        // then
                        assertThat(result).hasSize(1);
                        assertThat(result.get(0).question()).isNull(); // 문제 정보가 없으면 null
                }
        }

        // ══════════════════════════════════════════════════════
        // getCourseStats 테스트
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("getCourseStats 메서드는")
        class GetCourseStats {

                @Test
                @DisplayName("여러 코스에서 문제를 맞힌 경우 정확한 통계를 반환한다")
                void shouldReturnCorrectStatsWhenUserHasSolvedQuestionsInMultipleCourses() {
                        // given
                        // 전체 맞춘 문제 수 (중복 제거)
                        given(continuousQuizRepository.countTotalSolvedQuestions(TEST_USER_ID))
                                        .willReturn(45L);

                        // 코스별 전체 문제 수 Projection Mock
                        CourseQuestionStatsProjection totalProj1 = createMockProjection(1L, 50L);
                        CourseQuestionStatsProjection totalProj2 = createMockProjection(2L, 40L);
                        CourseQuestionStatsProjection totalProj3 = createMockProjection(3L, 60L);
                        given(continuousQuizRepository.countTotalQuestionsGroupByCourse())
                                        .willReturn(List.of(totalProj1, totalProj2, totalProj3));

                        // 코스별 맞춘 문제 수 Projection Mock
                        CourseQuestionStatsProjection solvedProj1 = createMockProjection(1L, 30L);
                        CourseQuestionStatsProjection solvedProj2 = createMockProjection(2L, 15L);
                        // courseId=3 은 맞춘 문제가 없음 → Map에 없음
                        given(continuousQuizRepository.countSolvedQuestionsGroupByCourse(TEST_USER_ID))
                                        .willReturn(List.of(solvedProj1, solvedProj2));

                        // 활성 코스 목록 (sortOrder 순서대로)
                        QuizCourse course1 = createMockCourse(1L, "Java 기초");
                        QuizCourse course2 = createMockCourse(2L, "Python 입문");
                        QuizCourse course3 = createMockCourse(3L, "알고리즘");
                        given(quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc())
                                        .willReturn(List.of(course1, course2, course3));

                        // when
                        ReviewCourseStatsResponse result = fsrsService.getCourseStats(TEST_USER_ID);

                        // then
                        assertThat(result.totalSolvedCount()).isEqualTo(45L);
                        assertThat(result.courseStats()).hasSize(3);

                        // sortOrder 순서 검증
                        assertThat(result.courseStats().get(0).courseId()).isEqualTo(1L);
                        assertThat(result.courseStats().get(0).courseName()).isEqualTo("Java 기초");
                        assertThat(result.courseStats().get(0).totalQuestions()).isEqualTo(50L);
                        assertThat(result.courseStats().get(0).solvedCount()).isEqualTo(30L);

                        assertThat(result.courseStats().get(1).courseId()).isEqualTo(2L);
                        assertThat(result.courseStats().get(1).solvedCount()).isEqualTo(15L);

                        // courseId=3은 맞춘 문제 없음 → 0 반환 (getOrDefault 검증)
                        assertThat(result.courseStats().get(2).courseId()).isEqualTo(3L);
                        assertThat(result.courseStats().get(2).solvedCount()).isEqualTo(0L);
                }

                @Test
                @DisplayName("정답 이력이 없는 코스는 solvedCount=0을 반환한다")
                void shouldReturnZeroForCourseWithNoSolvedQuestions() {
                        // given
                        given(continuousQuizRepository.countTotalSolvedQuestions(TEST_USER_ID))
                                        .willReturn(0L);

                        CourseQuestionStatsProjection totalProj = createMockProjection(1L, 50L);
                        given(continuousQuizRepository.countTotalQuestionsGroupByCourse())
                                        .willReturn(List.of(totalProj));

                        // 맞춘 문제 없음 → 빈 리스트
                        given(continuousQuizRepository.countSolvedQuestionsGroupByCourse(TEST_USER_ID))
                                        .willReturn(List.of());

                        QuizCourse course = createMockCourse(1L, "Java 기초");
                        given(quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc())
                                        .willReturn(List.of(course));

                        // when
                        ReviewCourseStatsResponse result = fsrsService.getCourseStats(TEST_USER_ID);

                        // then
                        assertThat(result.totalSolvedCount()).isEqualTo(0L);
                        assertThat(result.courseStats()).hasSize(1);
                        assertThat(result.courseStats().get(0).solvedCount()).isEqualTo(0L);
                        assertThat(result.courseStats().get(0).totalQuestions()).isEqualTo(50L);
                }

                @Test
                @DisplayName("한 문제를 여러 번 맞혀도 중복 제거되어 1개로 집계된다 (Repository Mock 데이터 기반)")
                void shouldHandleDuplicateCorrectAnswersAsSingleCount() {
                        // given: Repository가 이미 COUNT(DISTINCT ...)로 중복 제거된 값을 반환
                        // 예: 문제 ID 1, 2, 3을 각각 여러 번 맞혔지만 solvedCount=3 (중복 제거 후)
                        given(continuousQuizRepository.countTotalSolvedQuestions(TEST_USER_ID))
                                        .willReturn(3L); // 실제로는 5번 맞혔지만 중복 제거 후 3개

                        CourseQuestionStatsProjection totalProj = createMockProjection(1L, 10L);
                        given(continuousQuizRepository.countTotalQuestionsGroupByCourse())
                                        .willReturn(List.of(totalProj));

                        // 문제 3개를 여러 번 맞혔지만 중복 제거 후 3개
                        CourseQuestionStatsProjection solvedProj = createMockProjection(1L, 3L);
                        given(continuousQuizRepository.countSolvedQuestionsGroupByCourse(TEST_USER_ID))
                                        .willReturn(List.of(solvedProj));

                        QuizCourse course = createMockCourse(1L, "Java 기초");
                        given(quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc())
                                        .willReturn(List.of(course));

                        // when
                        ReviewCourseStatsResponse result = fsrsService.getCourseStats(TEST_USER_ID);

                        // then: 중복 제거된 값 검증
                        assertThat(result.totalSolvedCount()).isEqualTo(3L);
                        assertThat(result.courseStats().get(0).solvedCount()).isEqualTo(3L);
                }

                // Helper methods for mock creation
                private CourseQuestionStatsProjection createMockProjection(Long courseId, Long questionCount) {
                        return new CourseQuestionStatsProjection() {
                                @Override
                                public Long getCourseId() {
                                        return courseId;
                                }

                                @Override
                                public Long getQuestionCount() {
                                        return questionCount;
                                }

                                @Override
                                public Long getSumReps() {
                                        return 0L; // getCourseStats 테스트에서는 사용되지 않음
                                }

                                @Override
                                public Long getSumLapses() {
                                        return 0L; // getCourseStats 테스트에서는 사용되지 않음
                                }
                        };
                }

                private QuizCourse createMockCourse(Long id, String name) {
                        QuizCourse course = QuizCourse.builder()
                                        .code(name.toUpperCase().replace(" ", "_"))
                                        .name(name)
                                        .isActive(true)
                                        .build();
                        ReflectionTestUtils.setField(course, "id", id);
                        return course;
                }
                // ══════════════════════════════════════════════════════
                // getCourseWeaknessStats 테스트
                // ══════════════════════════════════════════════════════

                @Nested
                @DisplayName("getCourseWeaknessStats 메서드는")
                class GetCourseWeaknessStats {

                        @Test
                        @DisplayName("코스별 취약점 통계를 정확하게 집계하여 반환한다")
                        void shouldReturnCorrectWeaknessStats() {
                                // given
                                // 코스별 통계 Projection Mock (reps, lapses)
                                CourseQuestionStatsProjection p1 = createMockProjection(1L, 100L, 20L); // Java
                                CourseQuestionStatsProjection p2 = createMockProjection(2L, 50L, 5L); // Python

                                given(continuousQuizRepository.countReviewStatsGroupByCourse(TEST_USER_ID))
                                                .willReturn(List.of(p1, p2));

                                // 활성 코스 목록 (sortOrder 순서)
                                QuizCourse course1 = createMockCourse(1L, "Java 기초");
                                QuizCourse course2 = createMockCourse(2L, "Python 입문");
                                QuizCourse course3 = createMockCourse(3L, "알고리즘"); // 통계 없음

                                given(quizCourseRepository.findAllByIsActiveTrueOrderBySortOrderAscIdAsc())
                                                .willReturn(List.of(course1, course2, course3));

                                // when
                                ReviewCourseWeaknessResponse result = fsrsService.getCourseWeaknessStats(TEST_USER_ID);

                                // then
                                assertThat(result.courseWeaknessStats()).hasSize(3);

                                // Java
                                assertThat(result.courseWeaknessStats().get(0).courseId()).isEqualTo(1L);
                                assertThat(result.courseWeaknessStats().get(0).courseName()).isEqualTo("Java 기초");
                                assertThat(result.courseWeaknessStats().get(0).totalReps()).isEqualTo(100L);
                                assertThat(result.courseWeaknessStats().get(0).totalLapses()).isEqualTo(20L);

                                // Python
                                assertThat(result.courseWeaknessStats().get(1).courseId()).isEqualTo(2L);
                                assertThat(result.courseWeaknessStats().get(1).courseName()).isEqualTo("Python 입문");
                                assertThat(result.courseWeaknessStats().get(1).totalReps()).isEqualTo(50L);
                                assertThat(result.courseWeaknessStats().get(1).totalLapses()).isEqualTo(5L);

                                // Algorithm (통계 없음 -> 0 처리)
                                assertThat(result.courseWeaknessStats().get(2).courseId()).isEqualTo(3L);
                                assertThat(result.courseWeaknessStats().get(2).courseName()).isEqualTo("알고리즘");
                                assertThat(result.courseWeaknessStats().get(2).totalReps()).isEqualTo(0L);
                                assertThat(result.courseWeaknessStats().get(2).totalLapses()).isEqualTo(0L);
                        }

                        private CourseQuestionStatsProjection createMockProjection(Long courseId, Long sumReps,
                                        Long sumLapses) {
                                return new CourseQuestionStatsProjection() {
                                        @Override
                                        public Long getCourseId() {
                                                return courseId;
                                        }

                                        @Override
                                        public Long getQuestionCount() {
                                                return 0L; // Not used in this test
                                        }

                                        @Override
                                        public Long getSumReps() {
                                                return sumReps;
                                        }

                                        @Override
                                        public Long getSumLapses() {
                                                return sumLapses;
                                        }
                                };
                        }
                }
        }

        // ══════════════════════════════════════════════════════
        // getStats 테스트
        // ══════════════════════════════════════════════════════

        @Nested
        @DisplayName("getStats 메서드는")
        class GetStats {

                @Test
                @DisplayName("복습 항목이 없으면 모든 통계를 0으로 반환한다")
                void shouldReturnZeroStatsWhenNoReviewItems() {
                        // given
                        given(reviewItemRepository.findAllByUserId(TEST_USER_ID))
                                        .willReturn(List.of());
                        given(reviewItemRepository.countByUserIdAndNextReviewAtBefore(eq(TEST_USER_ID), any(LocalDateTime.class)))
                                        .willReturn(0L);

                        // when
                        ReviewStatsResponse result = fsrsService.getStats(TEST_USER_ID);

                        // then
                        assertThat(result.totalItems()).isEqualTo(0);
                        assertThat(result.dueItems()).isEqualTo(0);
                        assertThat(result.averageRetrievability()).isEqualTo(0.0);
                        assertThat(result.matureCards()).isEqualTo(0);
                        assertThat(result.dailyMaxCombo()).isEqualTo(0);
                }

                @Test
                @DisplayName("scheduledMinutes가 21일(30240분) 이상인 항목을 Mature Cards로 집계한다")
                void shouldCountMatureCardsCorrectly() {
                        // given
                        UserReviewItem matureItem1 = createReviewItemWithScheduledMinutes(1L, 30240); // 정확히 21일
                        UserReviewItem matureItem2 = createReviewItemWithScheduledMinutes(2L, 50000); // 21일 초과
                        UserReviewItem immatureItem = createReviewItemWithScheduledMinutes(3L, 10000); // 21일 미만

                        given(reviewItemRepository.findAllByUserId(TEST_USER_ID))
                                        .willReturn(List.of(matureItem1, matureItem2, immatureItem));
                        given(reviewItemRepository.countByUserIdAndNextReviewAtBefore(eq(TEST_USER_ID), any(LocalDateTime.class)))
                                        .willReturn(0L);
                        given(reviewLogRepository.findAllByReviewItemUserIdAndReviewedAtBetweenOrderByReviewedAtAsc(
                                        eq(TEST_USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                                        .willReturn(List.of());

                        // when
                        ReviewStatsResponse result = fsrsService.getStats(TEST_USER_ID);

                        // then
                        assertThat(result.matureCards()).isEqualTo(2); // 21일 이상인 항목 2개
                        assertThat(result.totalItems()).isEqualTo(3);
                }

                @Test
                @DisplayName("오늘의 최고 콤보를 정확하게 계산한다")
                void shouldCalculateDailyMaxComboCorrectly() {
                        // given
                        UserReviewItem item = createReviewItemWithScheduledMinutes(1L, 1440);
                        given(reviewItemRepository.findAllByUserId(TEST_USER_ID))
                                        .willReturn(List.of(item));
                        given(reviewItemRepository.countByUserIdAndNextReviewAtBefore(eq(TEST_USER_ID), any(LocalDateTime.class)))
                                        .willReturn(0L);

                        // 오늘의 복습 로그: 정답-정답-정답-오답-정답-정답 (최고 콤보 = 3)
                        List<UserReviewLog> todayLogs = List.of(
                                        createReviewLog(item, true),  // 콤보 1
                                        createReviewLog(item, true),  // 콤보 2
                                        createReviewLog(item, true),  // 콤보 3 ← 최고
                                        createReviewLog(item, false), // 콤보 리셋
                                        createReviewLog(item, true),  // 콤보 1
                                        createReviewLog(item, true)   // 콤보 2
                        );

                        given(reviewLogRepository.findAllByReviewItemUserIdAndReviewedAtBetweenOrderByReviewedAtAsc(
                                        eq(TEST_USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                                        .willReturn(todayLogs);

                        // when
                        ReviewStatsResponse result = fsrsService.getStats(TEST_USER_ID);

                        // then
                        assertThat(result.dailyMaxCombo()).isEqualTo(3);
                }

                // ── Helper Methods ──

                private UserReviewItem createReviewItemWithScheduledMinutes(Long id, int scheduledMinutes) {
                        UserReviewItem item = UserReviewItem.builder()
                                        .userId(TEST_USER_ID)
                                        .contentType(ReviewContentType.COURSE_QUESTION)
                                        .contentId(id)
                                        .state(FsrsConstants.STATE_REVIEW)
                                        .stability(10.0)
                                        .difficulty(5.0)
                                        .scheduledMinutes(scheduledMinutes)
                                        .lastReviewedAt(LocalDateTime.now().minusDays(1))
                                        .build();
                        ReflectionTestUtils.setField(item, "id", id);
                        return item;
                }

                private UserReviewLog createReviewLog(UserReviewItem item, boolean isCorrect) {
                        return UserReviewLog.builder()
                                        .reviewItem(item)
                                        .isCorrect(isCorrect)
                                        .responseTimeMs(1000L)
                                        .stability(5.0)
                                        .difficulty(5.0)
                                        .build();
                }
        }
}
