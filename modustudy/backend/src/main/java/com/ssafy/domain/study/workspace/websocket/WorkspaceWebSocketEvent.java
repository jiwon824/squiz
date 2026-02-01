package com.ssafy.domain.study.workspace.websocket;

import com.ssafy.domain.study.workspace.dto.response.MessageResponse;

import java.time.LocalDateTime;

/**
 * 워크스페이스 WebSocket 이벤트 DTO
 */
public class WorkspaceWebSocketEvent {

    public enum Type {
        MESSAGE,        // 새 메시지
        TYPING,         // 입력 중
        JOIN,           // 워크스페이스 입장
        LEAVE,          // 워크스페이스 퇴장
        DELETE,         // 메시지 삭제
        UPDATE,         // 메시지 수정
        PRESENCE,       // 온라인/자리비움 상태
        PIN             // 메시지 고정/해제
    }

    private Type type;
    private Long workspaceId;
    private Long senderId;
    private String senderNickname;
    private String senderProfileImageUrl;
    private MessageResponse message;
    private Long messageId;  // 삭제/수정 시 사용
    private String presenceStatus; // ACTIVE | IDLE
    private LocalDateTime timestamp;

    public WorkspaceWebSocketEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public WorkspaceWebSocketEvent(Type type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 새 메시지 이벤트 생성
     */
    public static WorkspaceWebSocketEvent newMessage(MessageResponse message) {
        WorkspaceWebSocketEvent event = new WorkspaceWebSocketEvent(Type.MESSAGE);
        event.setMessage(message);
        event.setWorkspaceId(message.getWorkspaceId());
        event.setSenderId(message.getUserId());
        event.setSenderNickname(message.getNickname());
        event.setSenderProfileImageUrl(message.getProfileImageUrl());
        return event;
    }

    /**
     * 입력 중 이벤트 생성
     */
    public static WorkspaceWebSocketEvent typing(Long workspaceId, Long userId, String nickname) {
        WorkspaceWebSocketEvent event = new WorkspaceWebSocketEvent(Type.TYPING);
        event.setWorkspaceId(workspaceId);
        event.setSenderId(userId);
        event.setSenderNickname(nickname);
        return event;
    }

    /**
     * 입장 이벤트 생성
     */
    public static WorkspaceWebSocketEvent join(Long workspaceId, Long userId, String nickname, String profileImageUrl) {
        WorkspaceWebSocketEvent event = new WorkspaceWebSocketEvent(Type.JOIN);
        event.setWorkspaceId(workspaceId);
        event.setSenderId(userId);
        event.setSenderNickname(nickname);
        event.setSenderProfileImageUrl(profileImageUrl);
        return event;
    }

    /**
     * 퇴장 이벤트 생성
     */
    public static WorkspaceWebSocketEvent leave(Long workspaceId, Long userId, String nickname) {
        WorkspaceWebSocketEvent event = new WorkspaceWebSocketEvent(Type.LEAVE);
        event.setWorkspaceId(workspaceId);
        event.setSenderId(userId);
        event.setSenderNickname(nickname);
        return event;
    }

    /**
     * 메시지 삭제 이벤트 생성
     */
    public static WorkspaceWebSocketEvent deleteMessage(Long workspaceId, Long messageId, Long userId) {
        WorkspaceWebSocketEvent event = new WorkspaceWebSocketEvent(Type.DELETE);
        event.setWorkspaceId(workspaceId);
        event.setMessageId(messageId);
        event.setSenderId(userId);
        return event;
    }

    /**
     * 메시지 수정 이벤트 생성
     */
    public static WorkspaceWebSocketEvent updateMessage(MessageResponse message) {
        WorkspaceWebSocketEvent event = new WorkspaceWebSocketEvent(Type.UPDATE);
        event.setMessage(message);
        event.setWorkspaceId(message.getWorkspaceId());
        event.setMessageId(message.getId());
        event.setSenderId(message.getUserId());
        return event;
    }

    /**
     * 상태 변경 이벤트 생성
     */
    public static WorkspaceWebSocketEvent presence(Long workspaceId, Long userId, String nickname, String profileImageUrl, String status) {
        WorkspaceWebSocketEvent event = new WorkspaceWebSocketEvent(Type.PRESENCE);
        event.setWorkspaceId(workspaceId);
        event.setSenderId(userId);
        event.setSenderNickname(nickname);
        event.setSenderProfileImageUrl(profileImageUrl);
        event.setPresenceStatus(status);
        return event;
    }

    /**
     * 메시지 고정/해제 이벤트 생성
     */
    public static WorkspaceWebSocketEvent pinMessage(MessageResponse message, Long userId) {
        WorkspaceWebSocketEvent event = new WorkspaceWebSocketEvent(Type.PIN);
        event.setMessage(message);
        event.setWorkspaceId(message.getWorkspaceId());
        event.setMessageId(message.getId());
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

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
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

    public String getSenderProfileImageUrl() {
        return senderProfileImageUrl;
    }

    public void setSenderProfileImageUrl(String senderProfileImageUrl) {
        this.senderProfileImageUrl = senderProfileImageUrl;
    }

    public MessageResponse getMessage() {
        return message;
    }

    public void setMessage(MessageResponse message) {
        this.message = message;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getPresenceStatus() {
        return presenceStatus;
    }

    public void setPresenceStatus(String presenceStatus) {
        this.presenceStatus = presenceStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
