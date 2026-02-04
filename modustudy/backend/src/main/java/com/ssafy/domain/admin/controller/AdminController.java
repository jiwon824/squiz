package com.ssafy.domain.admin.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.exception.BusinessException;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.admin.dto.*;
import com.ssafy.domain.admin.service.AdminStatsService;
import com.ssafy.domain.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService adminStatsService;

    /**
     * 관리자 권한 체크
     */
    private void checkAdminRole(Authentication authentication) {
        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        if (userDetails.getUser().getRole() != Role.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "관리자 권한이 필요합니다.");
        }
    }

    /**
     * 대시보드 요약 정보
     * GET /api/v1/admin/dashboard/summary
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary(Authentication authentication) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getDashboardSummary()));
    }

    /**
     * 회원 가입 추이
     * GET /api/v1/admin/stats/users?days=30
     */
    @GetMapping("/stats/users")
    public ResponseEntity<ApiResponse<List<UserSignupStatsDto>>> getUserSignupStats(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int days) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getUserSignupStats(days)));
    }

    /**
     * 스터디 상태별 통계
     * GET /api/v1/admin/stats/studies
     */
    @GetMapping("/stats/studies")
    public ResponseEntity<ApiResponse<List<StudyStatusStatsDto>>> getStudyStatusStats(Authentication authentication) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getStudyStatusStats()));
    }

    /**
     * 로그인 방식별 통계
     * GET /api/v1/admin/stats/login-methods
     */
    @GetMapping("/stats/login-methods")
    public ResponseEntity<ApiResponse<List<LoginMethodStatsDto>>> getLoginMethodStats(Authentication authentication) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getLoginMethodStats()));
    }

    /**
     * 퀴즈 통계
     * GET /api/v1/admin/stats/quiz?days=30
     */
    @GetMapping("/stats/quiz")
    public ResponseEntity<ApiResponse<QuizStatsDto>> getQuizStats(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int days) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getQuizStats(days)));
    }

    /**
     * 최근 가입 회원
     * GET /api/v1/admin/recent-users?limit=10
     */
    @GetMapping("/recent-users")
    public ResponseEntity<ApiResponse<List<RecentUserDto>>> getRecentUsers(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getRecentUsers(limit)));
    }

    /**
     * 인기 스터디
     * GET /api/v1/admin/popular-studies?limit=5
     */
    @GetMapping("/popular-studies")
    public ResponseEntity<ApiResponse<List<PopularStudyDto>>> getPopularStudies(
            Authentication authentication,
            @RequestParam(defaultValue = "5") int limit) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getPopularStudies(limit)));
    }

    // ========== 새로운 시계열 통계 API ==========

    /**
     * 일별 미팅 통계
     * GET /api/v1/admin/stats/meetings?days=30
     */
    @GetMapping("/stats/meetings")
    public ResponseEntity<ApiResponse<List<DailyMeetingStatsDto>>> getDailyMeetingStats(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int days) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getDailyMeetingStats(days)));
    }

    /**
     * 일별 출석 통계
     * GET /api/v1/admin/stats/attendance?days=30
     */
    @GetMapping("/stats/attendance")
    public ResponseEntity<ApiResponse<List<DailyAttendanceStatsDto>>> getDailyAttendanceStats(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int days) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getDailyAttendanceStats(days)));
    }

    /**
     * 일별 활동 통계 (잔디)
     * GET /api/v1/admin/stats/activity?days=30
     */
    @GetMapping("/stats/activity")
    public ResponseEntity<ApiResponse<List<DailyActivityStatsDto>>> getDailyActivityStats(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int days) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getDailyActivityStats(days)));
    }

    /**
     * 레벨별 사용자 분포
     * GET /api/v1/admin/stats/levels
     */
    @GetMapping("/stats/levels")
    public ResponseEntity<ApiResponse<List<UserLevelStatsDto>>> getUserLevelStats(Authentication authentication) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getUserLevelStats()));
    }

    /**
     * 토픽별 스터디 분포
     * GET /api/v1/admin/stats/topics
     */
    @GetMapping("/stats/topics")
    public ResponseEntity<ApiResponse<List<StudyTopicStatsDto>>> getStudyTopicStats(Authentication authentication) {
        checkAdminRole(authentication);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getStudyTopicStats()));
    }
}
