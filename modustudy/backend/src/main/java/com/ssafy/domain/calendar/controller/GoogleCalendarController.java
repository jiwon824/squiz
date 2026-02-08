package com.ssafy.domain.calendar.controller;

import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.calendar.dto.*;
import com.ssafy.domain.calendar.entity.CalendarWatch;
import com.ssafy.domain.calendar.repository.CalendarWatchRepository;
import com.ssafy.domain.calendar.service.GoogleCalendarService;
import com.ssafy.domain.calendar.service.PersonalScheduleService;
import com.ssafy.domain.study.service.StudySessionService;
import com.ssafy.domain.study.dto.response.StudySessionResponse;
import com.ssafy.domain.user.service.OAuth2Service;
import com.ssafy.domain.user.entity.SocialProvider;
import com.ssafy.domain.user.entity.UserSocialAccount;
import com.ssafy.domain.user.repository.UserSocialAccountRepository;

import java.time.LocalDate;
import java.util.Collections;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ssafy.common.auth.CurrentUserId;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Tag(name = "Google Calendar", description = "Google Calendar 연동 API")
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final PersonalScheduleService personalScheduleService;
    private final StudySessionService studySessionService;
    private final OAuth2Service oAuth2Service;
    private final UserSocialAccountRepository socialAccountRepository;
    private final CalendarWatchRepository calendarWatchRepository;

    @GetMapping("/google/auth-url")
    @Operation(summary = "Google 캘린더 OAuth 인증 URL 조회")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> getGoogleCalendarAuthUrl() {
        String authUrl = oAuth2Service.getGoogleAuthUrl();
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("authUrl", authUrl)));
    }

    @GetMapping("/status")
    @Operation(summary = "캘린더 연동 상태 확인")
    public ResponseEntity<ApiResponse<CalendarStatusResponse>> getCalendarStatus(
            @CurrentUserId Long userId) {

        UserSocialAccount socialAccount = socialAccountRepository
                .findByUserIdAndProvider(userId, SocialProvider.GOOGLE)
                .orElse(null);

        CalendarStatusResponse response;

        if (socialAccount == null || !socialAccount.hasCalendarAccess()) {
            response = CalendarStatusResponse.builder()
                    .connected(false)
                    .hasValidToken(false)
                    .build();
        } else {
            response = CalendarStatusResponse.builder()
                    .connected(true)
                    .email(socialAccount.getEmail())
                    .tokenExpiresAt(socialAccount.getTokenExpiresAt())
                    .hasValidToken(!socialAccount.isTokenExpired())
                    .calendarId(socialAccount.getCalendarId())
                    .build();
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/connect")
    @Operation(summary = "Google 캘린더 연동 (Authorization Code로)")
    public ResponseEntity<ApiResponse<CalendarStatusResponse>> connectGoogleCalendar(
            @CurrentUserId Long userId,
            @RequestBody java.util.Map<String, String> request) {

        String code = request.get("code");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Authorization code가 필요합니다.");
        }

// OAuth2Service를 통해 Google 계정 연동 (Calendar scope 포함된 토큰 저장)
        oAuth2Service.linkSocialAccount(userId, SocialProvider.GOOGLE, code);

        // 연동 완료 후 상태 반환
        UserSocialAccount socialAccount = socialAccountRepository
                .findByUserIdAndProvider(userId, SocialProvider.GOOGLE)
                .orElseThrow(() -> new IllegalStateException("Google 계정 연동에 실패했습니다."));

        CalendarStatusResponse response = CalendarStatusResponse.builder()
                .connected(true)
                .email(socialAccount.getEmail())
                .tokenExpiresAt(socialAccount.getTokenExpiresAt())
                .hasValidToken(!socialAccount.isTokenExpired())
                .calendarId(socialAccount.getCalendarId())
                .build();

                return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/connect/mobile")
    @Operation(summary = "Google 캘린더 연동 - 모바일용 (redirect_uri 없이)")
    public ResponseEntity<ApiResponse<CalendarStatusResponse>> connectGoogleCalendarMobile(
            @CurrentUserId Long userId,
            @RequestBody java.util.Map<String, String> request) {

        String code = request.get("code");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Authorization code가 필요합니다.");
        }

// 모바일용 연동 (redirect_uri 없이)
        oAuth2Service.linkSocialAccountMobile(userId, SocialProvider.GOOGLE, code);

        // 연동 완료 후 상태 반환
        UserSocialAccount socialAccount = socialAccountRepository
                .findByUserIdAndProvider(userId, SocialProvider.GOOGLE)
                .orElseThrow(() -> new IllegalStateException("Google 계정 연동에 실패했습니다."));

        CalendarStatusResponse response = CalendarStatusResponse.builder()
                .connected(true)
                .email(socialAccount.getEmail())
                .tokenExpiresAt(socialAccount.getTokenExpiresAt())
                .hasValidToken(!socialAccount.isTokenExpired())
                .calendarId(socialAccount.getCalendarId())
                .build();

                return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/disconnect")
    @Operation(summary = "캘린더 연동 해제")
    public ResponseEntity<ApiResponse<Void>> disconnectCalendar(
            @CurrentUserId Long userId) {

        UserSocialAccount socialAccount = socialAccountRepository
                .findByUserIdAndProvider(userId, SocialProvider.GOOGLE)
                .orElseThrow(() -> new IllegalArgumentException("Google 계정이 연동되어 있지 않습니다."));

        // Watch가 있으면 중지
        calendarWatchRepository.findByUserId(userId).ifPresent(watch -> {
            googleCalendarService.stopWatch(userId, watch.getChannelId(), watch.getResourceId());
            calendarWatchRepository.delete(watch);
        });

        // 토큰 삭제
        socialAccount.setAccessToken(null);
        socialAccount.setRefreshToken(null);
        socialAccount.setTokenExpiresAt(null);
        socialAccountRepository.save(socialAccount);

        return ResponseEntity.ok(ApiResponse.success(null, "캘린더 연동이 해제되었습니다."));
    }

    @GetMapping("/events")
    @Operation(summary = "캘린더 이벤트 조회")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getEvents(
            @CurrentUserId Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<CalendarEventResponse> events = googleCalendarService.getEvents(userId, startTime, endTime);

        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PostMapping("/sync")
    @Operation(summary = "수동 동기화 트리거")
    public ResponseEntity<ApiResponse<Void>> triggerSync(
            @CurrentUserId Long userId) {

        // Watch가 등록되어 있지 않으면 등록 시도 (실패해도 무시 - localhost에서는 webhook 불가)
        if (calendarWatchRepository.findByUserId(userId).isEmpty()) {
            try {
                String webhookUrl = "https://i14d106.p.ssafy.io/api/v1/calendar/webhook";
                googleCalendarService.registerWatch(userId, webhookUrl);
            } catch (Exception e) {
}
        }

        return ResponseEntity.ok(ApiResponse.success(null, "동기화가 시작되었습니다."));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Google Calendar Webhook 수신")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Goog-Channel-ID", required = false) String channelId,
            @RequestHeader(value = "X-Goog-Resource-ID", required = false) String resourceId,
            @RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState) {

                if (channelId == null) {
            return ResponseEntity.ok().build();
        }

        // 동기화 토큰 검증은 resourceState가 "sync"가 아닐 때만
        if (!"sync".equals(resourceState)) {
            CalendarWatch watch = calendarWatchRepository.findByChannelId(channelId).orElse(null);

            if (watch != null && watch.getSyncToken() != null) {
                try {
                    googleCalendarService.incrementalSync(watch.getUserId(), watch.getSyncToken());
} catch (Exception e) {
}
            }
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/watch/register")
    @Operation(summary = "Calendar Watch 등록 (Webhook)")
    public ResponseEntity<ApiResponse<CalendarWatch>> registerWatch(
            @CurrentUserId Long userId) {

        String webhookUrl = "https://i14d106.p.ssafy.io/api/v1/calendar/webhook";
        CalendarWatch watch = googleCalendarService.registerWatch(userId, webhookUrl);

        return ResponseEntity.ok(ApiResponse.success(watch));
    }

    @DeleteMapping("/watch")
    @Operation(summary = "Calendar Watch 해제")
    public ResponseEntity<ApiResponse<Void>> unregisterWatch(
            @CurrentUserId Long userId) {

        calendarWatchRepository.findByUserId(userId).ifPresent(watch -> {
            googleCalendarService.stopWatch(userId, watch.getChannelId(), watch.getResourceId());
            calendarWatchRepository.delete(watch);
        });

        return ResponseEntity.ok(ApiResponse.success(null, "Watch가 해제되었습니다."));
    }

    @GetMapping("/all")
    @Operation(summary = "모든 일정 통합 조회 (개인 + 스터디 + Google)")
    public ResponseEntity<ApiResponse<AllSchedulesResponse>> getAllSchedules(
            @CurrentUserId Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

// 1. 개인 일정 조회
        List<PersonalScheduleResponse> personalSchedules = personalScheduleService.getSchedules(userId, startDate, endDate);

        // 2. 스터디 세션 조회
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<StudySessionResponse> studySessions = studySessionService.getMyStudySessions(
                userId,
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay()
        );

        // 스터디 세션을 캘린더용 DTO로 변환
        List<com.ssafy.domain.calendar.dto.StudySessionResponse> calendarSessions = studySessions.stream()
                .map(s -> com.ssafy.domain.calendar.dto.StudySessionResponse.builder()
                        .id(s.getId())
                        .studyId(s.getStudyId())
                        .sessionNumber(s.getSessionNumber())
                        .title(s.getTitle())
                        .description(s.getDescription())
                        .scheduledAt(s.getScheduledAt() != null ? s.getScheduledAt().toString() : null)
                        .durationMinutes(s.getDurationMinutes())
                        .location(s.getLocation())
                        .isOnline(s.getIsOnline())
                        .status(s.getStatus() != null ? s.getStatus().name() : null)
                        .completedAt(s.getCompletedAt() != null ? s.getCompletedAt().toString() : null)
                        .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null)
                        .build())
                .toList();

        // 3. Google Calendar 이벤트 조회 (연동된 경우에만)
        List<CalendarEventResponse> googleEvents = Collections.emptyList();
        try {
            UserSocialAccount socialAccount = socialAccountRepository
                    .findByUserIdAndProvider(userId, SocialProvider.GOOGLE)
                    .orElse(null);

            // 디버그 로깅
            if (socialAccount == null) {
} else {
}

            if (socialAccount != null && socialAccount.hasCalendarAccess() && !socialAccount.isTokenExpired()) {
                LocalDateTime startDateTime = start.atStartOfDay();
                LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();
                googleEvents = googleCalendarService.getEvents(userId, startDateTime, endDateTime);
}
        } catch (Exception e) {
}

        AllSchedulesResponse response = AllSchedulesResponse.builder()
                .personal(personalSchedules)
                .studySessions(calendarSessions)
                .googleEvents(googleEvents)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

