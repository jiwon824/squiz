package com.ssafy.domain.user.dto;

import com.ssafy.domain.user.entity.SocialProvider;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuth2UserInfo {
    private String providerId;
    private String email;
    private String name;
    private String nickname;
    private String profileImageUrl;
    private SocialProvider provider;

    public static OAuth2UserInfo fromKakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuth2UserInfo.builder()
                .providerId(String.valueOf(attributes.get("id")))
                .email((String) kakaoAccount.get("email"))
                .name((String) kakaoAccount.get("name"))
                .nickname((String) profile.get("nickname"))
                .profileImageUrl((String) profile.get("profile_image_url"))
                .provider(SocialProvider.KAKAO)
                .build();
    }

    public static OAuth2UserInfo fromNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuth2UserInfo.builder()
                .providerId((String) response.get("id"))
                .email((String) response.get("email"))
                .name((String) response.get("name"))
                .profileImageUrl((String) response.get("profile_image"))
                .build();
    }
    /**
     * Google 사용자 정보 파싱
     */
    public static OAuth2UserInfo fromGoogle(Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
                .providerId((String) attributes.get("sub"))  // Google의 고유 ID
                .email((String) attributes.get("email"))
                .name((String) attributes.get("name"))
                .profileImageUrl((String) attributes.get("picture"))
                .build();
    }
}
