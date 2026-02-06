package com.ssafy.domain.attendance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결석 소명 제출 요청")
public record AttendanceExcuseRequest(
        @Schema(description = "소명 사유", example = "지각 버스 지연")
        String reason
        ) {
}
