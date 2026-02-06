package com.ssafy.common.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MeetingRoomParticipantDto {
    // Minimal participant payload for WebSocket events.
    private Long id;
    private String displayName;
    private boolean active;
    @JsonProperty("isSpeaking")
    private boolean isSpeaking;
    @JsonProperty("isPresent")
    private boolean isPresent;

    public MeetingRoomParticipantDto() {
    }

    public MeetingRoomParticipantDto(Long id, String displayName, boolean active) {
        this.id = id;
        this.displayName = displayName;
        this.active = active;
    }
    public MeetingRoomParticipantDto(Long id, String displayName, boolean active, boolean isSpeaking, boolean isPresent) {
        this.id = id;
        this.displayName = displayName;
        this.active = active;
        this.isSpeaking = isSpeaking;
        this.isPresent = isPresent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    @JsonProperty("isSpeaking")
    public void setSpeaking(boolean speaking) {
        this.isSpeaking = speaking;
    }

    public boolean isPresent() {
        return isPresent;
    }

    @JsonProperty("isPresent")
    public void setPresent(boolean present) {
        this.isPresent = present;
    }
}
