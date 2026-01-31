package com.ssafy.domain.quiz.controller;

import com.ssafy.domain.quiz.dto.response.StudyQuizListResponse;
import com.ssafy.domain.quiz.dto.response.StudyQuizDetailResponse;
import com.ssafy.domain.quiz.entity.StudyQuiz;
import com.ssafy.domain.quiz.service.StudyQuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}
