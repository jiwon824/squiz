package com.ssafy.domain.notification.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingUpdateRequest {

    @NotEmpty(message = "설정 목록은 비어있을 수 없습니다")
    @Valid
    private List<SettingItem> settings;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SettingItem {

        @NotNull(message = "알림 타입은 필수입니다")
        private String type;

        @NotNull(message = "활성화 여부는 필수입니다")
        private Boolean isEnabled;
    }
}
