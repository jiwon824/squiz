package com.ssafy.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private Boolean isNewUser;
    private UserDTO user;
    private String loginProvider;
}
