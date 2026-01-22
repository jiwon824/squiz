package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.enums.AttemptStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시도 결과 응답 DTO.
 *
 * 시도 제출 후 채점 결과를 반환한다.
 *
 * @param attemptId 시도 ID
 * @param score 획득 점수 (%)
 * @param correctCount 맞힌 문제 수
 * @param totalQuestions 총 문제 수
 * @param isPassed 통과 여부
 * @param status 시도 상태
 * @param completedAt 완료 시각
 * @param nextSectionUnlocked 다음 섹션 해금 여부
 * @param results 문제별 결과
 */
@Schema(description = "시도 결과 응답")
public record AttemptResultResponse(
        @Schema(description = "시도 ID", example = "1")
        Long attemptId,

        @Schema(description = "획득 점수 (%)", example = "80")
        int score,

        @Schema(description = "맞힌 문제 수", example = "24")
        int correctCount,

        @Schema(description = "총 문제 수", example = "30")
        int totalQuestions,

        @Schema(description = "통과 여부", example = "true")
        boolean isPassed,

        @Schema(description = "시도 상태", example = "COMPLETED")
        AttemptStatus status,

        @Schema(description = "완료 시각", example = "2025-01-22T10:30:00")
        LocalDateTime completedAt,

        @Schema(description = "다음 섹션 해금 여부", example = "true")
        boolean nextSectionUnlocked,

        @Schema(description = "문제별 결과")
        List<QuestionResult> results
) {
    /**
     * 문제별 결과 항목.
     *
     * @param questionId 문제 ID
     * @param isCorrect 정답 여부
     * @param userAnswer 사용자 답안
     */
    @Schema(description = "문제별 결과")
    public record QuestionResult(
            @Schema(description = "문제 ID", example = "123")
            Long questionId,

            @Schema(description = "정답 여부", example = "true")
            boolean isCorrect,

            @Schema(description = "사용자 답안", example = "A")
            String userAnswer
    ) {
    }
}
