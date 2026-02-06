package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.UserReviewItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 스터디 퀴즈 답안 제출 응답 DTO.
 *
 * FSRS 복습 스케줄 + 채점 결과를 프론트엔드에 반환한다.
 */
 @Schema(description = "스터디 퀴즈 답안 제출 응답")
 public record StudyQuizSubmitResponse(

        @Schema(description = "정답 여부", example = "true")
        boolean isCorrect,

        @Schema(description = "정답", example = "B")
        String correctAnswer,

        @Schema(description = "해설")
        String explanation,

        @Schema(description = "다음 복습까지 간격 (분)", example = "5")
        int scheduledMinutes,

        @Schema(description = "다음 복습 예정일")
        LocalDateTime nextReviewAt,

        @Schema(description = "카드 상태 (0:New, 1:Learning, 2:Review, 3:Relearning)", example = "2")
        int state,

        @Schema(description = "총 복습 횟수", example = "1")
        int reps) {

    /**
     * UserReviewItem + 채점 결과로부터 응답 DTO를 생성한다.
     *
     * @param item          FSRS 갱신된 복습 항목
     * @param isCorrect     정답 여부
     * @param correctAnswer 정답 텍스트
     * @param explanation   해설 (nullable)
     * @return StudyQuizSubmitResponse
     */
    public static StudyQuizSubmitResponse from(UserReviewItem item, boolean isCorrect,
                                                String correctAnswer, String explanation) {
        return new StudyQuizSubmitResponse(
                isCorrect,
                correctAnswer,
                explanation,
                item.getScheduledMinutes(),
                item.getNextReviewAt(),
                item.getState(),
                item.getReps());
    }
}
