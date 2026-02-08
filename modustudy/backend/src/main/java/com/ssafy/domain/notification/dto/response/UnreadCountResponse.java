package com.ssafy.domain.notification.dto.response;

import com.ssafy.domain.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class UnreadCountResponse {

    private long unreadCount;
    private Map<NotificationType, Long> byType;

    public static UnreadCountResponse of(long unreadCount, Map<NotificationType, Long> byType) {
        return UnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .byType(byType)
                .build();
    }
}
