package com.ssafy.domain.quiz.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.quiz.dto.request.ReviewSubmitRequest;
import com.ssafy.domain.quiz.dto.response.ReviewCourseStatsResponse;
import com.ssafy.domain.quiz.dto.response.ReviewHistoryResponse;
import com.ssafy.domain.quiz.dto.response.ReviewResult;
import com.ssafy.domain.quiz.dto.response.ReviewStatsResponse;
import com.ssafy.domain.quiz.dto.response.ReviewSubmitResponse;
import com.ssafy.domain.quiz.dto.response.TodayReviewResponse;
import com.ssafy.domain.quiz.entity.WrongAnswerSortType;
import com.ssafy.domain.quiz.service.FsrsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 복습 (FSRS) API 컨트롤러.
 *
 * MalBoka 방식 자동 간격 반복 복습 시스템의 REST 엔드포인트를 제공한다.
 * 비즈니스 로직과 접근 제어는 FsrsService에 위임한다.
 */
@Slf4j
@Tag(name = "Review", description = "AI 복습 (FSRS) API")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final FsrsService fsrsService;

    // ========== 복습 결과 제출 ==========

    /**
     * 복습 결과를 제출하고 FSRS 알고리즘으로 다음 복습 일정을 산출한다.
     *
     * 정답 여부와 응답 시간을 기반으로 Rating을 자동 산출하고,
     * FSRS v14 알고리즘으로 안정성/난이도/다음 복습일을 갱신한다.
     *
     * @param request     복습 결과 (contentType, contentId, isCorrect, responseTimeMs)
     * @param userDetails 인증된 사용자 정보
     * @return FSRS 갱신 결과
     */
    @Operation(summary = "복습 결과 제출", description = "복습 결과를 제출하고 FSRS 알고리즘으로 다음 복습 일정을 산출합니다. 인증 필요.")
    @PostMapping
    public ApiResponse<ReviewSubmitResponse> submitReview(
            @Valid @RequestBody ReviewSubmitRequest request,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();

        ReviewResult result = fsrsService.processReview(
                userId,
                request.contentType(),
                request.contentId(),
                request.userAnswer(),
                request.responseTimeMs());

        log.info("[ReviewController] 복습 제출 - userId: {}, contentType: {}, contentId: {}, isCorrect: {}",
                userId, request.contentType(), request.contentId(), result.isCorrect());

        return ApiResponse
                .success(ReviewSubmitResponse.from(result.getItem(), result.isCorrect(), result.getCorrectAnswer()));
    }

    // ========== 오늘 복습 예정 항목 조회 ==========

    /**
     * 오늘 복습 예정인 항목 목록을 조회한다.
     *
     * nextReviewAt이 현재 시각 이하인 항목을 nextReviewAt 오름차순으로 반환한다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return 복습 예정 항목 목록
     */
    @Operation(summary = "오늘 복습 예정 항목 조회", description = "오늘 복습 예정인 항목 목록을 조회합니다. 인증 필요.")
    @GetMapping("/today")
    public ApiResponse<TodayReviewResponse> getTodayReviews(
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        List<TodayReviewResponse.ReviewItemDto> items = fsrsService.getTodayReviewsWithQuestions(userId);

        return ApiResponse.success(new TodayReviewResponse(items, items.size()));
    }

    /**
     * 오답 노트 (많이 틀린 문제) 조회.
     *
     * lapses > 0 인 항목을 정렬 방식에 따라 반환한다.
     *
     * @param sortType 정렬 방식 (기본값: MOST_WRONG)
     */
    @Operation(summary = "오답 노트 조회", description = "오답 횟수가 많은 순 또는 FSRS 복습 우선순위로 문제 목록을 조회합니다. 인증 필요.")
    @GetMapping("/wrong-answers")
    public ApiResponse<TodayReviewResponse> getWrongAnswers(
            @Parameter(description = "정렬 방식 (MOST_WRONG: 많이 틀린 순, FSRS_RECOMMENDED: 복습 우선순위)") @RequestParam(required = false, defaultValue = "MOST_WRONG") WrongAnswerSortType sortType,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        List<TodayReviewResponse.ReviewItemDto> items = fsrsService.getWrongAnswersWithQuestions(userId, sortType);

        return ApiResponse.success(new TodayReviewResponse(items, items.size()));
    }

    // ========== 복습 항목 이력 조회 ==========

    /**
     * 특정 복습 항목의 상세 정보와 최근 복습 이력을 조회한다.
     *
     * 본인의 복습 항목만 조회할 수 있으며, 타인의 항목 접근 시 403을 반환한다.
     * 최근 10건의 복습 이력을 반환한다.
     *
     * @param reviewItemId 복습 항목 ID
     * @param userDetails  인증된 사용자 정보
     * @return 복습 항목 상세 및 최근 10건 이력
     */
    @Operation(summary = "복습 항목 이력 조회", description = "특정 복습 항목의 상세 정보와 최근 복습 이력을 조회합니다. 인증 필요.")
    @GetMapping("/{reviewItemId}/history")
    public ApiResponse<ReviewHistoryResponse> getReviewHistory(
            @Parameter(description = "복습 항목 ID") @PathVariable Long reviewItemId,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();

        return ApiResponse.success(fsrsService.getReviewHistory(userId, reviewItemId));
    }

    // ========== 복습 통계 조회 ==========

    /**
     * 사용자의 전체 복습 통계를 조회한다.
     *
     * 상태별 항목 수, 평균 안정성, 총 복습/오답 횟수, 숙련도 등을 반환한다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return 복습 통계
     */
    @Operation(summary = "복습 통계 조회", description = "사용자의 전체 복습 통계를 조회합니다. 인증 필요.")
    @GetMapping("/stats")
    public ApiResponse<ReviewStatsResponse> getReviewStats(
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();

        return ApiResponse.success(fsrsService.getStats(userId));
    }

    /**
     * 사용자의 코스별 정답 통계를 조회한다.
     *
     * 중복 카운트 방지: 한 문제를 여러 번 맞혔어도 '맞춘 문제'는 1개로 취급한다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return 전체 맞춘 문제 수 및 코스별 상세 통계
     */
    @Operation(summary = "코스별 정답 통계 조회", description = "사용자의 전체 맞춘 문제 수와 코스별 상세 통계를 조회합니다. 인증 필요.")
    @GetMapping("/courses/stats")
    public ApiResponse<ReviewCourseStatsResponse> getCourseStats(
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();

        return ApiResponse.success(fsrsService.getCourseStats(userId));
    }
}
