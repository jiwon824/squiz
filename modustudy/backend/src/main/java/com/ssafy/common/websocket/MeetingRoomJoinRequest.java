package com.ssafy.common.websocket;

import jakarta.validation.constraints.NotBlank;

public class MeetingRoomJoinRequest {
    // Join payload for room WebSocket.
    @NotBlank
    private String displayName;
    private String roomTitle;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRoomTitle() {
        return roomTitle;
    }

    public void setRoomTitle(String roomTitle) {
        this.roomTitle = roomTitle;
    }
}
