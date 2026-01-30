package com.ssafy.domain.meeting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.domain.meeting.dto.response.MeetingActionItemResponse;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.SummaryStatus;
import com.ssafy.domain.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MeetingServiceHelper {

    private final MeetingRepository meetingRepository;
    private final ObjectMapper objectMapper;
    private final LocalFileStorageService localFileStorageService;

    public Meeting getMeetingOrThrow(Long studyId, Long meetingId) {
        return meetingRepository.findByIdAndStudyId(meetingId, studyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND"));
    }

    public Meeting getMeetingOrThrow(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND"));
    }

    public List<String> parseKeywords(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            return List.of();
        }
    }

    public List<MeetingActionItemResponse> parseActionItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<MeetingActionItemResponse>>() {});
        } catch (IOException e) {
            return List.of();
        }
    }

    public String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(Objects.requireNonNullElse(value, List.of()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JSON_SERIALIZE_FAILED");
        }
    }

    public String readUploadedTextFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        Path path = localFileStorageService.resolveUploadedPath(fileUrl);
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return null;
        }
    }

    public SummaryStatus resolveSummaryStatus(Meeting meeting) {
        return meeting.getSummaryStatus() == null ? SummaryStatus.PENDING : meeting.getSummaryStatus();
    }

    public String buildRoomToken(Meeting meeting) {
        return "meeting-" + meeting.getId();
    }

    public String buildIndividualVoiceFilename(Long userId) {
        return userId + "voice.webm";
    }

    public String extractFileExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return null;
        }
        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return extension.isBlank() ? null : extension;
    }
}
