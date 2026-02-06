package com.ssafy.domain.meeting.dto.response;

import java.util.List;

public record MeetingJoinResponse(
        String roomToken,
        List<MeetingIceServerResponse> iceServers
        ) {
}
