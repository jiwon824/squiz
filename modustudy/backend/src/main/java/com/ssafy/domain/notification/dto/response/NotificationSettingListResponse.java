package com.ssafy.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationSettingListResponse {

    private List<NotificationSettingResponse> settings;

    public static NotificationSettingListResponse of(List<NotificationSettingResponse> settings) {
        return NotificationSettingListResponse.builder()
                .settings(settings)
                .build();
    }
}
