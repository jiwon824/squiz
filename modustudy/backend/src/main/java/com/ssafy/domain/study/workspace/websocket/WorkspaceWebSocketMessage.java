package com.ssafy.domain.study.workspace.websocket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 워크스페이스 WebSocket 메시지 전송용 DTO
 */
 public class WorkspaceWebSocketMessage {

    @NotNull
    private Long workspaceId;

    @NotBlank
    private String content;

    private String messageType; // TEXT, IMAGE, FILE 등

    public WorkspaceWebSocketMessage() {}

    public WorkspaceWebSocketMessage(Long workspaceId, String content, String messageType) {
        this.workspaceId = workspaceId;
        this.content = content;
        this.messageType = messageType;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType != null ? messageType : "TEXT";
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
