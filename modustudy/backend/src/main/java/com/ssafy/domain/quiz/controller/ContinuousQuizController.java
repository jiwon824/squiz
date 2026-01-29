package com.ssafy.domain.quiz.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.quiz.dto.request.ContinuousAnswerRequest;
import com.ssafy.domain.quiz.dto.response.ContinuousQuestionResponse;
import com.ssafy.domain.quiz.dto.response.ContinuousSubmitResponse;
import com.ssafy.domain.quiz.service.ContinuousQuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Continuous Quiz", description = "연속 학습 모드 API (말해보카 방식)")
@RestController
@RequestMapping("/api/v1/continuous-quiz")
@RequiredArgsConstructor
public class ContinuousQuizController {

    private final ContinuousQuizService continuousQuizService; // 서비스명 일치

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
                continuousQuizService.getNextQuestion(userDetails.getUser().getId(), courseId, sectionNumber)
        );
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
                continuousQuizService.processAnswerAndGetNext(userDetails.getUser().getId(), questionId, request)
        );
    }
}