package com.ssafy.common.websocket;

import jakarta.validation.constraints.NotNull;

public class MeetingRoomSpeakingRequest {
    @NotNull
    private Boolean speaking;

    public Boolean getSpeaking() {
        return speaking;
    }

    public void setSpeaking(Boolean speaking) {
        this.speaking = speaking;
    }
}
