package com.ssafy.common.websocket;

import jakarta.validation.constraints.NotBlank;

public class MeetingRoomPresenterRequest {
    // Presenter claim/release payload for room WebSocket.
    @NotBlank
    private String displayName;
    private String action;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
