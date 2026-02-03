package com.ssafy.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BoardPostCreateRequest(
        @NotNull Long studyId,
        @NotBlank String title,
        @NotBlank String content
) {
}
