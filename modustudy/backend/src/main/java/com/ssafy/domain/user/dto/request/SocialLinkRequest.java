package com.ssafy.domain.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소셜 계정 연동 요청
 */
@Getter
@NoArgsConstructor
public class SocialLinkRequest {
    private String code;  // OAuth authorization code
}