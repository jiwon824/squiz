package com.ssafy.domain.user.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_social_account")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSocialAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(nullable = false, length = 100)
    private String providerUserId;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private Boolean isPrimary = false;

    private LocalDateTime linkedAt;

    // Google Calendar 연동을 위한 토큰 필드
    @Column(length = 2048)
    private String accessToken;

    @Column(length = 512)
    private String refreshToken;

    private LocalDateTime tokenExpiresAt;

    @Column(length = 255)
    @Builder.Default
    private String calendarId = "primary";

    // 토큰 업데이트 메서드
    public void updateTokens(String accessToken, String refreshToken, LocalDateTime expiresAt) {
        this.accessToken = accessToken;
        if (refreshToken != null) {
            this.refreshToken = refreshToken;
        }
        this.tokenExpiresAt = expiresAt;
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired() {
        return tokenExpiresAt == null || tokenExpiresAt.isBefore(LocalDateTime.now());
    }

    // 캘린더 연동 여부 확인
    public boolean hasCalendarAccess() {
        return refreshToken != null && !refreshToken.isEmpty();
    }
}
