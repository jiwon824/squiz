package com.ssafy.domain.quiz.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.quiz.dto.request.ContinuousAnswerRequest;
import com.ssafy.domain.quiz.dto.response.ContinuousQuestionResponse;
import com.ssafy.domain.quiz.dto.response.ContinuousSubmitResponse;
import com.ssafy.domain.quiz.dto.response.QuizCourseDetailResponse;
import com.ssafy.domain.quiz.dto.response.QuizCourseListResponse;
import com.ssafy.domain.quiz.dto.response.SectionsWithProgressResponse;
import com.ssafy.domain.quiz.dto.response.SectionsWithProgressResponse;
import com.ssafy.domain.quiz.dto.response.WeakConceptDto;
import com.ssafy.domain.quiz.service.ContinuousQuizService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 연속 학습 모드 통합 컨트롤러.
 *
 * <p>
 * 코스 조회 + 연속 학습 문제 풀이를 하나의 컨트롤러에서 제공한다.
 * </p>
 *
 * <h3>엔드포인트</h3>
 * <ul>
 * <li>GET /courses — 코스 목록 (PUBLIC)</li>
 * <li>GET /courses/{courseId} — 코스 상세 (PUBLIC)</li>
 * <li>GET /courses/{courseId}/sections — 섹션 + 진행 상황 (AUTH)</li>
 * <li>GET /courses/{courseId}/sections/{sn}/next — 다음 문제 (AUTH)</li>
 * <li>POST /questions/{questionId}/submit — 답변 제출 + 다음 문제 (AUTH)</li>
 * </ul>
 */
@Slf4j
@Tag(name = "Continuous Quiz", description = "연속 학습 모드 API (코스 조회 + 문제 풀이)")
@RestController
@RequestMapping("/api/v1/continuous-quiz")
@RequiredArgsConstructor
public class ContinuousQuizController {

    private final ContinuousQuizService continuousQuizService;

    // ==================== 코스 조회 (PUBLIC) ====================

    /**
     * 코스 목록 조회. 인증 없이 접근 가능.
     */
    @Operation(summary = "코스 목록 조회", description = "활성화된 코스 목록을 조회합니다.")
    @GetMapping("/courses")
    public ApiResponse<QuizCourseListResponse> getCourseList() {
        QuizCourseListResponse response = continuousQuizService.getCourseList();
        log.info("[ContinuousQuiz] Fetched {} courses", response.courses().size());
        return ApiResponse.success(response);
    }

    /**
     * 코스 상세 조회. 인증 없이 접근 가능.
     */
    @Operation(summary = "코스 상세 조회", description = "코스 상세 정보와 섹션 목록을 조회합니다.")
    @GetMapping("/courses/{courseId}")
    public ApiResponse<QuizCourseDetailResponse> getCourseDetail(
            @Parameter(description = "코스 ID") @PathVariable Long courseId) {
        return ApiResponse.success(continuousQuizService.getCourseDetail(courseId));
    }

    /**
     * 섹션 목록 조회 (진행 상황 포함). 인증 필요.
     */
    @Operation(summary = "섹션 목록 조회 (진행 상황 포함)", description = "인증된 사용자의 진행 상황이 포함된 섹션 목록을 조회합니다.")
    @GetMapping("/courses/{courseId}/sections")
    public ApiResponse<SectionsWithProgressResponse> getSectionsWithProgress(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.success(continuousQuizService.getSectionsWithProgress(courseId, userId));
    }

    // ==================== 연속 학습 (AUTH) ====================

    /**
     * 섹션의 첫 문제 또는 다음 문제 조회.
     */
    @Operation(summary = "다음 문제 조회", description = "특정 섹션의 다음 학습 문제를 하나 가져옵니다.")
    @GetMapping("/courses/{courseId}/sections/{sectionNumber}/next")
    public ApiResponse<ContinuousQuestionResponse> getNextQuestion(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @Parameter(description = "섹션 번호") @PathVariable Integer sectionNumber,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        return ApiResponse.success(
                continuousQuizService.getNextQuestion(userDetails.getUser().getId(), courseId, sectionNumber));
    }

    /**
     * 정답 제출 및 다음 문제 즉시 반환 (Atomic API).
     */
    @Operation(summary = "정답 제출 및 다음 문제 조회", description = "현재 문제의 정답을 제출하고 FSRS 상태를 업데이트한 뒤, 다음 문제를 즉시 반환합니다.")
    @PostMapping("/questions/{questionId}/submit")
    public ApiResponse<ContinuousSubmitResponse> submitAndNext(
            @Parameter(description = "현재 문제 ID") @PathVariable Long questionId,
            @Valid @RequestBody ContinuousAnswerRequest request,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        log.info("[ContinuousQuiz] User {} submitted question {}", userDetails.getUser().getId(), questionId);

        return ApiResponse.success(
                continuousQuizService.processAnswerAndGetNext(userDetails.getUser().getId(), questionId, request));
    }

    /**
     * 취약 개념(섹션) 조회.
     */
    @Operation(summary = "취약 개념 조회", description = "사용자가 가장 어려워하는 섹션(개념) 상위 N개를 조회합니다. (FSRS 안정도 기반)")
    @GetMapping("/weak-concepts")
    public ApiResponse<List<WeakConceptDto>> getWeakConcepts(
            @Parameter(description = "조회할 개수 (기본값 5)") @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        return ApiResponse.success(
                continuousQuizService.getWeakConcepts(userDetails.getUser().getId(), limit));
    }

    /**
     * 코스별 학습 통계 조회.
     */
    @Operation(summary = "코스별 학습 통계 조회", description = "코스별 시도한 문제 수(Attempted)와 정답 수(Correct)를 조회합니다.")
    @GetMapping("/course-stats")
    public ApiResponse<List<com.ssafy.domain.quiz.dto.response.CourseQuizStatDto>> getCourseStats(
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        return ApiResponse.success(continuousQuizService.getCourseStats(userDetails.getUser().getId()));
    }
}
