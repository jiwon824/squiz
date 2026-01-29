package com.ssafy.domain.user.controller;

import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.user.dto.request.OAuth2CallbackRequest;
import com.ssafy.domain.user.dto.request.TokenRefreshRequest;
import com.ssafy.domain.user.dto.response.AuthResponse;
import com.ssafy.domain.user.dto.response.TokenRefreshResponse;
import com.ssafy.domain.user.service.OAuth2Service;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ssafy.domain.user.dto.request.LoginRequest;

import com.ssafy.common.response.MessageResponse;
import com.ssafy.domain.user.dto.request.PasswordResetRequest;
import com.ssafy.domain.user.dto.request.PasswordResetConfirmRequest;

import com.ssafy.domain.user.dto.request.SocialLinkRequest;
import com.ssafy.domain.user.dto.response.LinkedAccountsResponse;
import com.ssafy.domain.user.dto.response.SocialAccountResponse;
import com.ssafy.domain.user.entity.SocialProvider;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuth2Service oAuth2Service;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * [임시] 비밀번호 해시 생성 - 나중에 삭제
     * GET /api/v1/auth/hash?password=password123
     */
    @GetMapping("/hash")
    public ResponseEntity<String> generateHash(@RequestParam String password) {
        return ResponseEntity.ok(passwordEncoder.encode(password));
    }

    /**
     * 1. 소셜 로그인 URL 요청
     * GET /api/v1/auth/oauth/{provider}
     */
    @GetMapping("/oauth/{provider}")
    public ResponseEntity<ApiResponse<AuthUrlResponse>> getAuthorizationUrl(
            @PathVariable String provider) {

        String authUrl;

        switch (provider.toLowerCase()) {
            case "kakao":
                authUrl = oAuth2Service.getKakaoAuthUrl();
                break;
            case "naver":
                authUrl = oAuth2Service.getNaverAuthUrl();
                break;
            case "google":
                authUrl = oAuth2Service.getGoogleAuthUrl();
                break;
            default:
                return ResponseEntity
                        .badRequest()
                        .body(ApiResponse.error("INVALID_PROVIDER", "지원하지 않는 OAuth 제공자입니다."));
        }

        AuthUrlResponse response = new AuthUrlResponse(authUrl);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 2. 소셜 로그인 콜백 처리
     * POST /api/v1/auth/oauth/{provider}/callback
     */
    @PostMapping("/oauth/{provider}/callback")
    public ResponseEntity<ApiResponse<AuthResponse>> handleCallback(
            @PathVariable String provider,
            @RequestBody OAuth2CallbackRequest request,
            @RequestParam(required = false) String state) {  // 네이버용 state 파라미터

        AuthResponse response;

        switch (provider.toLowerCase()) {
            case "kakao":
                response = oAuth2Service.processKakaoCallback(request.getCode());
                break;
            case "naver":
                response = oAuth2Service.processNaverCallback(request.getCode(), state);
                break;
            case "google":
                response = oAuth2Service.processGoogleCallback(request.getCode());
                break;
            default:
                return ResponseEntity
                        .badRequest()
                        .body(ApiResponse.error("INVALID_PROVIDER", "지원하지 않는 OAuth 제공자입니다."));
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 3. Access Token 갱신
     * POST /api/v1/auth/token/refresh
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @RequestBody TokenRefreshRequest request) {

        String newAccessToken = oAuth2Service.refreshAccessToken(request.getRefreshToken());

        TokenRefreshResponse response = TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(3600)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내부 클래스: AuthUrlResponse
     */
    @Getter
    @AllArgsConstructor
    private static class AuthUrlResponse {
        private String authUrl;
    }

    /**
     * 일반 로그인 (이메일 + 비밀번호)
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody LoginRequest request) {

        AuthResponse response = oAuth2Service.login(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
    /**
     * 비밀번호 재설정 요청 (이메일 전송)
     * POST /api/v1/auth/password/reset-request
     */
    @PostMapping("/password/reset-request")
    public ResponseEntity<ApiResponse<MessageResponse>> requestPasswordReset(
            @RequestBody PasswordResetRequest request) {

        oAuth2Service.requestPasswordReset(request.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success("비밀번호 재설정 이메일이 전송되었습니다.")
        );
    }

    /**
     * 토큰 유효성 검증
     * GET /api/v1/auth/password/reset/verify?token=xxxxx
     */
    @GetMapping("/password/reset/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyResetToken(
            @RequestParam String token) {

        boolean isValid = oAuth2Service.verifyResetToken(token);

        return ResponseEntity.ok(ApiResponse.success(isValid));
    }

    /**
     * 비밀번호 재설정 실행
     * POST /api/v1/auth/password/reset
     */
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @RequestBody PasswordResetConfirmRequest request) {

        oAuth2Service.resetPassword(request.getToken(), request.getNewPassword());

        return ResponseEntity.ok(
                ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.")
        );
    }
    /**
     * 8. 연동된 소셜 계정 목록 조회
     * GET /api/v1/auth/social/my
     */
    @GetMapping("/social/my")
    public ResponseEntity<ApiResponse<LinkedAccountsResponse>> getLinkedAccounts(
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long userId) {

        if (userId == null) {
            return ResponseEntity
                    .status(401)
                    .body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다."));
        }

        LinkedAccountsResponse response = oAuth2Service.getLinkedSocialAccounts(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 9. 소셜 계정 연동 추가
     * POST /api/v1/auth/social/{provider}/link
     */
    @PostMapping("/social/{provider}/link")
    public ResponseEntity<ApiResponse<SocialAccountResponse>> linkSocialAccount(
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long userId,
            @PathVariable String provider,
            @RequestBody SocialLinkRequest request) {

        // provider 문자열을 enum으로 변환
        SocialProvider socialProvider;
        try {
            socialProvider = SocialProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("INVALID_PROVIDER", "지원하지 않는 OAuth 제공자입니다."));
        }

        SocialAccountResponse response = oAuth2Service.linkSocialAccount(
                userId,
                socialProvider,
                request.getCode()
        );

        // 메시지 없이 data만 반환
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    /**
     * 10. 소셜 계정 연동 해제
     * DELETE /api/v1/auth/social/{provider}
     */
    @DeleteMapping("/social/{provider}")
    public ResponseEntity<ApiResponse<MessageResponse>> unlinkSocialAccount(
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long userId,
            @PathVariable String provider) {

        // provider 문자열을 enum으로 변환
        SocialProvider socialProvider;
        try {
            socialProvider = SocialProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("INVALID_PROVIDER", "지원하지 않는 OAuth 제공자입니다."));
        }

        oAuth2Service.unlinkSocialAccount(userId, socialProvider);

        String providerName;
        switch (socialProvider) {
            case GOOGLE:
                providerName = "구글";
                break;
            case KAKAO:
                providerName = "카카오";
                break;
            case NAVER:
                providerName = "네이버";
                break;
            default:
                providerName = provider;
        }

        return ResponseEntity.ok(
                ApiResponse.success(providerName + " 계정 연동이 해제되었습니다.")
        );
    }
}