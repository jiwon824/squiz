package com.ssafy.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BoardPostUpdateRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
