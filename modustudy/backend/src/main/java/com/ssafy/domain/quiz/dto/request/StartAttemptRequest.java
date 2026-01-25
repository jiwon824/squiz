package com.ssafy.domain.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 섹션 시도 시작 요청 DTO.
 *
 * 사용자가 퀴즈 섹션을 시작할 때 사용한다.
 *
 * @param sectionId 시작할 섹션 ID
 */
@Schema(description = "섹션 시도 시작 요청")
public record StartAttemptRequest(
        @Schema(description = "시작할 섹션 ID", example = "1")
        @NotNull
        Long sectionId
) {
}
