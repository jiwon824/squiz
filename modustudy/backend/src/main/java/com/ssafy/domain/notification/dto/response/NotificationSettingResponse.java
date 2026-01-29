package com.ssafy.domain.notification.dto.response;

import com.ssafy.domain.notification.entity.NotificationSetting;
import com.ssafy.domain.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSettingResponse {

    private String type;
    private String typeName;
    private Boolean isEnabled;

    public static NotificationSettingResponse from(NotificationSetting entity) {
        NotificationType type = NotificationType.valueOf(entity.getNotificationType());
        return NotificationSettingResponse.builder()
                .type(entity.getNotificationType())
                .typeName(type.getDisplayName())
                .isEnabled(entity.getIsEnabled())
                .build();
    }

    public static NotificationSettingResponse of(NotificationType type, Boolean isEnabled) {
        return NotificationSettingResponse.builder()
                .type(type.name())
                .typeName(type.getDisplayName())
                .isEnabled(isEnabled)
                .build();
    }
}