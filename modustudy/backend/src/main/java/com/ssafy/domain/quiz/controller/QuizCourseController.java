package com.ssafy.domain.quiz.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.quiz.dto.request.SaveAnswerRequest;
import com.ssafy.domain.quiz.dto.response.*;
import com.ssafy.domain.quiz.service.QuizCourseService;
import com.ssafy.domain.quiz.service.QuizSectionAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 퀴즈 코스 API 컨트롤러.
 *
 * API 문서: docs/api/API_21_퀴즈코스.md
 *
 * 호출 경로:
 * - Web/Mobile 코스 목록 화면(SC-190, M-080)에서 GET /api/v1/quiz-courses
 */
@Slf4j
@Tag(name = "Quiz Course", description = "퀴즈 코스 API")
@RestController
@RequestMapping("/api/v1/quiz-courses")
@RequiredArgsConstructor
public class QuizCourseController {

    private final QuizCourseService quizCourseService;
    private final QuizSectionAttemptService attemptService;

    // ========== 코스 조회 API ==========

    /**
     * 코스 목록 조회.
     *
     * 인증 없이 접근 가능한 공개 목록이며, 활성화된 코스만 반환.
     *
     * @return 코스 목록 응답
     */
    @Operation(summary = "코스 목록 조회", description = "활성화된 코스 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<QuizCourseListResponse> getCourseList() {
        QuizCourseListResponse response = quizCourseService.getCourseList();
        log.info("[QuizCourseController] Fetched {} courses", response.courses().size());
        response.courses().forEach(course -> log.info("[QuizCourseController] Course: {}", course));
        return ApiResponse.success(response);
    }

    /**
     * 코스 상세 조회.
     *
     * 인증 없이 접근 가능한 공개 상세이며, 활성화된 코스만 반환.
     *
     * @param courseId 코스 ID
     * @return 코스 상세 응답
     */
    @Operation(summary = "코스 상세 조회", description = "코스 상세 정보와 섹션 목록을 조회합니다.")
    @GetMapping("/{courseId}")
    public ApiResponse<QuizCourseDetailResponse> getCourseDetail(
            @Parameter(description = "코스 ID") @PathVariable Long courseId) {
        return ApiResponse.success(quizCourseService.getCourseDetail(courseId));
    }

    /**
     * 섹션 목록 조회 (진행 상황 포함).
     *
     * 인증된 사용자의 진행 상황(해금 여부, 통과 여부, 최고 점수, 시도 횟수)이 포함된
     * 섹션 목록을 반환한다.
     *
     * @param courseId    코스 ID
     * @param userDetails 인증된 사용자 정보
     * @return 진행 상황이 포함된 섹션 목록
     */
    @Operation(summary = "섹션 목록 조회 (진행 상황 포함)", description = "인증된 사용자의 진행 상황이 포함된 섹션 목록을 조회합니다. 인증 필요.")
    @GetMapping("/{courseId}/sections")
    public ApiResponse<SectionsWithProgressResponse> getSectionsWithProgress(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.success(quizCourseService.getSectionsWithProgress(courseId, userId));
    }

    // ========== 섹션 시도 API ==========

    /**
     * 섹션 시도 시작/재개.
     *
     * 진행 중인 시도가 있으면 재개하고, 없으면 새로 생성한다.
     * 문제는 셔플되어 order_index 순서로 반환된다.
     *
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @param userDetails   인증된 사용자 정보
     * @return 시도 정보 및 문제 목록
     */
    @Operation(summary = "섹션 시도 시작/재개", description = "섹션 문제 풀이를 시작하거나 기존 진행 중인 시도를 재개합니다. 인증 필요.")
    @PostMapping("/{courseId}/sections/{sectionNumber}/attempts")
    public ApiResponse<SectionAttemptResponse> startOrResumeAttempt(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @Parameter(description = "섹션 번호") @PathVariable Integer sectionNumber,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.success(attemptService.startOrResumeAttempt(courseId, sectionNumber, userId));
    }

    /**
     * 특정 시도 재개 (명시적 attemptId 사용).
     *
     * 클라이언트가 이미 알고 있는 attemptId를 사용하여 특정 시도를 재개한다.
     * 진행 중인 시도만 재개할 수 있으며, 본인의 시도인지 검증한다.
     *
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @param attemptId     시도 ID
     * @param userDetails   인증된 사용자 정보
     * @return 시도 정보 및 문제 목록 (savedAnswer 포함)
     */
    @Operation(summary = "특정 시도 재개", description = "명시적 attemptId를 사용하여 특정 시도를 재개합니다. 인증 필요.")
    @PostMapping("/{courseId}/sections/{sectionNumber}/attempts/{attemptId}")
    public ApiResponse<SectionAttemptResponse> resumeAttempt(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @Parameter(description = "섹션 번호") @PathVariable Integer sectionNumber,
            @Parameter(description = "시도 ID") @PathVariable Long attemptId,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.success(attemptService.resumeAttempt(attemptId, userId));
    }

    /**
     * 단일 답안 실시간 저장.
     *
     * <p>
     * 사용자가 문제를 풀고 "다음" 버튼을 누를 때마다 호출되어
     * 해당 문제의 답안을 즉시 저장한다. 브라우저 충돌이나 네트워크
     * 끊김 시에도 데이터 유실을 방지하는 실시간 저장 방식이다.
     * </p>
     *
     * <p>
     * 동일 문제에 대해 여러 번 호출해도 마지막 답안으로 덮어쓰므로
     * 멱등성이 보장된다.
     * </p>
     *
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @param attemptId     시도 ID
     * @param request       저장할 단일 답안
     * @param userDetails   인증된 사용자 정보
     * @return 성공 응답
     */
    @Operation(summary = "단일 답안 실시간 저장", description = "문제 풀이 중 '다음' 버튼 클릭 시 해당 답안을 즉시 저장합니다. 멱등성 보장. 인증 필요.")
    @PatchMapping("/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/answers")
    public ApiResponse<Void> saveAnswer(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @Parameter(description = "섹션 번호") @PathVariable Integer sectionNumber,
            @Parameter(description = "시도 ID") @PathVariable Long attemptId,
            @Valid @RequestBody SaveAnswerRequest request,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        attemptService.saveAnswer(attemptId, request, userId);
        return ApiResponse.success((Void) null);
    }

    /**
     * 시도 제출 및 채점.
     *
     * 진행 중인 시도를 제출하고 채점 결과를 반환한다.
     * 통과 시 다음 섹션이 해금되고, 코스 완료 시 배지가 수여된다.
     *
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @param attemptId     시도 ID
     * @param userDetails   인증된 사용자 정보
     * @return 채점 결과
     */
    @Operation(summary = "시도 제출 및 채점", description = "시도를 제출하고 채점 결과를 받습니다. 인증 필요.")
    @PostMapping("/{courseId}/sections/{sectionNumber}/attempts/{attemptId}/submit")
    public ApiResponse<AttemptResultResponse> submitAttempt(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @Parameter(description = "섹션 번호") @PathVariable Integer sectionNumber,
            @Parameter(description = "시도 ID") @PathVariable Long attemptId,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.success(attemptService.submitAttempt(attemptId, userId));
    }

    /**
     * 시도 포기.
     *
     * 진행 중인 시도를 포기한다. 점수는 기록되지 않는다.
     *
     * @param courseId      코스 ID
     * @param sectionNumber 섹션 번호
     * @param attemptId     시도 ID
     * @param userDetails   인증된 사용자 정보
     * @return 성공 응답
     */
    @Operation(summary = "시도 포기", description = "진행 중인 시도를 포기합니다. 인증 필요.")
    @DeleteMapping("/{courseId}/sections/{sectionNumber}/attempts/{attemptId}")
    public ApiResponse<Void> abandonAttempt(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @Parameter(description = "섹션 번호") @PathVariable Integer sectionNumber,
            @Parameter(description = "시도 ID") @PathVariable Long attemptId,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        attemptService.abandonAttempt(attemptId, userId);
        return ApiResponse.success((Void) null);
    }

    // ========== Legacy API (Deprecated) ==========

    /**
     * 섹션 문제 조회 (Deprecated).
     *
     * @deprecated 시도 기반 API로 변경됨. {@link #startOrResumeAttempt} 사용 권장.
     */
    @Deprecated
    @Operation(summary = "섹션 문제 조회 (Deprecated)", description = "전체 섹션 문제를 조회하는 게 아니라, 1개의 시도에 정해진 개수의 문제를 보여주는 방식으로 변경(시도 기반 API로 변경) POST /attempts 사용 권고")
    @GetMapping("/{courseId}/sections/{sectionNumber}")
    public ApiResponse<SectionQuestionsResponse> getSectionQuestions(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @Parameter(description = "섹션 번호") @PathVariable Integer sectionNumber,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ApiResponse.success(quizCourseService.getSectionQuestions(courseId, sectionNumber, userId));
    }
}
