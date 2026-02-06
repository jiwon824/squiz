package com.ssafy.domain.user.dto.response;

import com.ssafy.domain.user.entity.SocialProvider;
import com.ssafy.domain.user.entity.UserSocialAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 소셜 계정 정보 응답
 */
 @Getter
 @Builder
 @AllArgsConstructor
 public class SocialAccountResponse {
    private SocialProvider provider;  // GOOGLE, KAKAO, NAVER
    private String email;
    private LocalDateTime linkedAt;
    private Boolean isPrimary;  // 주 계정 여부

    /**
     * Entity -> DTO 변환
     */
    public static SocialAccountResponse from(UserSocialAccount socialAccount) {
        return SocialAccountResponse.builder()
                .provider(socialAccount.getProvider())
                .email(socialAccount.getEmail())
                .linkedAt(socialAccount.getLinkedAt())
                .isPrimary(socialAccount.getIsPrimary())
                .build();
    }
}
