package com.ssafy.domain.quiz.service;

import com.ssafy.domain.quiz.entity.ReviewContentType;
import com.ssafy.domain.quiz.entity.UserReviewItem;
import com.ssafy.domain.quiz.entity.UserReviewLog;
import com.ssafy.domain.quiz.repository.UserReviewItemRepository;
import com.ssafy.domain.quiz.repository.UserReviewLogRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * FsrsService 단위 테스트.
 *
 * <p>FSRS v14 알고리즘 기반 간격 반복 복습 로직을 검증한다.</p>
 *
 * <h3>테스트 대상</h3>
 * <ul>
 *   <li>calculateRating: 정답 여부와 응답 시간 기반 자동 Rating 산출</li>
 *   <li>updateFsrsState: 신규 카드 초기화 및 기존 카드 상태 갱신</li>
 *   <li>processReview: 복습 처리 (Upsert + Log)</li>
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

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_CONTENT_ID = 100L;

    // ══════════════════════════════════════════════════════
    //  calculateRating 테스트
    // ══════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculateRating 메서드는")
    class CalculateRating {

        @Test
        @DisplayName("오답 시 항상 Again(1)을 반환한다")
        void shouldReturnAgainForIncorrectAnswer() {
            // given
            boolean isCorrect = false;
            long responseTimeMs = 1000L;  // 빠른 응답이어도 오답이면 Again

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
            long responseTimeMs = 2000L;  // 경계값

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
            long responseTimeMs = 5000L;  // 경계값

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
                "false, 500, 1",     // 오답 → Again
                "false, 10000, 1",   // 오답 → Again
                "true, 500, 4",      // 정답 & 빠름 → Easy
                "true, 2000, 4",     // 정답 & 경계 → Easy
                "true, 2001, 3",     // 정답 & 중간 → Good
                "true, 5000, 3",     // 정답 & 경계 → Good
                "true, 5001, 2",     // 정답 & 느림 → Hard
                "true, 10000, 2"     // 정답 & 매우 느림 → Hard
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
    //  updateFsrsState 테스트
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
            assertThat(newItem.getStability()).isGreaterThan(0);  // 초기 안정성 설정됨
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
                    .elapsedDays(3)
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
                    .elapsedDays(5)
                    .lastReviewedAt(LocalDateTime.now().minusDays(5))
                    .build();

            // when
            fsrsService.updateFsrsState(reviewItem, FsrsConstants.RATING_AGAIN);

            // then
            assertThat(reviewItem.getState()).isEqualTo(FsrsConstants.STATE_RELEARNING);
            assertThat(reviewItem.getLapses()).isEqualTo(1);
        }

        @Test
        @DisplayName("scheduledDays는 최소 1일 이상이다")
        void shouldHaveMinimumScheduledDaysOfOne() {
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

            // then
            assertThat(newItem.getScheduledDays()).isGreaterThanOrEqualTo(1);
        }
    }

    // ══════════════════════════════════════════════════════
    //  processReview 테스트
    // ══════════════════════════════════════════════════════

    @Nested
    @DisplayName("processReview 메서드는")
    class ProcessReview {

        @Test
        @DisplayName("신규 복습 항목을 생성하고 FSRS 상태를 초기화한다")
        void shouldCreateNewReviewItemAndInitializeFsrs() {
            // given: 기존 복습 항목 없음
            given(reviewItemRepository.findByUserIdAndContentTypeAndContentId(
                    TEST_USER_ID, ReviewContentType.COURSE_QUESTION, TEST_CONTENT_ID
            )).willReturn(Optional.empty());
            given(reviewItemRepository.save(any(UserReviewItem.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(reviewLogRepository.save(any(UserReviewLog.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            UserReviewItem result = fsrsService.processReview(
                    TEST_USER_ID,
                    ReviewContentType.COURSE_QUESTION,
                    TEST_CONTENT_ID,
                    true,   // 정답
                    1500L   // 1500ms → Easy
            );

            // then: 신규 항목이 생성되고 FSRS 상태가 초기화됨
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(result.getContentType()).isEqualTo(ReviewContentType.COURSE_QUESTION);
            assertThat(result.getContentId()).isEqualTo(TEST_CONTENT_ID);
            assertThat(result.getReps()).isEqualTo(1);
            assertThat(result.getLastResponseTimeMs()).isEqualTo(1500L);

            // 복습 로그가 저장되었는지 검증
            verify(reviewLogRepository).save(any(UserReviewLog.class));
        }

        @Test
        @DisplayName("기존 복습 항목을 업데이트한다")
        void shouldUpdateExistingReviewItem() {
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
                    TEST_USER_ID, ReviewContentType.COURSE_QUESTION, TEST_CONTENT_ID
            )).willReturn(Optional.of(existingItem));
            given(reviewItemRepository.save(any(UserReviewItem.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(reviewLogRepository.save(any(UserReviewLog.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            UserReviewItem result = fsrsService.processReview(
                    TEST_USER_ID,
                    ReviewContentType.COURSE_QUESTION,
                    TEST_CONTENT_ID,
                    true,
                    3000L  // 3000ms → Good
            );

            // then
            assertThat(result.getReps()).isEqualTo(4);  // 기존 3 + 1
            assertThat(result.getId()).isEqualTo(1L);   // 같은 ID
        }

        @Test
        @DisplayName("복습 로그(UserReviewLog)를 생성한다")
        void shouldCreateReviewLog() {
            // given
            given(reviewItemRepository.findByUserIdAndContentTypeAndContentId(
                    anyLong(), any(), anyLong()
            )).willReturn(Optional.empty());
            given(reviewItemRepository.save(any(UserReviewItem.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(reviewLogRepository.save(any(UserReviewLog.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            fsrsService.processReview(
                    TEST_USER_ID,
                    ReviewContentType.COURSE_QUESTION,
                    TEST_CONTENT_ID,
                    true,
                    2000L
            );

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
                    TEST_USER_ID, ReviewContentType.COURSE_QUESTION, TEST_CONTENT_ID
            )).willReturn(Optional.of(existingItem));
            given(reviewItemRepository.save(any(UserReviewItem.class)))
                    .willAnswer(inv -> inv.getArgument(0));
            given(reviewLogRepository.save(any(UserReviewLog.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            UserReviewItem result = fsrsService.processReview(
                    TEST_USER_ID,
                    ReviewContentType.COURSE_QUESTION,
                    TEST_CONTENT_ID,
                    false,  // 오답
                    4000L
            );

            // then
            assertThat(result.getLapses()).isEqualTo(2);  // 기존 1 + 1
            assertThat(result.getState()).isEqualTo(FsrsConstants.STATE_RELEARNING);
        }
    }

    // ══════════════════════════════════════════════════════
    //  Rating 임계값 상수 검증
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
}
