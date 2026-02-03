package com.ssafy.domain.gamification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 채팅 메시지 이벤트
 */
@Getter
@AllArgsConstructor
public class ChatMessageEvent {
    private final Long userId;
    private final Long studyId;
    private final String studyName;
    private final LocalDate chatDate;
}
