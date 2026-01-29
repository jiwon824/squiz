package com.ssafy.domain.calendar.controller;

import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.calendar.dto.CalendarEventResponse;
import com.ssafy.domain.calendar.dto.CalendarStatusResponse;
import com.ssafy.domain.calendar.entity.CalendarWatch;
import com.ssafy.domain.calendar.repository.CalendarWatchRepository;
import com.ssafy.domain.calendar.service.GoogleCalendarService;
import com.ssafy.domain.user.service.OAuth2Service;
import com.ssafy.domain.user.entity.SocialProvider;
import com.ssafy.domain.user.entity.UserSocialAccount;
import com.ssafy.domain.user.repository.UserSocialAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Tag(name = "Google Calendar", description = "Google Calendar 연동 API")
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;
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
            @AuthenticationPrincipal Long userId) {

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

    @PostMapping("/disconnect")
    @Operation(summary = "캘린더 연동 해제")
    public ResponseEntity<ApiResponse<Void>> disconnectCalendar(
            @AuthenticationPrincipal Long userId) {

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

        log.info("캘린더 연동 해제: userId={}", userId);

        return ResponseEntity.ok(ApiResponse.success(null, "캘린더 연동이 해제되었습니다."));
    }

    @GetMapping("/events")
    @Operation(summary = "캘린더 이벤트 조회")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getEvents(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<CalendarEventResponse> events = googleCalendarService.getEvents(userId, startTime, endTime);

        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PostMapping("/sync")
    @Operation(summary = "수동 동기화 트리거")
    public ResponseEntity<ApiResponse<Void>> triggerSync(
            @AuthenticationPrincipal Long userId) {

        // Watch가 등록되어 있지 않으면 등록
        if (calendarWatchRepository.findByUserId(userId).isEmpty()) {
            String webhookUrl = "https://i14d106.p.ssafy.io/api/v1/calendar/webhook";
            googleCalendarService.registerWatch(userId, webhookUrl);
        }

        log.info("수동 동기화 요청: userId={}", userId);

        return ResponseEntity.ok(ApiResponse.success(null, "동기화가 시작되었습니다."));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Google Calendar Webhook 수신")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Goog-Channel-ID", required = false) String channelId,
            @RequestHeader(value = "X-Goog-Resource-ID", required = false) String resourceId,
            @RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState) {

        log.info("Webhook 수신: channelId={}, resourceState={}", channelId, resourceState);

        if (channelId == null) {
            return ResponseEntity.ok().build();
        }

        // 동기화 토큰 검증은 resourceState가 "sync"가 아닐 때만
        if (!"sync".equals(resourceState)) {
            CalendarWatch watch = calendarWatchRepository.findByChannelId(channelId).orElse(null);

            if (watch != null && watch.getSyncToken() != null) {
                try {
                    googleCalendarService.incrementalSync(watch.getUserId(), watch.getSyncToken());
                    log.info("Incremental sync 완료: userId={}", watch.getUserId());
                } catch (Exception e) {
                    log.error("Incremental sync 실패: {}", e.getMessage());
                }
            }
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/watch/register")
    @Operation(summary = "Calendar Watch 등록 (Webhook)")
    public ResponseEntity<ApiResponse<CalendarWatch>> registerWatch(
            @AuthenticationPrincipal Long userId) {

        String webhookUrl = "https://i14d106.p.ssafy.io/api/v1/calendar/webhook";
        CalendarWatch watch = googleCalendarService.registerWatch(userId, webhookUrl);

        return ResponseEntity.ok(ApiResponse.success(watch));
    }

    @DeleteMapping("/watch")
    @Operation(summary = "Calendar Watch 해제")
    public ResponseEntity<ApiResponse<Void>> unregisterWatch(
            @AuthenticationPrincipal Long userId) {

        calendarWatchRepository.findByUserId(userId).ifPresent(watch -> {
            googleCalendarService.stopWatch(userId, watch.getChannelId(), watch.getResourceId());
            calendarWatchRepository.delete(watch);
        });

        return ResponseEntity.ok(ApiResponse.success(null, "Watch가 해제되었습니다."));
    }
}
