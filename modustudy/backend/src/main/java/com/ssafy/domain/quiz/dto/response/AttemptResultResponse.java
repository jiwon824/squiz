package com.ssafy.domain.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 시도 결과 응답 DTO.
 *
 * 시도 제출 후 채점 결과를 반환한다.
 *
 * @param attemptId            시도 ID
 * @param score                획득 점수 (%)
 * @param correctCount         맞힌 문제 수
 * @param totalQuestions       총 문제 수
 * @param passScore            통과 기준 점수 (%)
 * @param isPassed             통과 여부
 * @param isNextSectionUnlocked 다음 섹션 해금 여부
 * @param earnedBadge          획득한 배지 (코스 완료 시)
 * @param results              문제별 결과
 */
@Schema(description = "시도 결과 응답")
public record AttemptResultResponse(
        @Schema(description = "시도 ID", example = "123")
        Long attemptId,

        @Schema(description = "획득 점수 (%)", example = "80")
        int score,

        @Schema(description = "맞힌 문제 수", example = "8")
        int correctCount,

        @Schema(description = "총 문제 수", example = "10")
        int totalQuestions,

        @Schema(description = "통과 기준 점수 (%)", example = "70")
        int passScore,

        @Schema(description = "통과 여부", example = "true")
        boolean isPassed,

        @Schema(description = "다음 섹션 해금 여부", example = "true")
        boolean isNextSectionUnlocked,

        @Schema(description = "획득한 배지 (코스 완료 시)")
        BadgeInfo earnedBadge,

        @Schema(description = "문제별 결과")
        List<QuestionResultItem> results
) {
    /**
     * 문제별 결과 항목.
     *
     * @param orderIndex    문제 순서 (1부터 시작)
     * @param questionId    문제 ID
     * @param userAnswer    사용자 답안
     * @param correctAnswer 정답
     * @param isCorrect     정답 여부
     * @param explanation   해설
     */
    @Schema(description = "문제별 결과 항목")
    public record QuestionResultItem(
            @Schema(description = "문제 순서", example = "1")
            int orderIndex,

            @Schema(description = "문제 ID", example = "101")
            Long questionId,

            @Schema(description = "사용자 답안", example = "B")
            String userAnswer,

            @Schema(description = "정답", example = "B")
            String correctAnswer,

            @Schema(description = "정답 여부", example = "true")
            Boolean isCorrect,

            @Schema(description = "해설", example = "Java에서 정수형은 int 키워드를 사용합니다.")
            String explanation
    ) {
    }
}
