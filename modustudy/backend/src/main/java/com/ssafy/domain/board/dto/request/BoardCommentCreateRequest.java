package com.ssafy.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BoardCommentCreateRequest(
        Long parentId,
        @NotBlank String content
) {
}
