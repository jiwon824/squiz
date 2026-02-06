package com.ssafy.domain.quiz.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.domain.quiz.entity.ReviewContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 복습 결과 제출 요청 DTO.
 *
 * @param contentType    콘텐츠 유형 (COURSE_QUESTION, STUDY_QUESTION)
 * @param contentId      콘텐츠 ID
 * @param userAnswer     사용자 답안
 * @param responseTimeMs 응답 시간 (밀리초)
 */
 @Schema(description = "복습 결과 제출 요청")
 public record ReviewSubmitRequest(

        @Schema(description = "콘텐츠 유형", example = "COURSE_QUESTION") @NotNull(message = "콘텐츠 유형은 필수입니다") ReviewContentType contentType,

        @Schema(description = "콘텐츠 ID", example = "101") @NotNull(message = "콘텐츠 ID는 필수입니다") Long contentId,

        @Schema(description = "사용자 답안", example = "0") String userAnswer,

        @Schema(description = "응답 시간 (밀리초)", example = "3200") @NotNull(message = "응답 시간은 필수입니다") @Positive(message = "응답 시간은 양수여야 합니다") Long responseTimeMs) {
}
