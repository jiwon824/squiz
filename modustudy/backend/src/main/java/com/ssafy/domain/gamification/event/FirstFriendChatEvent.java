package com.ssafy.domain.gamification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 친구와의 첫 채팅 이벤트
 * 친구 관계에서 처음으로 DM을 보낼 때 발행
 */
 @Getter
 @AllArgsConstructor
 public class FirstFriendChatEvent {
    private final Long userId;        // 메시지를 보낸 사용자
    private final Long friendId;      // 메시지를 받은 친구
    private final Long conversationId;
    private final LocalDate chatDate;
}
