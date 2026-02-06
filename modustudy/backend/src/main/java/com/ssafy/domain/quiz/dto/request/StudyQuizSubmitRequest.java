package com.ssafy.domain.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 스터디 퀴즈 답안 제출 요청 DTO.
 *
 * @param userAnswer     사용자 답안
 * @param responseTimeMs 응답 시간 (밀리초)
 */
 @Schema(description = "스터디 퀴즈 답안 제출 요청")
 public record StudyQuizSubmitRequest(

        @Schema(description = "사용자 답안", example = "B")
        @NotBlank(message = "답안은 필수입니다")
        String userAnswer,

        @Schema(description = "응답 시간 (밀리초)", example = "3200")
        @NotNull(message = "응답 시간은 필수입니다")
        @Positive(message = "응답 시간은 양수여야 합니다")
        Long responseTimeMs) {
}
