package com.ssafy.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BoardReportRequest(
        @NotBlank(message = "신고 사유는 필수입니다.")
        String reason
) {
}
