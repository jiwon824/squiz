package com.ssafy.domain.attendance.dto.response;

import com.ssafy.domain.attendance.entity.AttendanceCheckType;
import com.ssafy.domain.attendance.entity.AttendanceExcuseStatus;
import com.ssafy.domain.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "월별 출석 캘린더 응답")
public record AttendanceCalendarResponse(
        @Schema(description = "년도", example = "2026")
        int year,
        @Schema(description = "월", example = "1")
        int month,
        @Schema(description = "출석 기록 목록")
        List<AttendanceCalendarItem> items
        ) {
    @Schema(description = "캘린더 출석 항목")
    public record AttendanceCalendarItem(
            @Schema(description = "날짜", example = "2026-01-29")
            LocalDate date,
            @Schema(description = "세션 ID", example = "10")
            Long sessionId,
            @Schema(description = "출석 상태", example = "PRESENT")
            AttendanceStatus status,
            @Schema(description = "결석 소명 처리 상태", example = "PENDING")
            AttendanceExcuseStatus excuseStatus,
            @Schema(description = "출석 체크 방식", example = "AUTO")
            AttendanceCheckType checkType,
            @Schema(description = "세션 시작 시간")
            LocalDateTime scheduledAt
    ) {
    }
}
