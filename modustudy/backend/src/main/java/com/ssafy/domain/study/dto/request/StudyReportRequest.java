package com.ssafy.domain.study.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StudyReportRequest(
        @NotBlank(message = "신고 사유는 필수입니다.")
        String reason
        ) {
}
