package com.ssafy.domain.attendance.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.common.response.MessageResponse;
import com.ssafy.domain.attendance.dto.request.AttendanceExcuseDecisionRequest;
import com.ssafy.domain.attendance.dto.request.AttendanceExcuseRequest;
import com.ssafy.domain.attendance.dto.request.AttendanceManualUpdateRequest;
import com.ssafy.domain.attendance.dto.response.AttendanceCalendarResponse;
import com.ssafy.domain.attendance.dto.response.AttendanceResponse;
import com.ssafy.domain.attendance.dto.response.SessionAttendanceInfoResponse;
import com.ssafy.domain.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Attendance", description = "출석 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/studies/{studyId}")
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * BLE 출석 시작
     */
    @Operation(summary = "BLE 출석 시작", description = "스터디장이 BLE 출석을 시작합니다.")
    @PostMapping("/sessions/{sessionId}/attendance/ble/start")
    public ResponseEntity<ApiResponse<MessageResponse>> startBleAttendance(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        MessageResponse response = attendanceService.startBleAttendance(studyId, sessionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * BLE 출석 체크
     */
    @Operation(summary = "BLE 출석 체크", description = "BLE 비콘 감지 시 출석을 체크합니다.")
    @PostMapping("/sessions/{sessionId}/attendance/ble/check")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkBleAttendance(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.checkAttendanceBle(studyId, sessionId, userId)
        ));
    }

    /**
     * 셀프 출석 체크
     */
    @Operation(summary = "셀프 출석 체크", description = "스터디장 미접속 15분 이후 셀프 출석을 체크합니다.")
    @PostMapping("/sessions/{sessionId}/attendance/self")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkSelfAttendance(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.checkAttendanceSelf(studyId, sessionId, userId)
        ));
    }

    /**
     * 온라인 자동 출석 체크
     */
    @Operation(summary = "온라인 자동 출석 체크", description = "온라인 미팅 자동 시작 후 10분 내 입장 시 자동 출석을 체크합니다.")
    @PostMapping("/sessions/{sessionId}/attendance/online/auto")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkAutoAttendance(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.checkAttendanceAutoOnline(studyId, sessionId, userId)
        ));
    }

    /**
     * 출석 상태 수동 변경 (스터디장)
     */
    @Operation(summary = "출석 상태 수동 변경", description = "스터디장이 출석 상태를 수동으로 변경합니다.")
    @PatchMapping("/sessions/{sessionId}/attendance/{targetUserId}")
    public ResponseEntity<ApiResponse<AttendanceResponse>> updateAttendanceStatus(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @Parameter(description = "대상 사용자 ID") @PathVariable Long targetUserId,
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @Valid @RequestBody AttendanceManualUpdateRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.updateAttendanceStatus(studyId, sessionId, userId, targetUserId, request)
        ));
    }

    /**
     * 실시간 출석 현황 조회 (스터디장)
     */
    @Operation(summary = "실시간 출석 현황 조회", description = "스터디장이 세션별 출석 현황을 조회합니다.")
    @GetMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<ApiResponse<SessionAttendanceInfoResponse>> getSessionAttendance(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getSessionAttendance(studyId, sessionId, userId)
        ));
    }

    /**
     * 월별 출석 캘린더 조회
     */
    @Operation(summary = "월별 출석 캘린더 조회", description = "사용자가 월별 출석 기록을 캘린더 형태로 조회합니다.")
    @GetMapping("/attendance/calendar")
    public ResponseEntity<ApiResponse<AttendanceCalendarResponse>> getMonthlyCalendar(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @Parameter(description = "연도") @RequestParam int year,
            @Parameter(description = "월") @RequestParam int month,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getMonthlyCalendar(studyId, userId, year, month)
        ));
    }

    /**
     * 결석 소명 제출
     */
    @Operation(summary = "결석 소명 제출", description = "결석에 대한 소명을 제출합니다.")
    @PostMapping("/sessions/{sessionId}/attendance/excuse")
    public ResponseEntity<ApiResponse<AttendanceResponse>> submitExcuse(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @Valid @RequestBody AttendanceExcuseRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.submitExcuse(studyId, sessionId, userId, request)
        ));
    }

    /**
     * 결석 소명 승인/거절 (스터디장)
     */
    @Operation(summary = "결석 소명 승인/거절", description = "스터디장이 결석 소명을 승인 또는 거절합니다.")
    @PatchMapping("/sessions/{sessionId}/attendance/{targetUserId}/excuse")
    public ResponseEntity<ApiResponse<AttendanceResponse>> decideExcuse(
            @Parameter(description = "스터디 ID") @PathVariable Long studyId,
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @Parameter(description = "대상 사용자 ID") @PathVariable Long targetUserId,
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @Valid @RequestBody AttendanceExcuseDecisionRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.decideExcuse(studyId, sessionId, userId, targetUserId, request)
        ));
    }
}
