package com.ssafy.domain.user.dto.response;

import com.ssafy.domain.user.entity.SocialProvider;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.entity.UserSocialAccount;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String nickname;
    private String profileImage;
    private String loginProvider;
    private String role;

    // 기존 메서드 (다른 곳에서 사용 중일 수 있으니 유지)
    public static UserDTO from(User user) {
        String provider = null;
        if (user.getSocialAccounts() != null && !user.getSocialAccounts().isEmpty()) {
            provider = user.getSocialAccounts().stream()
                    .filter(UserSocialAccount::getIsPrimary)
                    .findFirst()
                    .map(account -> account.getProvider().name())
                    .orElse(null);
        }

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .loginProvider(provider)
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .build();
    }

    // 새로운 메서드 (OAuth2Service에서 사용)
    public static UserDTO from(User user, SocialProvider provider) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .loginProvider(provider != null ? provider.name() : null)
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .build();
    }
}
