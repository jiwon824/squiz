package com.ssafy.domain.user.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuth2CallbackRequest {
    private String code;
}
