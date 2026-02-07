package com.ssafy.domain.attendance.dto.request;

import com.ssafy.domain.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "출석 상태 수동 변경 요청")
public record AttendanceManualUpdateRequest(
        @Schema(description = "변경할 출석 상태", example = "PRESENT")
        AttendanceStatus status,
        @Schema(description = "사유 (선택)", example = "BLE 인식 오류")
        String reason
        ) {
}
