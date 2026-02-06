package com.ssafy.domain.attendance.dto.request;

import com.ssafy.domain.attendance.entity.AttendanceExcuseStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결석 소명 승인/거절 요청")
public record AttendanceExcuseDecisionRequest(
        @Schema(description = "소명 처리 상태 (APPROVED/REJECTED)", example = "APPROVED")
        AttendanceExcuseStatus status
        ) {
}
