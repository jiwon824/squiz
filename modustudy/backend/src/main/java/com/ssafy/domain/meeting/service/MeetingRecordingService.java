package com.ssafy.domain.meeting.service;

import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.config.SfuProperties;
import com.ssafy.domain.meeting.dto.request.MeetingRecordingRequest;
import com.ssafy.domain.meeting.dto.response.MeetingRecordingResponse;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingAudioRecording;
import com.ssafy.domain.meeting.entity.MeetingAudioTrackType;
import com.ssafy.domain.meeting.entity.MeetingRecording;
import com.ssafy.domain.meeting.entity.RecordingStatus;
import com.ssafy.domain.meeting.repository.MeetingAudioRecordingRepository;
import com.ssafy.domain.meeting.repository.MeetingRecordingRepository;
import com.ssafy.domain.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class MeetingRecordingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingRecordingService.class);

    private final MeetingRecordingRepository meetingRecordingRepository;
    private final MeetingAudioRecordingRepository meetingAudioRecordingRepository;
    private final MeetingRepository meetingRepository;
    private final LocalFileStorageService localFileStorageService;
    private final SfuProperties sfuProperties;
    private final RestTemplate restTemplate;
    private final MeetingServiceHelper helper;

    @Transactional
    public MeetingRecordingResponse upsertRecording(Long studyId, Long meetingId, MeetingRecordingRequest request) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        if (meetingRecordingRepository.findByMeetingId(meetingId).isEmpty()
                && (request.recordingUrl() == null || request.recordingUrl().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RECORDING_URL_REQUIRED");
        }
        MeetingRecording recording = meetingRecordingRepository.findByMeetingId(meetingId)
                .orElseGet(() -> MeetingRecording.builder()
                        .meetingId(meetingId)
                        .recordingUrl(request.recordingUrl())
                        .format(request.format())
                        .durationSeconds(request.durationSeconds())
                        .startedAt(request.startedAt())
                        .endedAt(request.endedAt())
                        .fileSize(request.fileSize())
                        .status(request.status() == null ? RecordingStatus.UPLOADING : request.status())
                        .build());
        if (recording.getId() == null) {
            recording = meetingRecordingRepository.save(recording);
        } else {
            recording.updateDetails(request.recordingUrl(), request.format(), request.durationSeconds(),
                    request.startedAt(), request.endedAt(), request.fileSize());
        }
        if (request.status() != null) {
            recording.updateStatus(request.status());
        }
        if (request.status() == RecordingStatus.READY) {
            meeting.updateRecordingStatus(RecordingStatus.READY);
        }
        return toRecordingResponse(recording);
    }

    /**
     * 내부 API용 - meetingId만으로 오디오 녹음 파일 업로드
     * AI 서버에서 전처리 후 호출
     */
    @Transactional
    public MeetingRecordingResponse uploadRecordingVideoInternal(Long meetingId, MultipartFile video) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND"));
        if (video == null || video.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AUDIO_REQUIRED");
        }
        String recordingUrl = localFileStorageService.saveMeetingRecordingVideo(meetingId, video);
        String format = helper.extractFileExtension(video.getOriginalFilename());
        Long fileSize = video.getSize();

        // MeetingAudioRecording 테이블에 MIXED 타입으로 저장 (AI 스케줄러가 확인)
        var existingAudio = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, MeetingAudioTrackType.MIXED);

        MeetingAudioRecording audioRecording;
        if (existingAudio.isEmpty()) {
            audioRecording = MeetingAudioRecording.builder()
                    .meetingId(meetingId)
                    .userId(null)
                    .trackType(MeetingAudioTrackType.MIXED)
                    .recordingUrl(recordingUrl)
                    .format(format)
                    .fileSize(fileSize)
                    .build();
            meetingAudioRecordingRepository.save(audioRecording);
} else {
            audioRecording = existingAudio.get(0);
            audioRecording.updateRecording(recordingUrl, format, fileSize);
}

        meeting.updateRecordingStatus(RecordingStatus.READY);

        // 응답용 DTO 생성
        return new MeetingRecordingResponse(
                audioRecording.getId(),
                recordingUrl,
                format,
                null,  // durationSeconds
                null,  // startedAt
                null,  // endedAt
                fileSize,
                RecordingStatus.READY.name(),
                audioRecording.getCreatedAt()
        );
    }

    @Transactional
    public MeetingRecordingResponse uploadRecordingVideo(Long studyId, Long meetingId, MultipartFile video) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        if (video == null || video.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIDEO_REQUIRED");
        }
        String recordingUrl = localFileStorageService.saveMeetingRecordingVideo(meetingId, video);
        String format = helper.extractFileExtension(video.getOriginalFilename());
        Long fileSize = video.getSize();
        MeetingRecording recording = meetingRecordingRepository.findByMeetingId(meetingId)
                .orElseGet(() -> MeetingRecording.builder()
                        .meetingId(meetingId)
                        .recordingUrl(recordingUrl)
                        .format(format)
                        .fileSize(fileSize)
                        .status(RecordingStatus.READY)
                        .build());
        if (recording.getId() == null) {
            recording = meetingRecordingRepository.save(recording);
        } else {
            recording.updateDetails(recordingUrl, format, null, null, null, fileSize);
            if (recording.getStatus() != RecordingStatus.READY) {
                recording.updateStatus(RecordingStatus.READY);
            }
        }
        meeting.updateRecordingStatus(RecordingStatus.READY);
        return toRecordingResponse(recording);
    }

    @Transactional(readOnly = true)
    public MeetingRecordingResponse getRecording(Long studyId, Long meetingId) {
        helper.getMeetingOrThrow(studyId, meetingId);
        MeetingRecording recording = meetingRecordingRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RECORDING_NOT_FOUND"));
        return toRecordingResponse(recording);
    }

    @Async
    public CompletableFuture<Void> triggerSfuRecordingStart(Long meetingId) {
        String controlUrl = sfuProperties.getControlUrl();
        if (controlUrl == null || controlUrl.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        String roomId = "meeting-" + meetingId;
        try {
            var payload = Map.of("roomId", roomId, "meetingId", meetingId);
            String url = controlUrl + "/recordings/start";
            var response = restTemplate.postForEntity(url, payload, String.class);
} catch (Exception e) {
}
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> triggerSfuRecordingStop(Long meetingId) {
        String controlUrl = sfuProperties.getControlUrl();
        if (controlUrl == null || controlUrl.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        String roomId = "meeting-" + meetingId;
        try {
            var payload = Map.of("roomId", roomId);
            String url = controlUrl + "/recordings/stop";
            var response = restTemplate.postForEntity(url, payload, String.class);
} catch (Exception e) {
}
        return CompletableFuture.completedFuture(null);
    }

    private MeetingRecordingResponse toRecordingResponse(MeetingRecording recording) {
        return new MeetingRecordingResponse(
                recording.getId(),
                recording.getRecordingUrl(),
                recording.getFormat(),
                recording.getDurationSeconds(),
                recording.getStartedAt(),
                recording.getEndedAt(),
                recording.getFileSize(),
                recording.getStatus().name(),
                recording.getCreatedAt()
        );
    }
}

