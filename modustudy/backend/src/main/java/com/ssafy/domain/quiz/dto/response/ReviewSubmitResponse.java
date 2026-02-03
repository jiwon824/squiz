package com.ssafy.domain.quiz.dto.response;

import com.ssafy.domain.quiz.entity.UserReviewItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 복습 결과 제출 응답 DTO.
 *
 * FSRS 알고리즘 처리 후 갱신된 상태를 반환한다.
 *
 * @param reviewItemId     복습 항목 ID
 * @param isCorrect        정답 여부
 * @param correctAnswer    정답 문자열
 * @param state            카드 상태 (0:New, 1:Learning, 2:Review, 3:Relearning)
 * @param stability        안정성 (S)
 * @param difficulty       난이도 (D)
 * @param scheduledMinutes 다음 복습까지 간격 (분)
 * @param nextReviewAt     다음 복습 예정일
 */
@Schema(description = "복습 결과 제출 응답")
public record ReviewSubmitResponse(

        @Schema(description = "복습 항목 ID", example = "1") Long reviewItemId,

        @Schema(description = "정답 여부", example = "true") boolean isCorrect,

        @Schema(description = "정답", example = "A") String correctAnswer,

        @Schema(description = "카드 상태 (0:New, 1:Learning, 2:Review, 3:Relearning)", example = "2") int state,

        @Schema(description = "안정성", example = "5.80") double stability,

        @Schema(description = "난이도", example = "4.93") double difficulty,

        @Schema(description = "다음 복습까지 간격 (분)", example = "10") int scheduledMinutes,

        @Schema(description = "다음 복습 예정일") LocalDateTime nextReviewAt) {
    /**
     * UserReviewItem 엔티티로부터 응답 DTO를 생성한다.
     */
    public static ReviewSubmitResponse from(UserReviewItem item, boolean isCorrect, String correctAnswer) {
        return new ReviewSubmitResponse(
                item.getId(),
                isCorrect,
                correctAnswer,
                item.getState(),
                item.getStability(),
                item.getDifficulty(),
                item.getScheduledMinutes(),
                item.getNextReviewAt());
    }
}
