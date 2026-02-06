package com.ssafy.domain.quiz.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.quiz.dto.request.StudyQuizSubmitRequest;
import com.ssafy.domain.quiz.dto.response.StudyQuizDetailResponse;
import com.ssafy.domain.quiz.dto.response.StudyQuizListResponse;
import com.ssafy.domain.quiz.dto.response.StudyQuizSubmitResponse;
import com.ssafy.domain.quiz.entity.StudyQuiz;
import com.ssafy.domain.quiz.service.StudyQuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 스터디 퀴즈 API
 * AI 미팅 기반 자동 생성 퀴즈 조회
 */
 @Tag(name = "StudyQuiz", description = "스터디 퀴즈 API")
 @RestController
 @RequestMapping("/api/v1/studies/{studyId}/quizzes")
 @RequiredArgsConstructor
 public class StudyQuizController {

    private final StudyQuizService studyQuizService;

    /**
     * 스터디별 퀴즈 목록 조회
     */
    @Operation(summary = "스터디 퀴즈 목록 조회", description = "해당 스터디에서 생성된 퀴즈 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<List<StudyQuizListResponse>> getStudyQuizzes(
            @PathVariable Long studyId) {

        List<StudyQuiz> quizzes = studyQuizService.getQuizzesByStudyId(studyId);

        List<StudyQuizListResponse> response = quizzes.stream()
                .map(StudyQuizListResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 퀴즈 상세 조회 (문제 포함)
     */
    @Operation(summary = "퀴즈 상세 조회", description = "퀴즈의 문제들을 포함한 상세 정보를 조회합니다")
    @GetMapping("/{quizId}")
    public ResponseEntity<StudyQuizDetailResponse> getQuizDetail(
            @PathVariable Long studyId,
            @PathVariable Long quizId) {

        StudyQuiz quiz = studyQuizService.getQuizById(quizId);

        // 스터디 ID 검증
        if (!quiz.getStudyId().equals(studyId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(StudyQuizDetailResponse.from(quiz));
    }

    /**
     * 퀴즈 문제 답안 제출 — 채점 + FSRS 복습 스케줄링
     */
    @Operation(summary = "퀴즈 답안 제출",
            description = "퀴즈 문제에 답안을 제출하고 채점 결과 및 FSRS 복습 스케줄을 반환합니다. 인증 필요.")
    @PostMapping("/{quizId}/questions/{questionId}/submit")
    public ApiResponse<StudyQuizSubmitResponse> submitAnswer(
            @PathVariable Long studyId,
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            @Valid @RequestBody StudyQuizSubmitRequest request,
            @AuthenticationPrincipal SsafyUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();

        StudyQuizSubmitResponse response = studyQuizService.submitAnswer(
                userId, questionId, request.userAnswer(), request.responseTimeMs());

        return ApiResponse.success(response);
    }
}
