package com.ssafy.domain.attendance.dto.request;

import com.ssafy.domain.attendance.entity.AttendanceCheckType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "출석 체크 요청")
public record AttendanceRequest(
        @Schema(description = "출석 체크 방식 (BLE, SELF, AUTO)", example = "BLE")
        AttendanceCheckType checkType
        ) {
}
