package com.ssafy.domain.attendance.dto.response;

import com.ssafy.domain.attendance.entity.Attendance;
import com.ssafy.domain.attendance.entity.AttendanceCheckType;
import com.ssafy.domain.attendance.entity.AttendanceExcuseStatus;
import com.ssafy.domain.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "출석 응답")
public record AttendanceResponse(
        @Schema(description = "출석 ID", example = "1")
        Long id,
        @Schema(description = "세션 ID", example = "10")
        Long sessionId,
        @Schema(description = "사용자 ID", example = "100")
        Long userId,
        @Schema(description = "출석 체크 방식", example = "BLE")
        AttendanceCheckType checkType,
        @Schema(description = "출석 상태", example = "PRESENT")
        AttendanceStatus status,
        @Schema(description = "출석 체크 시각")
        LocalDateTime checkedAt,
        @Schema(description = "출석 처리자 ID (BLE/수동)", example = "1")
        Long checkedBy,
        @Schema(description = "소명 사유")
        String excuseReason,
        @Schema(description = "소명 처리 상태", example = "PENDING")
        AttendanceExcuseStatus excuseStatus
        ) {
    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getSession() == null ? null : attendance.getSession().getId(),
                attendance.getUser() == null ? null : attendance.getUser().getId(),
                attendance.getCheckType(),
                attendance.getStatus(),
                attendance.getCheckedAt(),
                attendance.getCheckedBy() == null ? null : attendance.getCheckedBy().getId(),
                attendance.getExcuseReason(),
                attendance.getExcuseStatus()
        );
    }
}
