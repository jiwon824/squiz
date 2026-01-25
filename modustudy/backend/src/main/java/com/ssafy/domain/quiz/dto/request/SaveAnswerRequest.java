package com.ssafy.domain.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 단일 답안 저장 요청 DTO.
 *
 * 진행 중인 시도에 대해 단일 답안을 저장할 때 사용한다.
 *
 * @param answer 저장할 답안
 */
@Schema(description = "단일 답안 저장 요청")
public record SaveAnswerRequest(
        @Schema(description = "저장할 답안")
        @NotNull
        AnswerItem answer
) {
    /**
     * 답안 항목.
     *
     * @param questionId 문제 ID
     * @param answer 사용자 답안
     */
    @Schema(description = "답안 항목")
    public record AnswerItem(
            @Schema(description = "문제 ID", example = "123")
            @NotNull
            Long questionId,

            @Schema(description = "사용자 답안", example = "A")
            String answer
    ) {
    }
}
