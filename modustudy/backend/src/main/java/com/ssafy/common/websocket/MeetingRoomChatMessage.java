package com.ssafy.common.websocket;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class MeetingRoomChatMessage {
    // Optional userId for persisted chat messages.
    private Long id;
    private Long userId;
    @NotBlank
    private String sender;
    @NotBlank
    private String text;
    // Default to server time when the chat message is received.
    private Instant sentAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
