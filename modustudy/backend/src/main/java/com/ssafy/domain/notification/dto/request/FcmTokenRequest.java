package com.ssafy.domain.notification.dto.request;

import com.ssafy.domain.notification.entity.DeviceType;
import com.ssafy.domain.notification.entity.FcmToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmTokenRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다")
    private String token;

    @NotNull(message = "디바이스 타입은 필수입니다")
    private DeviceType deviceType;

    public FcmToken toEntity(Long userId) {
        return FcmToken.builder()
                .userId(userId)
                .token(token)
                .deviceType(deviceType)
                .isActive(true)
                .build();
    }
}
