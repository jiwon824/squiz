package com.ssafy.domain.meeting.controller;

import com.ssafy.config.SfuProperties;
import com.ssafy.domain.meeting.dto.response.MeetingIceServerResponse;
import com.ssafy.domain.meeting.dto.response.SfuConfigResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sfu")
public class SfuController {
    // Exposes SFU config to clients before WebRTC setup.
    private final SfuProperties sfuProperties;

    public SfuController(SfuProperties sfuProperties) {
        this.sfuProperties = sfuProperties;
    }

    @GetMapping("/config")
    public SfuConfigResponse getConfig() {
        List<MeetingIceServerResponse> iceServers = sfuProperties.getIceServers().stream()
                .filter(server -> server.getUrls() != null && !server.getUrls().isBlank())
                .map(server -> new MeetingIceServerResponse(
                        server.getUrls(),
                        server.getUsername(),
                        server.getCredential()))
                .collect(Collectors.toList());
        return new SfuConfigResponse(sfuProperties.getBaseUrl(), iceServers);
    }
}
