package com.ssafy.domain.user.service;

import com.ssafy.common.util.JwtTokenUtil;
import com.ssafy.domain.user.dto.OAuth2UserInfo;
import com.ssafy.domain.user.dto.response.AuthResponse;
import com.ssafy.domain.user.dto.response.UserDTO;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.*;
import com.ssafy.domain.user.repository.RefreshTokenRepository;
import com.ssafy.domain.user.repository.UserRepository;
import com.ssafy.domain.user.repository.UserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.ssafy.domain.user.entity.PasswordResetToken;
import com.ssafy.domain.user.repository.PasswordResetTokenRepository;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OAuth2Service {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final RestTemplate restTemplate;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;


    // ==================== 카카오 ====================

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri:http://localhost:8080/login/oauth2/code/kakao}")
    private String kakaoRedirectUri;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * 카카오 로그인 URL 생성
     */
    public String getKakaoAuthUrl() {
        return "https://kauth.kakao.com/oauth/authorize?" +
                "client_id=" + kakaoClientId +
                "&redirect_uri=" + kakaoRedirectUri +
                "&response_type=code" +
                "&scope=profile_image,account_email";
    }

    /**
     * 카카오 로그인 콜백 처리
     */
    public AuthResponse processKakaoCallback(String code) {
        log.info("카카오 로그인 콜백 처리 시작");

        // 1. Authorization Code로 Access Token 받기
        String kakaoAccessToken = getKakaoAccessToken(code);

        // 2. Access Token으로 사용자 정보 받기
        OAuth2UserInfo userInfo = getKakaoUserInfo(kakaoAccessToken);

        // 3. 이메일 기반 User 찾기 또는 생성 + 소셜 계정 연동
        User user = findOrCreateUserWithSocial(userInfo, SocialProvider.KAKAO);

        // 4. 신규 유저 여부 판단 (nickname이 없으면 추가 정보 입력 필요)
        boolean isNewUser = (user.getNickname() == null);

        // 5. JWT 토큰 발급
        String accessToken = jwtTokenUtil.createAccessToken(String.valueOf(user.getId()));
        String refreshToken = jwtTokenUtil.createRefreshToken(String.valueOf(user.getId()));

        // 6. Refresh Token 저장
        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .isNewUser(isNewUser)
                .user(UserDTO.from(user, SocialProvider.KAKAO))
                .loginProvider("KAKAO")
                .build();
    }

    /**
     * Authorization Code로 Access Token 받기 (카카오)
     */
    private String getKakaoAccessToken(String code) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    /**
     * Access Token으로 사용자 정보 받기 (카카오)
     */
    private OAuth2UserInfo getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                Map.class
        );

        return OAuth2UserInfo.fromKakao(response.getBody());
    }

    // ==================== 네이버 ====================

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri:http://localhost:8080/login/oauth2/code/naver}")
    private String naverRedirectUri;

    /**
     * 네이버 로그인 URL 생성
     */
    public String getNaverAuthUrl() {
        return "https://nid.naver.com/oauth2.0/authorize?" +
                "client_id=" + naverClientId +
                "&redirect_uri=" + naverRedirectUri +
                "&response_type=code" +
                "&state=random_state_string";
    }

    /**
     * 네이버 로그인 콜백 처리
     */
    public AuthResponse processNaverCallback(String code, String state) {
        log.info("네이버 로그인 콜백 처리 시작");

        // 1. Authorization Code로 Access Token 받기
        String naverAccessToken = getNaverAccessToken(code, state);

        // 2. Access Token으로 사용자 정보 받기
        OAuth2UserInfo userInfo = getNaverUserInfo(naverAccessToken);

        // 3. 이메일 기반 User 찾기 또는 생성 + 소셜 계정 연동
        User user = findOrCreateUserWithSocial(userInfo, SocialProvider.NAVER);

        // 4. 신규 유저 여부 판단 (nickname이 없으면 추가 정보 입력 필요)
        boolean isNewUser = (user.getNickname() == null);

        // 5. JWT 토큰 발급
        String accessToken = jwtTokenUtil.createAccessToken(String.valueOf(user.getId()));
        String refreshToken = jwtTokenUtil.createRefreshToken(String.valueOf(user.getId()));

        // 6. Refresh Token 저장
        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .isNewUser(isNewUser)
                .user(UserDTO.from(user, SocialProvider.NAVER))
                .loginProvider("NAVER")
                .build();
    }

    /**
     * Authorization Code로 Access Token 받기 (네이버)
     */
    private String getNaverAccessToken(String code, String state) {
        String tokenUrl = "https://nid.naver.com/oauth2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", naverClientId);
        params.add("client_secret", naverClientSecret);
        params.add("code", code);
        params.add("state", state);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        Map<String, Object> body = response.getBody();

        if (body.containsKey("error")) {
            throw new RuntimeException("네이버 토큰 발급 실패: " + body.get("error_description"));
        }

        return (String) body.get("access_token");
    }

    /**
     * Access Token으로 사용자 정보 받기 (네이버)
     */
    private OAuth2UserInfo getNaverUserInfo(String accessToken) {
        String userInfoUrl = "https://openapi.naver.com/v1/nid/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                Map.class
        );

        return OAuth2UserInfo.fromNaver(response.getBody());
    }

    // ==================== 구글 ====================

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:http://localhost:8080/login/oauth2/code/google}")
    private String googleRedirectUri;

    /**
     * 구글 로그인 URL 생성
     */
    public String getGoogleAuthUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + googleClientId +
                "&redirect_uri=" + googleRedirectUri +
                "&response_type=code" +
                "&scope=openid%20profile%20email" +
                "&access_type=offline" +
                "&prompt=consent";
    }

    /**
     * 구글 로그인 콜백 처리
     */
    public AuthResponse processGoogleCallback(String code) {
        log.info("구글 로그인 콜백 처리 시작");

        // 1. Authorization Code로 Access Token 받기
        String googleAccessToken = getGoogleAccessToken(code);

        // 2. Access Token으로 사용자 정보 받기
        OAuth2UserInfo userInfo = getGoogleUserInfo(googleAccessToken);

        // 3. 이메일 기반 User 찾기 또는 생성 + 소셜 계정 연동
        User user = findOrCreateUserWithSocial(userInfo, SocialProvider.GOOGLE);

        // 4. 신규 유저 여부 판단 (nickname이 없으면 추가 정보 입력 필요)
        boolean isNewUser = (user.getNickname() == null);

        // 5. JWT 토큰 발급
        String accessToken = jwtTokenUtil.createAccessToken(String.valueOf(user.getId()));
        String refreshToken = jwtTokenUtil.createRefreshToken(String.valueOf(user.getId()));

        // 6. Refresh Token 저장
        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .isNewUser(isNewUser)
                .user(UserDTO.from(user, SocialProvider.GOOGLE))
                .loginProvider("GOOGLE")
                .build();
    }

    /**
     * Authorization Code로 Access Token 받기 (구글)
     */
    private String getGoogleAccessToken(String code) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        Map<String, Object> body = response.getBody();

        if (body.containsKey("error")) {
            throw new RuntimeException("구글 토큰 발급 실패: " + body.get("error_description"));
        }

        return (String) body.get("access_token");
    }

    /**
     * Access Token으로 사용자 정보 받기 (구글)
     */
    private OAuth2UserInfo getGoogleUserInfo(String accessToken) {
        String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                Map.class
        );

        return OAuth2UserInfo.fromGoogle(response.getBody());
    }

    // ==================== 공통 헬퍼 메서드 ====================

    /**
     * 이메일로 User 찾기 또는 생성 + 소셜 계정 연동
     *
     * 핵심 로직:
     * 1. 이메일로 기존 User 찾기
     * 2. User 있으면 → 해당 provider로 소셜 계정 추가
     * 3. User 없으면 → 신규 User + 소셜 계정 생성
     */
    private User findOrCreateUserWithSocial(OAuth2UserInfo userInfo, SocialProvider provider) {

        // 1. 이메일로 기존 User 찾기
        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());

        User user;

        if (existingUser.isPresent()) {
            // ===== 기존 User 있음 =====
            user = existingUser.get();

            log.info("기존 회원 발견: userId={}, email={}", user.getId(), user.getEmail());

            // 해당 provider로 이미 연동되었는지 확인
            Optional<UserSocialAccount> existingSocial = socialAccountRepository
                    .findByProviderAndProviderUserId(provider, userInfo.getProviderId());

            if (existingSocial.isEmpty()) {
                // 새로운 소셜 계정 추가!
                UserSocialAccount newSocial = UserSocialAccount.builder()
                        .user(user)
                        .provider(provider)
                        .providerUserId(userInfo.getProviderId())
                        .email(userInfo.getEmail())
                        .isPrimary(false)  // 추가 연동이므로 primary 아님
                        .linkedAt(LocalDateTime.now())
                        .build();

                socialAccountRepository.save(newSocial);
                log.info("소셜 계정 추가 연동: userId={}, provider={}", user.getId(), provider);
            } else {
                log.info("이미 연동된 소셜 계정: userId={}, provider={}", user.getId(), provider);
            }

        } else {
            // ===== 신규 User 생성 =====
            user = User.builder()
                    .email(userInfo.getEmail())
                    .name(userInfo.getName())
                    .nickname(null)  // 추가 정보 입력 필요
                    .profileImage(userInfo.getProfileImageUrl())
                    .role(Role.USER)
                    .isActive(true)
                    .lastLoginAt(LocalDateTime.now())
                    .isOnline(true)
                    .isSearchable(true)
                    .totalExp(0)
                    .currentPoints(0)
                    .currentLevel(1)
                    .levelName("새싹")
                    .build();

            user = userRepository.save(user);

            // 소셜 계정 생성
            UserSocialAccount socialAccount = UserSocialAccount.builder()
                    .user(user)
                    .provider(provider)
                    .providerUserId(userInfo.getProviderId())
                    .email(userInfo.getEmail())
                    .isPrimary(true)  // 첫 연동이므로 primary
                    .linkedAt(LocalDateTime.now())
                    .build();

            socialAccountRepository.save(socialAccount);
            log.info("신규 회원 가입: userId={}, provider={}", user.getId(), provider);
        }

        // 로그인 상태 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        user.setIsOnline(true);
        userRepository.save(user);

        return user;
    }

    /**
     * Refresh Token 저장
     */
    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isRevoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Access Token 갱신
     */
    public String refreshAccessToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (token.getIsRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Refresh token expired");
        }

        return jwtTokenUtil.createAccessToken(String.valueOf(token.getUser().getId()));
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setIsOnline(false);
        user.setLastSeenAt(LocalDateTime.now());

        userRepository.save(user);
    }

    /**
     * 일반 로그인 (이메일 + 비밀번호)
     */
    public AuthResponse login(String email, String password) {
        // 1. 이메일로 사용자 찾기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다."));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        // 3. 계정 활성화 확인
        if (!user.getIsActive()) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        // 4. 로그인 상태 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        user.setIsOnline(true);
        userRepository.save(user);

        // 5. JWT 토큰 발급
        String accessToken = jwtTokenUtil.createAccessToken(String.valueOf(user.getId()));
        String refreshToken = jwtTokenUtil.createRefreshToken(String.valueOf(user.getId()));

        // 6. Refresh Token 저장
        saveRefreshToken(user, refreshToken);

        // 7. UserDTO 생성 (loginProvider는 일반 로그인이므로 "EMAIL")
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .loginProvider("EMAIL")  // 일반 로그인
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .isNewUser(false)  // 기존 회원
                .user(userDTO)
                .build();
    }
    /**
     * 비밀번호 재설정 요청 - 이메일 전송
     */
    @Transactional
    public void requestPasswordReset(String email) {
        // 1. 이메일로 사용자 찾기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 계정이 없습니다."));

        // 2. 비밀번호가 설정되지 않은 경우 체크
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalStateException("비밀번호가 설정되지 않은 계정입니다. 프로필 설정을 먼저 완료해주세요.");
        }

        // 3. 기존에 미사용 토큰이 있으면 삭제
        passwordResetTokenRepository.findByUserAndUsedFalse(user)
                .ifPresent(passwordResetTokenRepository::delete);

        // 4. 새 토큰 생성 (30분 유효)
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // 5. 이메일 전송
        String resetLink = frontendUrl + "/password/reset?token=" + token;
        emailService.sendPasswordResetEmail(email, resetLink);
    }

    /**
     * 토큰 유효성 검증
     */
    @Transactional(readOnly = true)
    public boolean verifyResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElse(null);

        return resetToken != null && resetToken.isValid();
    }

    /**
     * 비밀번호 재설정 실행
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // 1. 토큰 조회
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        // 2. 토큰 유효성 검증
        if (!resetToken.isValid()) {
            throw new IllegalStateException("만료되었거나 이미 사용된 토큰입니다.");
        }

        // 3. 비밀번호 변경
        User user = resetToken.getUser();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);
        userRepository.save(user);

        // 4. 토큰 사용 처리
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        // 5. 해당 사용자의 모든 리프레시 토큰 삭제 (보안)
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}