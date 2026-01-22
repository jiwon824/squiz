package com.ssafy.domain.dm.websocket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DM WebSocket 메시지 전송용 DTO
 */
public class DmWebSocketMessage {

    @NotNull
    private Long receiverId;

    @NotBlank
    private String content;

    public DmWebSocketMessage() {}

    public DmWebSocketMessage(Long receiverId, String content) {
        this.receiverId = receiverId;
        this.content = content;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
