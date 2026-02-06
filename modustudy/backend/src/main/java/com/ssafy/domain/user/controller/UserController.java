package com.ssafy.domain.user.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.common.response.MessageResponse;
import com.ssafy.domain.user.dto.request.ProfileSetupRequest;
import com.ssafy.domain.user.dto.request.StudyPreferenceRequest;
import com.ssafy.domain.user.dto.request.UserUpdateRequest;
import com.ssafy.domain.user.dto.response.StudyPreferenceResponse;
import com.ssafy.domain.user.dto.response.UserDTO;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.service.UserService;
import com.ssafy.domain.user.service.OAuth2Service;
import com.ssafy.domain.user.dto.response.StatsResponse;
import com.ssafy.domain.study.dto.response.StudySessionResponse;
import com.ssafy.domain.study.service.StudySessionService;
import com.ssafy.domain.calendar.dto.PersonalScheduleRequest;
import com.ssafy.domain.calendar.dto.PersonalScheduleResponse;
import com.ssafy.domain.calendar.service.PersonalScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 API Controller
 */
 @RestController
 @RequestMapping("/api/v1/users")
 @RequiredArgsConstructor
 public class UserController {

    private final UserService userService;
    private final OAuth2Service oAuth2Service;
    private final StudySessionService studySessionService;
    private final PersonalScheduleService personalScheduleService;

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getMyInfo(Authentication authentication) {
        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        User user = userService.getUserWithSocialAccounts(userId);

        return ResponseEntity.ok(
                ApiResponse.success(UserDTO.from(user))
        );
    }

    /**
     * 추가 정보 입력 (최초 로그인)
     * POST /api/v1/users/me/profile
     */
    @PostMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserDTO>> setupProfile(
            Authentication authentication,
            @RequestBody ProfileSetupRequest request) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        User updatedUser = userService.setupProfile(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(UserDTO.from(updatedUser))
        );
    }

    /**
     * 내 정보 수정
     * PUT /api/v1/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> updateMyInfo(
            Authentication authentication,
            @RequestBody UserUpdateRequest request) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        User updatedUser = userService.updateUserInfo(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(UserDTO.from(updatedUser))
        );
    }

    /**
     * 로그아웃
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            Authentication authentication) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        oAuth2Service.logout(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        MessageResponse.builder()
                                .message("로그아웃되었습니다.")
                                .build()
                )
        );
    }

    /**
     * 스터디 선호 설정 조회
     * GET /api/v1/users/me/study-preference
     */
    @GetMapping("/me/study-preference")
    public ResponseEntity<ApiResponse<StudyPreferenceResponse>> getStudyPreference(
            Authentication authentication) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        StudyPreferenceResponse response = userService.getStudyPreference(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 스터디 선호 설정 수정
     * PUT /api/v1/users/me/study-preference
     */
    @PutMapping("/me/study-preference")
    public ResponseEntity<ApiResponse<StudyPreferenceResponse>> updateStudyPreference(
            Authentication authentication,
            @RequestBody StudyPreferenceRequest request) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        StudyPreferenceResponse response = userService.updateStudyPreference(userId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 서비스 통계 조회 (메인 페이지용)
     * GET /api/v1/users/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsResponse>> getServiceStats() {
        StatsResponse stats = userService.getServiceStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 내 스터디 세션 조회 (기간별)
     * GET /api/v1/users/me/study-sessions?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     */
    @GetMapping("/me/study-sessions")
    public ResponseEntity<List<StudySessionResponse>> getMyStudySessions(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<StudySessionResponse> sessions = studySessionService.getMyStudySessions(userId, startDateTime, endDateTime);

        return ResponseEntity.ok(sessions);
    }

    /**
     * 프로필 이미지 업로드
     * POST /api/v1/users/me/profile-image
     */
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserDTO>> updateProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        User updatedUser = userService.updateProfileImage(userId, file);

        return ResponseEntity.ok(
                ApiResponse.success(UserDTO.from(updatedUser))
        );
    }

    /**
     * 프로필 이미지 삭제 (기본 이미지로 변경)
     * DELETE /api/v1/users/me/profile-image
     */
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<UserDTO>> deleteProfileImage(
            Authentication authentication) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        User updatedUser = userService.deleteProfileImage(userId);

        return ResponseEntity.ok(
                ApiResponse.success(UserDTO.from(updatedUser))
        );
    }

    // ==================== 개인 일정 API ====================

    /**
     * 개인 일정 목록 조회
     * GET /api/v1/users/me/schedules?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     */
    @GetMapping("/me/schedules")
    public ResponseEntity<ApiResponse<List<PersonalScheduleResponse>>> getMySchedules(
            Authentication authentication,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        List<PersonalScheduleResponse> schedules = personalScheduleService.getSchedules(userId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(schedules));
    }

    /**
     * 개인 일정 단건 조회
     * GET /api/v1/users/me/schedules/{scheduleId}
     */
    @GetMapping("/me/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<PersonalScheduleResponse>> getMySchedule(
            Authentication authentication,
            @PathVariable Long scheduleId) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        PersonalScheduleResponse schedule = personalScheduleService.getSchedule(userId, scheduleId);

        return ResponseEntity.ok(ApiResponse.success(schedule));
    }

    /**
     * 개인 일정 생성
     * POST /api/v1/users/me/schedules
     */
    @PostMapping("/me/schedules")
    public ResponseEntity<ApiResponse<PersonalScheduleResponse>> createMySchedule(
            Authentication authentication,
            @RequestBody PersonalScheduleRequest request) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        PersonalScheduleResponse schedule = personalScheduleService.createSchedule(userId, request);

        return ResponseEntity.ok(ApiResponse.success(schedule));
    }

    /**
     * 개인 일정 수정
     * PUT /api/v1/users/me/schedules/{scheduleId}
     */
    @PutMapping("/me/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<PersonalScheduleResponse>> updateMySchedule(
            Authentication authentication,
            @PathVariable Long scheduleId,
            @RequestBody PersonalScheduleRequest request) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        PersonalScheduleResponse schedule = personalScheduleService.updateSchedule(userId, scheduleId, request);

        return ResponseEntity.ok(ApiResponse.success(schedule));
    }

    /**
     * 개인 일정 삭제
     * DELETE /api/v1/users/me/schedules/{scheduleId}
     */
    @DeleteMapping("/me/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteMySchedule(
            Authentication authentication,
            @PathVariable Long scheduleId) {

        SsafyUserDetails userDetails = (SsafyUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        personalScheduleService.deleteSchedule(userId, scheduleId);

        return ResponseEntity.ok(ApiResponse.success(null, "일정이 삭제되었습니다."));
    }
}
