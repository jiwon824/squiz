package com.ssafy.domain.dm.websocket;

import com.ssafy.domain.dm.dto.response.DirectMessageResponse;

import java.time.LocalDateTime;

/**
 * DM WebSocket 이벤트 DTO
 */
public class DmWebSocketEvent {

    public enum Type {
        MESSAGE,        // 새 메시지
        READ,           // 읽음 처리
        TYPING,         // 입력 중
        ONLINE,         // 온라인 상태
        OFFLINE         // 오프라인 상태
    }

    private Type type;
    private Long conversationId;
    private Long senderId;
    private String senderNickname;
    private DirectMessageResponse message;
    private LocalDateTime timestamp;

    public DmWebSocketEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public DmWebSocketEvent(Type type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public static DmWebSocketEvent newMessage(DirectMessageResponse message) {
        DmWebSocketEvent event = new DmWebSocketEvent(Type.MESSAGE);
        event.setMessage(message);
        event.setConversationId(message.conversationId());
        event.setSenderId(message.senderId());
        event.setSenderNickname(message.senderNickname());
        return event;
    }

    public static DmWebSocketEvent read(Long conversationId, Long userId) {
        DmWebSocketEvent event = new DmWebSocketEvent(Type.READ);
        event.setConversationId(conversationId);
        event.setSenderId(userId);
        return event;
    }

    public static DmWebSocketEvent typing(Long conversationId, Long userId, String nickname) {
        DmWebSocketEvent event = new DmWebSocketEvent(Type.TYPING);
        event.setConversationId(conversationId);
        event.setSenderId(userId);
        event.setSenderNickname(nickname);
        return event;
    }

    public static DmWebSocketEvent online(Long userId) {
        DmWebSocketEvent event = new DmWebSocketEvent(Type.ONLINE);
        event.setSenderId(userId);
        return event;
    }

    public static DmWebSocketEvent offline(Long userId) {
        DmWebSocketEvent event = new DmWebSocketEvent(Type.OFFLINE);
        event.setSenderId(userId);
        return event;
    }

    // Getters and Setters
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public DirectMessageResponse getMessage() {
        return message;
    }

    public void setMessage(DirectMessageResponse message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
