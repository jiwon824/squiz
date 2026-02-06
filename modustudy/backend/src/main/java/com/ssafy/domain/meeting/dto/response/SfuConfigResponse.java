package com.ssafy.domain.meeting.dto.response;

import java.util.List;

public record SfuConfigResponse(
        // Base SFU signaling URL (ws/wss).
        String baseUrl,
        List<MeetingIceServerResponse> iceServers
        ) {
}
