package com.ssafy.domain.notification.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class NotificationListResponse {

    private List<NotificationResponse> content;
    private int page;
    private long totalElements;
    private long unreadCount;

    public static NotificationListResponse of(Page<NotificationResponse> page, long unreadCount) {
        return NotificationListResponse.builder()
                .content(page.getContent())
                .page(page.getNumber())
                .totalElements(page.getTotalElements())
                .unreadCount(unreadCount)
                .build();
    }
}
