package com.ssafy.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmTokenDeleteRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다")
    private String token;
}