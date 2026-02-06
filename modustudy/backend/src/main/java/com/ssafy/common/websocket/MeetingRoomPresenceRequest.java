package com.ssafy.common.websocket;

import jakarta.validation.constraints.NotNull;

public class MeetingRoomPresenceRequest {
    @NotNull
    private Boolean present;

    public Boolean getPresent() {
        return present;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }
}
