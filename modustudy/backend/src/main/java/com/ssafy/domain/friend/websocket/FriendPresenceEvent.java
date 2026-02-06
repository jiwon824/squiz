package com.ssafy.domain.friend.websocket;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FriendPresenceEvent {
    private String type = "PRESENCE";
    private Long userId;
    private Boolean isOnline;
    private LocalDateTime lastSeenAt;

    public FriendPresenceEvent(Long userId, Boolean isOnline, LocalDateTime lastSeenAt) {
        this.userId = userId;
        this.isOnline = isOnline;
        this.lastSeenAt = lastSeenAt;
    }
}
