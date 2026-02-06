package com.ssafy.domain.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 퀴즈 문제 보기 항목 DTO.
 *
 * @param id 보기 식별자 (A, B, C, D 등)
 * @param text 보기 내용
 */
 @Schema(description = "퀴즈 문제 보기 항목")
 public record OptionItem(
        @Schema(description = "보기 식별자", example = "A")
        String id,

        @Schema(description = "보기 내용", example = "int")
        String text
        ) {
}
