package com.ssafy.domain.meeting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.domain.meeting.dto.request.MeetingActionItemRequest;
import com.ssafy.domain.meeting.dto.request.MeetingKeywordUpdateRequest;
import com.ssafy.domain.meeting.dto.request.MeetingRecordingRequest;
import com.ssafy.domain.meeting.dto.request.MeetingRequest;
import com.ssafy.domain.meeting.dto.request.MeetingSummaryUpdateRequest;
import com.ssafy.domain.meeting.dto.response.*;
import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.config.SfuProperties;
import com.ssafy.domain.meeting.entity.ActionItemStatus;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingActionItem;
import com.ssafy.domain.meeting.entity.MeetingAudioRecording;
import com.ssafy.domain.meeting.entity.MeetingAudioTrackType;
import com.ssafy.domain.meeting.entity.MeetingChatMessage;
import com.ssafy.domain.meeting.entity.MeetingParticipant;
import com.ssafy.domain.meeting.entity.MeetingSttFile;
import com.ssafy.domain.meeting.entity.MeetingSttSummary;
import com.ssafy.domain.meeting.entity.MeetingTextTrackType;
import com.ssafy.domain.meeting.entity.MeetingPhoto;
import com.ssafy.domain.meeting.entity.MeetingRecording;
import com.ssafy.domain.meeting.entity.MeetingStatus;
import com.ssafy.domain.meeting.entity.MeetingType;
import com.ssafy.domain.meeting.entity.RecordingStatus;
import com.ssafy.domain.meeting.entity.SttStatus;
import com.ssafy.domain.meeting.entity.SummaryStatus;
import com.ssafy.domain.meeting.repository.MeetingActionItemRepository;
import com.ssafy.domain.meeting.repository.MeetingAudioRecordingRepository;
import com.ssafy.domain.meeting.repository.MeetingChatMessageRepository;
import com.ssafy.domain.meeting.repository.MeetingParticipantRepository;
import com.ssafy.domain.meeting.repository.MeetingPhotoRepository;
import com.ssafy.domain.meeting.repository.MeetingRecordingRepository;
import com.ssafy.domain.meeting.repository.MeetingRepository;
import com.ssafy.domain.meeting.repository.MeetingSttFileRepository;
import com.ssafy.domain.meeting.repository.MeetingSttSummaryRepository;
import lombok.RequiredArgsConstructor;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);
    private static final int MAX_PLANNED_DURATION_SECONDS = 3 * 60 * 60;

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingPhotoRepository meetingPhotoRepository;
    private final MeetingRecordingRepository meetingRecordingRepository;
    private final MeetingActionItemRepository meetingActionItemRepository;
    private final MeetingAudioRecordingRepository meetingAudioRecordingRepository;
    private final MeetingChatMessageRepository meetingChatMessageRepository;
    private final MeetingSttFileRepository meetingSttFileRepository;
    private final MeetingSttSummaryRepository meetingSttSummaryRepository;
    private final com.ssafy.domain.meeting.repository.MeetingTranscriptRepository meetingTranscriptRepository;
    private final ObjectMapper objectMapper;
    private final SfuProperties sfuProperties;
    private final LocalFileStorageService localFileStorageService;
    private final org.springframework.web.client.RestTemplate restTemplate;
    private final com.ssafy.domain.ai.service.AiService aiService;
    @Value("${meeting.pdf.font-path:}")
    private String pdfFontPath;
    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;


    @Transactional(readOnly = true)
    public Page<MeetingListItemResponse> listMeetings(Long studyId, MeetingType meetingType,
                                                      LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endAt = endDate == null ? null : endDate.plusDays(1).atStartOfDay().minusNanos(1);
        return meetingRepository.searchMeetings(studyId, meetingType, startAt, endAt, pageable)
                .map(meeting -> new MeetingListItemResponse(
                        meeting.getId(),
                        meeting.getTitle(),
                        meeting.getSessionId() == null ? null : new MeetingSessionResponse(meeting.getSessionId(), null, null),
                        (meeting.getMeetingType() == null ? MeetingType.OTHER : meeting.getMeetingType()).name(),
                        meeting.getStartedAt(),
                        meeting.getEndedAt(),
                        meeting.getDurationSeconds(),
                        meeting.getParticipantCount(),
                        meetingSttSummaryRepository
                                .existsByMeetingIdAndTrackTypeAndUserIdIsNull(meeting.getId(),
                                        MeetingTextTrackType.MIXED),
                        meetingSttFileRepository
                                .existsByMeetingIdAndTrackTypeAndUserIdIsNull(meeting.getId(),
                                        MeetingTextTrackType.MIXED),
                        meetingPhotoRepository.countByMeetingId(meeting.getId())
                ));
    }

    @Transactional(readOnly = true)
    public MeetingDetailResponse getMeetingDetail(Long studyId, Long meetingId) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        List<MeetingParticipantResponse> participants = meetingParticipantRepository.findByMeetingId(meetingId).stream()
                .map(participant -> new MeetingParticipantResponse(
                        participant.getUserId(),
                        null,
                        participant.getJoinedAt(),
                        participant.getLeftAt()))
                .toList();

        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElse(null);
        SummaryStatus summaryStatus = resolveSummaryStatus(meeting);
        String summaryText = summary == null ? null : readUploadedTextFile(summary.getFileUrl());
        MeetingSummaryResponse summaryResponse = summary == null ? null : new MeetingSummaryResponse(
                summary.getId(),
                summaryText,
                parseActionItems(summary.getActionItemsJson()),
                parseKeywords(summary.getKeywordsJson()),
                summaryStatus.name(),
                summary.getCreatedAt()
        );

        return new MeetingDetailResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getSessionId() == null ? null : new MeetingSessionResponse(meeting.getSessionId(), null, null),
                meeting.getWorkspaceId() == null ? null : new MeetingWorkspaceResponse(meeting.getWorkspaceId(), null),
                (meeting.getMeetingType() == null ? MeetingType.OTHER : meeting.getMeetingType()).name(),
                meeting.getStartedAt(),
                meeting.getEndedAt(),
                meeting.getDurationSeconds(),
                meeting.getPlannedDurationSeconds(),
                meeting.getStatus().name(),
                meeting.getRecordingStatus().name(),
                meeting.getSttStatus().name(),
                summaryStatus.name(),
                meeting.getAutoShareSummary(),
                meeting.getShareWorkspaceId(),
                participants,
                summary == null ? List.of() : parseKeywords(summary.getKeywordsJson()),
                summaryResponse
        );
    }

    @Transactional
    public MeetingResponse startMeeting(Long studyId, MeetingRequest request) {
        if (meetingRepository.existsByStudyIdAndStatus(studyId, MeetingStatus.IN_PROGRESS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_IN_PROGRESS");
        }
        MeetingType meetingType = request.meetingType() == null ? MeetingType.OTHER : request.meetingType();
        boolean autoShareSummary = Boolean.TRUE.equals(request.autoShareSummary());
        int plannedDurationSeconds = request.plannedDurationSeconds() == null ? 3600 : request.plannedDurationSeconds();
        if (plannedDurationSeconds <= 0) {
            plannedDurationSeconds = 3600;
        }
        if (plannedDurationSeconds > MAX_PLANNED_DURATION_SECONDS) {
            plannedDurationSeconds = MAX_PLANNED_DURATION_SECONDS;
        }
        Meeting meeting = Meeting.start(studyId, request.sessionId(), request.workspaceId(),
                request.title(), meetingType, autoShareSummary, request.shareWorkspaceId(), LocalDateTime.now(),
                plannedDurationSeconds);
        Meeting saved = meetingRepository.save(meeting);
        triggerSfuRecordingStart(saved.getId());
        return new MeetingResponse(saved.getId(), saved.getTitle(), buildRoomToken(saved), saved.getStatus().name(),
                saved.getMeetingType().name(), saved.getRecordingStatus().name(), saved.getSttStatus().name(),
                resolveSummaryStatus(saved).name());
    }

    @Transactional
    public MeetingDetailResponse updatePlannedDuration(Long studyId, Long meetingId, Integer plannedDurationSeconds) {
        if (plannedDurationSeconds == null || plannedDurationSeconds <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLANNED_DURATION_REQUIRED");
        }
        if (plannedDurationSeconds > MAX_PLANNED_DURATION_SECONDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLANNED_DURATION_TOO_LONG");
        }
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_ALREADY_ENDED");
        }
        Integer current = meeting.getPlannedDurationSeconds();
        if (current != null && plannedDurationSeconds < current) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLANNED_DURATION_CANNOT_DECREASE");
        }
        meeting.updatePlannedDurationSeconds(plannedDurationSeconds);
        return getMeetingDetail(studyId, meetingId);
    }

    @Transactional
    public MeetingEndResponse endMeeting(Long studyId, Long meetingId) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_ALREADY_ENDED");
        }
        List<MeetingParticipant> participants = meetingParticipantRepository.findByMeetingId(meetingId);
        LocalDateTime endedAt = LocalDateTime.now();
        for (MeetingParticipant participant : participants) {
            if (participant.getLeftAt() == null) {
                participant.leave(endedAt);
            }
        }
        int participantCount = participants.size();
        meeting.end(endedAt, participantCount);
        meeting.updateSummaryStatus(SummaryStatus.PROCESSING);
        finalizeIndividualVoiceRecordings(meetingId, participants);
        triggerSfuRecordingStop(meetingId);
        return new MeetingEndResponse(meeting.getDurationSeconds(), meeting.getParticipantCount(),
                meeting.getSummaryStatus().name());
    }

    private void triggerSfuRecordingStart(Long meetingId) {
        if (sfuProperties.getControlUrl() == null || sfuProperties.getControlUrl().isBlank()) {
            return;
        }
        String roomId = "meeting-" + meetingId;
        try {
            var payload = java.util.Map.of("roomId", roomId, "meetingId", meetingId);
            restTemplate.postForEntity(sfuProperties.getControlUrl() + "/recordings/start", payload, String.class);
        } catch (Exception e) {
            log.warn("SFU recording start failed. meetingId={} error={}", meetingId, e.toString());
        }
    }

    private void triggerSfuRecordingStop(Long meetingId) {
        if (sfuProperties.getControlUrl() == null || sfuProperties.getControlUrl().isBlank()) {
            return;
        }
        String roomId = "meeting-" + meetingId;
        try {
            var payload = java.util.Map.of("roomId", roomId);
            restTemplate.postForEntity(sfuProperties.getControlUrl() + "/recordings/stop", payload, String.class);
        } catch (Exception e) {
            log.warn("SFU recording stop failed. meetingId={} error={}", meetingId, e.toString());
        }
    }

    @Transactional
    public MeetingJoinResponse joinMeeting(Long studyId, Long meetingId, Long userId) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_ALREADY_ENDED");
        }
        MeetingParticipant participant = meetingParticipantRepository.findTopByMeetingIdAndUserIdOrderByJoinedAtDesc(meetingId, userId)
                .orElseGet(() -> MeetingParticipant.join(meetingId, userId, LocalDateTime.now()));
        if (participant.getId() != null) {
            participant.rejoin(LocalDateTime.now());
        }
        meetingParticipantRepository.save(participant);
        meeting.updateParticipantCount(meetingParticipantRepository.countByMeetingId(meetingId));
        // Provide ICE servers for WebRTC clients to connect to the SFU.
        List<MeetingIceServerResponse> iceServers = sfuProperties.getIceServers().stream()
                .filter(server -> server.getUrls() != null && !server.getUrls().isBlank())
                .map(server -> new MeetingIceServerResponse(
                        server.getUrls(),
                        server.getUsername(),
                        server.getCredential()))
                .toList();
        return new MeetingJoinResponse(buildRoomToken(meeting), iceServers);
    }

    @Transactional
    public void leaveMeeting(Long studyId, Long meetingId, Long userId) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        MeetingParticipant participant = meetingParticipantRepository.findTopByMeetingIdAndUserIdOrderByJoinedAtDesc(meetingId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NOT_IN_MEETING"));
        participant.leave(LocalDateTime.now());
        meeting.updateParticipantCount(meetingParticipantRepository.countByMeetingId(meetingId));
        concatVoiceSegmentsIfExists(meetingId, userId);
    }

    @Transactional(readOnly = true)
    public MeetingSummaryResponse getSummary(Long studyId, Long meetingId) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SUMMARY_NOT_READY"));
        SummaryStatus currentStatus = resolveSummaryStatus(meeting);
        String summaryText = readUploadedTextFile(summary.getFileUrl());
        if (currentStatus == SummaryStatus.PENDING && summaryText != null && !summaryText.isBlank()) {
            meeting.updateSummaryStatus(SummaryStatus.DONE);
        }
        if (resolveSummaryStatus(meeting) != SummaryStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SUMMARY_NOT_READY");
        }
        return new MeetingSummaryResponse(
                summary.getId(),
                summaryText,
                parseActionItems(summary.getActionItemsJson()),
                parseKeywords(summary.getKeywordsJson()),
                resolveSummaryStatus(meeting).name(),
                summary.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public MeetingChatMessagePageResponse getChatMessages(Long studyId, Long meetingId, Pageable pageable) {
        getMeetingOrThrow(studyId, meetingId);
        Page<MeetingChatMessage> page = meetingChatMessageRepository
                .findByMeetingIdOrderBySentAtAsc(meetingId, pageable);
        List<MeetingChatMessageResponse> content = page.stream()
                .map(message -> new MeetingChatMessageResponse(
                        message.getId(),
                        message.getUserId(),
                        message.getSenderName(),
                        message.getContent(),
                        message.getSentAt()))
                .toList();
        return new MeetingChatMessagePageResponse(content, page.getTotalElements(), page.hasNext());
    }

    @Transactional(readOnly = true)
    public List<MeetingAudioRecordingResponse> getAudioRecordings(Long studyId, Long meetingId,
                                                                  MeetingAudioTrackType trackType,
                                                                  Long userId) {
        getMeetingOrThrow(studyId, meetingId);
        List<MeetingAudioRecording> recordings;
        if (trackType != null && userId != null) {
            recordings = meetingAudioRecordingRepository
                    .findByMeetingIdAndTrackTypeAndUserIdOrderByCreatedAtAsc(meetingId, trackType, userId);
        } else if (trackType != null) {
            recordings = meetingAudioRecordingRepository
                    .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, trackType);
        } else if (userId != null) {
            recordings = meetingAudioRecordingRepository
                    .findByMeetingIdAndUserIdOrderByCreatedAtAsc(meetingId, userId);
        } else {
            recordings = meetingAudioRecordingRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId);
        }
        return recordings.stream()
                .map(recording -> new MeetingAudioRecordingResponse(
                        recording.getId(),
                        recording.getMeetingId(),
                        recording.getUserId(),
                        recording.getTrackType(),
                        recording.getRecordingUrl(),
                        recording.getFormat(),
                        recording.getFileSize(),
                        recording.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeetingAudioRecordingResponse> getAudioRecordingsForUser(Long studyId, Long meetingId, Long userId) {
        getMeetingOrThrow(studyId, meetingId);
        List<MeetingAudioRecording> mixed = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, MeetingAudioTrackType.MIXED);
        List<MeetingAudioRecording> individual = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeAndUserIdOrderByCreatedAtAsc(
                        meetingId, MeetingAudioTrackType.INDIVIDUAL, userId);
        return java.util.stream.Stream.concat(mixed.stream(), individual.stream())
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(recording -> new MeetingAudioRecordingResponse(
                        recording.getId(),
                        recording.getMeetingId(),
                        recording.getUserId(),
                        recording.getTrackType(),
                        recording.getRecordingUrl(),
                        recording.getFormat(),
                        recording.getFileSize(),
                        recording.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeetingPhotoResponse> getPhotos(Long studyId, Long meetingId, Long userId) {
        getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        return meetingPhotoRepository.findByMeetingIdAndUserIdOrderByCapturedAtDesc(meetingId, userId).stream()
                .map(photo -> new MeetingPhotoResponse(
                        photo.getId(),
                        photo.getImageUrl(),
                        photo.getCapturedAt(),
                        photo.getIsSelected()))
                .toList();
    }

    @Transactional
    public MeetingPhotoResponse addPhoto(Long studyId, Long meetingId, Long userId, MultipartFile image) {
        getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMAGE_REQUIRED");
        }
        // Save to local filesystem and return a public URL path.
        String imageUrl = localFileStorageService.saveMeetingPhoto(meetingId, image);
        MeetingPhoto saved = meetingPhotoRepository.save(
                MeetingPhoto.capture(meetingId, userId, imageUrl, LocalDateTime.now()));
        return new MeetingPhotoResponse(saved.getId(), saved.getImageUrl(), saved.getCapturedAt(), saved.getIsSelected());
    }

    @Transactional
    public MeetingPhotoResponse selectPhoto(Long studyId, Long meetingId, Long userId, Long photoId) {
        getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        MeetingPhoto target = meetingPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND"));
        if (!target.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PHOTO_MEETING_MISMATCH");
        }
        if (target.getUserId() == null || !target.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PHOTO_FORBIDDEN");
        }
        List<MeetingPhoto> photos = meetingPhotoRepository.findByMeetingIdAndUserIdOrderByCapturedAtDesc(meetingId, userId);
        for (MeetingPhoto photo : photos) {
            photo.updateSelected(photo.getId().equals(photoId));
        }
        meetingPhotoRepository.saveAll(photos);
        return new MeetingPhotoResponse(target.getId(), target.getImageUrl(), target.getCapturedAt(), true);
    }

    @Transactional
    public List<MeetingPhotoResponse> selectPhotos(Long studyId, Long meetingId, Long userId, List<Long> photoIds) {
        getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        List<MeetingPhoto> photos = meetingPhotoRepository.findByMeetingIdAndUserIdOrderByCapturedAtDesc(meetingId, userId);
        var selectedIds = photoIds == null ? java.util.Set.<Long>of() : new java.util.HashSet<>(photoIds);
        for (MeetingPhoto photo : photos) {
            photo.updateSelected(selectedIds.contains(photo.getId()));
        }
        meetingPhotoRepository.saveAll(photos);
        return photos.stream()
                .map(photo -> new MeetingPhotoResponse(
                        photo.getId(),
                        photo.getImageUrl(),
                        photo.getCapturedAt(),
                        photo.getIsSelected()))
                .toList();
    }

    @Transactional
    public void updateKeywords(Long studyId, Long meetingId, MeetingKeywordUpdateRequest request) {
        getMeetingOrThrow(studyId, meetingId);
        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElseGet(() -> meetingSttSummaryRepository.save(MeetingSttSummary.builder()
                        .meetingId(meetingId)
                        .trackType(MeetingTextTrackType.MIXED)
                        .fileUrl("")
                        .build()));
        summary.updateKeywordsJson(writeJson(request.keywords()));
        meetingSttSummaryRepository.save(summary);
    }

    @Transactional
    public void updateSummaryStatus(Long studyId, Long meetingId, SummaryStatus status) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        meeting.updateSummaryStatus(status);
    }

    @Transactional
    public MeetingSummaryResponse upsertSummary(Long studyId, Long meetingId, MeetingSummaryUpdateRequest request) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElseGet(() -> meetingSttSummaryRepository.save(MeetingSttSummary.builder()
                        .meetingId(meetingId)
                        .trackType(MeetingTextTrackType.MIXED)
                        .fileUrl("")
                        .build()));
        if (request.summary() != null) {
            String fileUrl = localFileStorageService.saveMeetingTextContent(
                    meetingId, null, true, "summary.txt", request.summary());
            summary.updateFileUrl(fileUrl);
        }
        if (request.actionItems() != null) {
            summary.updateActionItemsJson(writeJson(request.actionItems()));
        }
        if (request.keywords() != null) {
            summary.updateKeywordsJson(writeJson(request.keywords()));
        }
        SummaryStatus status = request.status();
        if (status == null) {
            status = request.summary() == null ? resolveSummaryStatus(meeting) : SummaryStatus.DONE;
        }
        if (status != null) {
            meeting.updateSummaryStatus(status);
        }
        meetingSttSummaryRepository.save(summary);
        String summaryText = readUploadedTextFile(summary.getFileUrl());
        return new MeetingSummaryResponse(
                summary.getId(),
                summaryText,
                parseActionItems(summary.getActionItemsJson()),
                parseKeywords(summary.getKeywordsJson()),
                resolveSummaryStatus(meeting).name(),
                summary.getCreatedAt()
        );
    }

    @Transactional
    public void updateParticipantMute(Long studyId, Long meetingId, Long userId, boolean muted) {
        getMeetingOrThrow(studyId, meetingId);
        MeetingParticipant participant = meetingParticipantRepository.findTopByMeetingIdAndUserIdOrderByJoinedAtDesc(meetingId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NOT_IN_MEETING"));
        participant.updateMute(muted);
    }

    @Transactional
    public void addChatMessage(Long meetingId, Long userId, String senderName, String content, Instant sentAt) {
        if (meetingRepository.findById(meetingId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND");
        }
        LocalDateTime sentAtTime = sentAt == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(sentAt, ZoneId.systemDefault());
        meetingChatMessageRepository.save(MeetingChatMessage.builder()
                .meetingId(meetingId)
                .userId(userId)
                .senderName(senderName)
                .content(content)
                .sentAt(sentAtTime)
                .build());
    }

    @Transactional
    public MeetingAudioRecordingResponse uploadRecordingAudio(Long studyId, Long meetingId,
                                                              MeetingAudioTrackType trackType,
                                                              Long userId,
                                                              MultipartFile audio) {
        getMeetingOrThrow(studyId, meetingId);
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AUDIO_REQUIRED");
        }
        if (trackType == MeetingAudioTrackType.INDIVIDUAL && userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
        }
        if (trackType == MeetingAudioTrackType.MIXED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MIXED_AUDIO_NOT_SUPPORTED");
        }
        String filename = buildIndividualVoiceFilename(userId);
        String recordingUrl = localFileStorageService.saveMeetingVoiceFinal(meetingId, filename, audio);
        String format = extractFileExtension(audio.getOriginalFilename());
        MeetingAudioRecording saved = saveOrUpdateAudioRecording(
                meetingId,
                userId,
                trackType,
                recordingUrl,
                format == null ? "webm" : format,
                audio.getSize()
        );
        return toAudioRecordingResponse(saved);
    }

    @Transactional
    public void uploadRecordingAudioSegment(Long studyId, Long meetingId, Long userId, MultipartFile audio) {
        getMeetingOrThrow(studyId, meetingId);
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AUDIO_REQUIRED");
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
        }
        localFileStorageService.saveMeetingVoiceSegment(meetingId, userId, audio);
    }

    @Transactional
    public MeetingAudioRecordingResponse concatRecordingAudioSegments(Long studyId, Long meetingId, Long userId) {
        getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
        }
        Path segmentsDir = localFileStorageService.resolveMeetingVoiceSegmentsDir(meetingId, userId);
        if (!Files.exists(segmentsDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_AUDIO_SEGMENTS");
        }
        List<Path> segments;
        try {
            segments = Files.list(segmentsDir)
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AUDIO_SEGMENT_READ_FAILED");
        }
        if (segments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_AUDIO_SEGMENTS");
        }
        Path voiceDir = localFileStorageService.resolveMeetingVoiceDir(meetingId);
        try {
            Files.createDirectories(voiceDir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "VOICE_DIR_CREATE_FAILED");
        }
        Path concatFile = voiceDir.resolve("segments-" + userId + ".txt").normalize();
        try {
            String contents = segments.stream()
                    .map(path -> "file '" + path.toAbsolutePath().toString().replace("\\", "/") + "'")
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            Files.writeString(concatFile, contents);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AUDIO_CONCAT_LIST_FAILED");
        }
        Path outputPath = localFileStorageService.resolveMeetingVoiceFile(meetingId, buildIndividualVoiceFilename(userId));
        runFfmpegConcat(concatFile, outputPath);
        Long fileSize;
        try {
            fileSize = Files.size(outputPath);
        } catch (IOException e) {
            fileSize = null;
        }
        MeetingAudioRecording saved = saveOrUpdateAudioRecording(
                meetingId,
                userId,
                MeetingAudioTrackType.INDIVIDUAL,
                localFileStorageService.buildMeetingVoiceUrl(meetingId, buildIndividualVoiceFilename(userId)),
                "webm",
                fileSize
        );
        cleanupVoiceSegments(segmentsDir, concatFile);
        return toAudioRecordingResponse(saved);
    }

    private void finalizeIndividualVoiceRecordings(Long meetingId, List<MeetingParticipant> participants) {
        var userIds = new java.util.HashSet<Long>();
        if (participants != null) {
            participants.stream()
                    .map(MeetingParticipant::getUserId)
                    .filter(Objects::nonNull)
                    .forEach(userIds::add);
        }
        userIds.addAll(findVoiceSegmentUserIds(meetingId));
        userIds.forEach((userId) -> {
            try {
                concatVoiceSegmentsIfExists(meetingId, userId);
            } catch (Exception e) {
                log.warn("Failed to finalize voice segments. meetingId={} userId={} error={}",
                        meetingId, userId, e.toString());
            }
        });
    }

    private void concatVoiceSegmentsIfExists(Long meetingId, Long userId) {
        Path segmentsDir = localFileStorageService.resolveMeetingVoiceSegmentsDir(meetingId, userId);
        if (!Files.exists(segmentsDir)) {
            return;
        }
        List<Path> segments;
        try {
            segments = Files.list(segmentsDir)
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.warn("Voice segment read failed. meetingId={} userId={} error={}", meetingId, userId, e.toString());
            return;
        }
        if (segments.isEmpty()) {
            return;
        }
        Path voiceDir = localFileStorageService.resolveMeetingVoiceDir(meetingId);
        try {
            Files.createDirectories(voiceDir);
        } catch (IOException e) {
            log.warn("Voice dir create failed. meetingId={} userId={} error={}", meetingId, userId, e.toString());
            return;
        }
        Path concatFile = voiceDir.resolve("segments-" + userId + ".txt").normalize();
        try {
            String contents = segments.stream()
                    .map(path -> "file '" + path.toAbsolutePath().toString().replace("\\", "/") + "'")
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            Files.writeString(concatFile, contents);
        } catch (IOException e) {
            log.warn("Voice concat list failed. meetingId={} userId={} error={}", meetingId, userId, e.toString());
            return;
        }
        Path outputPath = localFileStorageService.resolveMeetingVoiceFile(meetingId, buildIndividualVoiceFilename(userId));
        try {
            runFfmpegConcat(concatFile, outputPath);
        } catch (ResponseStatusException e) {
            log.warn("Voice concat ffmpeg failed. meetingId={} userId={} error={}", meetingId, userId, e.toString());
            return;
        }
        Long fileSize;
        try {
            fileSize = Files.size(outputPath);
        } catch (IOException e) {
            fileSize = null;
        }
        saveOrUpdateAudioRecording(
                meetingId,
                userId,
                MeetingAudioTrackType.INDIVIDUAL,
                localFileStorageService.buildMeetingVoiceUrl(meetingId, buildIndividualVoiceFilename(userId)),
                "webm",
                fileSize
        );
        cleanupVoiceSegments(segmentsDir, concatFile);
    }

    private List<Long> findVoiceSegmentUserIds(Long meetingId) {
        Path segmentsRoot = localFileStorageService.resolveMeetingVoiceDir(meetingId).resolve("segments");
        if (!Files.exists(segmentsRoot)) {
            return List.of();
        }
        try {
            return Files.list(segmentsRoot)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .map(name -> {
                        try {
                            return Long.parseLong(name);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            log.warn("Voice segment user scan failed. meetingId={} error={}", meetingId, e.toString());
            return List.of();
        }
    }

    @Transactional
    public MeetingSttFileResponse uploadSttTextFile(Long studyId, Long meetingId,
                                                    MeetingTextTrackType trackType,
                                                    Long userId,
                                                    MultipartFile file) {
        return uploadSttFileInternal(studyId, meetingId, trackType, userId, file);
    }

    @Transactional
    public MeetingSttSummaryResponse uploadSummaryTextFile(Long studyId, Long meetingId,
                                                           MeetingTextTrackType trackType,
                                                           Long userId,
                                                           MultipartFile file) {
        return uploadSummaryFileInternal(studyId, meetingId, trackType, userId, file);
    }

    @Transactional(readOnly = true)
    public MeetingSttFileResponse getMeetingSttFile(Long studyId, Long meetingId,
                                                    MeetingTextTrackType trackType,
                                                    Long userId) {
        getMeetingOrThrow(studyId, meetingId);
        MeetingSttFile file = (userId == null
                ? meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, trackType)
                : meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserId(meetingId, trackType, userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TEXT_FILE_NOT_FOUND"));
        return new MeetingSttFileResponse(
                file.getId(),
                file.getMeetingId(),
                file.getUserId(),
                file.getTrackType(),
                file.getFileUrl(),
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public MeetingSttSummaryResponse getMeetingSttSummary(Long studyId, Long meetingId,
                                                          MeetingTextTrackType trackType,
                                                          Long userId) {
        getMeetingOrThrow(studyId, meetingId);
        MeetingSttSummary file = (userId == null
                ? meetingSttSummaryRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, trackType)
                : meetingSttSummaryRepository.findByMeetingIdAndTrackTypeAndUserId(meetingId, trackType, userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TEXT_FILE_NOT_FOUND"));
        return new MeetingSttSummaryResponse(
                file.getId(),
                file.getMeetingId(),
                file.getUserId(),
                file.getTrackType(),
                file.getFileUrl(),
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }

    @Transactional
    public MeetingRecordingResponse upsertRecording(Long studyId, Long meetingId, MeetingRecordingRequest request) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
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

    @Transactional
    public MeetingRecordingResponse uploadRecordingVideo(Long studyId, Long meetingId, MultipartFile video) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        if (video == null || video.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIDEO_REQUIRED");
        }
        String recordingUrl = localFileStorageService.saveMeetingRecordingVideo(meetingId, video);
        String format = extractFileExtension(video.getOriginalFilename());
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

    @Transactional(readOnly = true)
    public MeetingRecordingResponse getRecording(Long studyId, Long meetingId) {
        getMeetingOrThrow(studyId, meetingId);
        MeetingRecording recording = meetingRecordingRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RECORDING_NOT_FOUND"));
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

    @Transactional(readOnly = true)
    public List<MeetingActionItemResponse> getActionItems(Long studyId, Long meetingId) {
        getMeetingOrThrow(studyId, meetingId);
        return meetingActionItemRepository.findByMeetingId(meetingId).stream()
                .map(item -> new MeetingActionItemResponse(
                        item.getId(),
                        item.getContent(),
                        item.getAssigneeId(),
                        item.getStatus()))
                .toList();
    }

    @Transactional
    public MeetingActionItemResponse addActionItem(Long studyId, Long meetingId, MeetingActionItemRequest request) {
        getMeetingOrThrow(studyId, meetingId);
        if (request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ACTION_ITEM_CONTENT_REQUIRED");
        }
        ActionItemStatus status = request.status() == null ? ActionItemStatus.TODO : request.status();
        MeetingActionItem saved = meetingActionItemRepository.save(MeetingActionItem.builder()
                .meetingId(meetingId)
                .content(request.content())
                .assigneeId(request.assigneeId())
                .status(status)
                .build());
        return new MeetingActionItemResponse(
                saved.getId(),
                saved.getContent(),
                saved.getAssigneeId(),
                saved.getStatus());
    }

    @Transactional
    public MeetingActionItemResponse updateActionItem(Long studyId, Long meetingId, Long actionItemId,
                                                      MeetingActionItemRequest request) {
        getMeetingOrThrow(studyId, meetingId);
        MeetingActionItem item = meetingActionItemRepository.findById(actionItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ACTION_ITEM_NOT_FOUND"));
        if (!item.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ACTION_ITEM_MISMATCH");
        }
        if (request.assigneeId() != null) {
            item.updateAssignee(request.assigneeId());
        }
        if (request.status() != null) {
            item.updateStatus(request.status());
        }
        if (request.content() != null) {
            item.updateContent(request.content());
        }
        return new MeetingActionItemResponse(
                item.getId(),
                item.getContent(),
                item.getAssigneeId(),
                item.getStatus());
    }

    @Transactional(readOnly = true)
    public String exportMeetingMarkdown(Long studyId, Long meetingId) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElse(null);
        MeetingSttFile transcriptFile = meetingSttFileRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElse(null);
        List<MeetingPhoto> selectedPhotos = meetingPhotoRepository
                .findByMeetingIdAndIsSelectedTrueOrderByCapturedAtDesc(meetingId);
        StringBuilder builder = new StringBuilder();
        builder.append("# Meeting Summary").append("\n\n");
        builder.append("- Title: ").append(meeting.getTitle()).append("\n");
        builder.append("- Type: ").append(meeting.getMeetingType().name()).append("\n");
        builder.append("- Started At: ").append(meeting.getStartedAt()).append("\n");
        builder.append("- Ended At: ").append(meeting.getEndedAt()).append("\n\n");
        String summaryText = summary == null ? null : readUploadedTextFile(summary.getFileUrl());
        if (summaryText != null && !summaryText.isBlank()) {
            builder.append("## Overall Summary").append("\n").append(summaryText).append("\n\n");
        }
        List<MeetingActionItemResponse> actionItems = parseActionItems(
                summary == null ? null : summary.getActionItemsJson());
        if (!actionItems.isEmpty()) {
            builder.append("## Action Items").append("\n");
            for (MeetingActionItemResponse item : actionItems) {
                builder.append("- [").append(item.status().name()).append("] ")
                        .append(item.content());
                if (item.assigneeId() != null) {
                    builder.append(" (assignee: ").append(item.assigneeId()).append(")");
                }
                builder.append("\n");
            }
            builder.append("\n");
        }
        if (transcriptFile != null) {
            String transcriptText = readUploadedTextFile(transcriptFile.getFileUrl());
            if (transcriptText != null && !transcriptText.isBlank()) {
                builder.append("## Transcripts").append("\n");
                builder.append(transcriptText).append("\n");
            }
        }
        if (!selectedPhotos.isEmpty()) {
            builder.append("\n## 회의 사진\n");
            for (MeetingPhoto photo : selectedPhotos) {
                builder.append("![").append("회의 사진").append("](").append(photo.getImageUrl()).append(")\n");
            }
        }
        return builder.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exportMeetingPdf(Long studyId, Long meetingId) {
        String markdown = exportMeetingMarkdown(studyId, meetingId);
        List<MeetingPhoto> selectedPhotos = meetingPhotoRepository
                .findByMeetingIdAndIsSelectedTrueOrderByCapturedAtDesc(meetingId);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            BaseFont baseFont = resolvePdfBaseFont();
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font sectionFont = new Font(baseFont, 13, Font.BOLD);
            Font bodyFont = new Font(baseFont, 11);
            for (String line : markdown.split("\n")) {
                if (line.startsWith("## 회의 사진") || line.startsWith("![")) {
                    continue;
                }
                if (line.startsWith("# ")) {
                    document.add(new Paragraph(line.substring(2), titleFont));
                } else if (line.startsWith("## ")) {
                    Paragraph paragraph = new Paragraph(line.substring(3), sectionFont);
                    paragraph.setSpacingBefore(10f);
                    document.add(paragraph);
                } else {
                    Paragraph paragraph = new Paragraph(line, bodyFont);
                    paragraph.setLeading(0f, 1.4f);
                    paragraph.setAlignment(Element.ALIGN_LEFT);
                    document.add(paragraph);
                }
            }
            appendSelectedPhotos(document, sectionFont, bodyFont, selectedPhotos);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_EXPORT_FAILED");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_FONT_LOAD_FAILED");
        }
    }

    private Meeting getMeetingOrThrow(Long studyId, Long meetingId) {
        return meetingRepository.findByIdAndStudyId(meetingId, studyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND"));
    }

    private List<String> parseKeywords(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            return List.of();
        }
    }

    private List<MeetingActionItemResponse> parseActionItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<MeetingActionItemResponse>>() {});
        } catch (IOException e) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(Objects.requireNonNullElse(value, List.of()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JSON_SERIALIZE_FAILED");
        }
    }

    private String buildRoomToken(Meeting meeting) {
        return "meeting-" + meeting.getId();
    }

    private SummaryStatus resolveSummaryStatus(Meeting meeting) {
        return meeting.getSummaryStatus() == null ? SummaryStatus.PENDING : meeting.getSummaryStatus();
    }


    private String buildIndividualVoiceFilename(Long userId) {
        return userId + "voice.webm";
    }

    private MeetingAudioRecording saveOrUpdateAudioRecording(Long meetingId,
                                                             Long userId,
                                                             MeetingAudioTrackType trackType,
                                                             String recordingUrl,
                                                             String format,
                                                             Long fileSize) {
        MeetingAudioRecording existing = userId == null
                ? meetingAudioRecordingRepository
                        .findTopByMeetingIdAndTrackTypeAndUserIdIsNullOrderByCreatedAtDesc(meetingId, trackType)
                        .orElse(null)
                : meetingAudioRecordingRepository
                        .findTopByMeetingIdAndTrackTypeAndUserIdOrderByCreatedAtDesc(meetingId, trackType, userId)
                        .orElse(null);
        if (existing != null) {
            existing.updateRecording(recordingUrl, format, fileSize);
            return meetingAudioRecordingRepository.save(existing);
        }
        return meetingAudioRecordingRepository.save(MeetingAudioRecording.builder()
                .meetingId(meetingId)
                .userId(userId)
                .trackType(trackType)
                .recordingUrl(recordingUrl)
                .format(format)
                .fileSize(fileSize)
                .build());
    }

    private MeetingAudioRecordingResponse toAudioRecordingResponse(MeetingAudioRecording saved) {
        return new MeetingAudioRecordingResponse(
                saved.getId(),
                saved.getMeetingId(),
                saved.getUserId(),
                saved.getTrackType(),
                saved.getRecordingUrl(),
                saved.getFormat(),
                saved.getFileSize(),
                saved.getCreatedAt());
    }

    private void runFfmpegConcat(Path concatFile, Path outputPath) {
        List<String> args = List.of(
                ffmpegPath,
                "-y",
                "-f",
                "concat",
                "-safe",
                "0",
                "-i",
                concatFile.toAbsolutePath().toString(),
                "-c",
                "copy",
                outputPath.toAbsolutePath().toString()
        );
        runFfmpeg(args, "voice-concat", null);
    }

    private void runFfmpeg(List<String> args, String label, Long meetingId) {
        try {
            ProcessBuilder builder = new ProcessBuilder(args);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                log.warn("FFmpeg failed. label={} meetingId={} exit={} output={}", label, meetingId, exit, output.trim());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FFMPEG_FAILED");
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("FFmpeg error. label={} meetingId={} error={}", label, meetingId, e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FFMPEG_FAILED");
        }
    }

    private void cleanupVoiceSegments(Path segmentsDir, Path concatFile) {
        try {
            if (Files.exists(concatFile)) {
                Files.deleteIfExists(concatFile);
            }
            if (!Files.exists(segmentsDir)) {
                return;
            }
            try (var stream = Files.list(segmentsDir)) {
                stream.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // ignore
                    }
                });
            }
        } catch (IOException ignored) {
            // ignore cleanup failures
        }
    }

    private BaseFont resolvePdfBaseFont() throws IOException, DocumentException {
        if (pdfFontPath != null && !pdfFontPath.isBlank()) {
            return BaseFont.createFont(pdfFontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private void appendSelectedPhotos(Document document, Font sectionFont, Font bodyFont, List<MeetingPhoto> photos)
            throws DocumentException {
        if (photos == null || photos.isEmpty()) {
            return;
        }
        Paragraph paragraph = new Paragraph("회의 사진", sectionFont);
        paragraph.setSpacingBefore(10f);
        document.add(paragraph);
        for (MeetingPhoto photo : photos) {
            Path imagePath = localFileStorageService.resolveUploadedPath(photo.getImageUrl());
            if (imagePath == null || !Files.exists(imagePath)) {
                document.add(new Paragraph(photo.getImageUrl(), bodyFont));
                continue;
            }
            try {
                Image image = Image.getInstance(imagePath.toAbsolutePath().toString());
                float maxWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                image.scaleToFit(maxWidth, 360f);
                image.setSpacingBefore(6f);
                document.add(image);
            } catch (Exception e) {
                document.add(new Paragraph(photo.getImageUrl(), bodyFont));
            }
        }
    }

    private String extractFileExtension(String filename) {
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

    private MeetingSttFileResponse uploadSttFileInternal(Long studyId, Long meetingId,
                                                         MeetingTextTrackType trackType,
                                                         Long userId,
                                                         MultipartFile file) {
        getMeetingOrThrow(studyId, meetingId);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_REQUIRED");
        }
        validateTextTrack(trackType, userId);
        boolean mixed = trackType == MeetingTextTrackType.MIXED;
        String fileUrl = localFileStorageService.saveMeetingTextFile(meetingId, userId, mixed, "stt.txt", file);
        MeetingSttFile saved = (userId == null
                ? meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, trackType)
                : meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserId(meetingId, trackType, userId))
                .map(existing -> {
                    existing.updateFileUrl(fileUrl);
                    return meetingSttFileRepository.save(existing);
                })
                .orElseGet(() -> meetingSttFileRepository.save(MeetingSttFile.builder()
                        .meetingId(meetingId)
                        .userId(userId)
                        .trackType(trackType)
                        .fileUrl(fileUrl)
                        .build()));
        return new MeetingSttFileResponse(
                saved.getId(),
                saved.getMeetingId(),
                saved.getUserId(),
                saved.getTrackType(),
                saved.getFileUrl(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    private MeetingSttSummaryResponse uploadSummaryFileInternal(Long studyId, Long meetingId,
                                                                MeetingTextTrackType trackType,
                                                                Long userId,
                                                                MultipartFile file) {
        getMeetingOrThrow(studyId, meetingId);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FILE_REQUIRED");
        }
        validateTextTrack(trackType, userId);
        boolean mixed = trackType == MeetingTextTrackType.MIXED;
        String fileUrl = localFileStorageService.saveMeetingTextFile(meetingId, userId, mixed, "summary.txt", file);
        MeetingSttSummary saved = (userId == null
                ? meetingSttSummaryRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, trackType)
                : meetingSttSummaryRepository.findByMeetingIdAndTrackTypeAndUserId(meetingId, trackType, userId))
                .map(existing -> {
                    existing.updateFileUrl(fileUrl);
                    return meetingSttSummaryRepository.save(existing);
                })
                .orElseGet(() -> meetingSttSummaryRepository.save(MeetingSttSummary.builder()
                        .meetingId(meetingId)
                        .userId(userId)
                        .trackType(trackType)
                        .fileUrl(fileUrl)
                        .build()));
        return new MeetingSttSummaryResponse(
                saved.getId(),
                saved.getMeetingId(),
                saved.getUserId(),
                saved.getTrackType(),
                saved.getFileUrl(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    private void validateTextTrack(MeetingTextTrackType trackType, Long userId) {
        if (trackType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRACK_TYPE_REQUIRED");
        }
        if (trackType == MeetingTextTrackType.INDIVIDUAL && userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
        }
        if (trackType == MeetingTextTrackType.MIXED && userId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_NOT_ALLOWED");
        }
    }

    private String readUploadedTextFile(String fileUrl) {
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

    /**
     * 미팅 종료 후 AI 처리 시작
     * - 전체 음성 + 화자별 음성 → AI 서버 전송
     * - STT, 요약, 키워드, 액션아이템, 퀴즈 생성
     *
     * @param studyId 스터디 ID
     * @param meetingId 미팅 ID
     * @return AI 처리 작업 ID (job_id)
     */
    @Transactional
    public String startAiProcessing(Long studyId, Long meetingId) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() != MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_NOT_ENDED");
        }

        // 1. 전체 음성 파일 찾기 (MIXED)
        List<MeetingAudioRecording> mixedRecordings = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, MeetingAudioTrackType.MIXED);
        if (mixedRecordings.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_MIXED_AUDIO");
        }
        Path mixedAudioPath = localFileStorageService.resolveUploadedPath(mixedRecordings.get(0).getRecordingUrl());
        if (mixedAudioPath == null || !Files.exists(mixedAudioPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MIXED_AUDIO_NOT_FOUND");
        }

        // 2. 화자별 음성 파일 찾기 (INDIVIDUAL)
        List<MeetingAudioRecording> individualRecordings = meetingAudioRecordingRepository
                .findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(meetingId, MeetingAudioTrackType.INDIVIDUAL);
        java.util.Map<Long, Path> individualPaths = new java.util.HashMap<>();
        for (MeetingAudioRecording rec : individualRecordings) {
            if (rec.getUserId() != null) {
                Path path = localFileStorageService.resolveUploadedPath(rec.getRecordingUrl());
                if (path != null && Files.exists(path)) {
                    individualPaths.put(rec.getUserId(), path);
                }
            }
        }

        // 3. AI 처리 요청
        meeting.updateSummaryStatus(SummaryStatus.PROCESSING);
        String jobId = aiService.processMeetingAsync(mixedAudioPath, individualPaths, true);

        log.info("AI 처리 시작 - meetingId: {}, jobId: {}", meetingId, jobId);
        return jobId;
    }

    /**
     * AI 처리 결과 조회 및 저장
     *
     * @param studyId 스터디 ID
     * @param meetingId 미팅 ID
     * @param jobId AI 작업 ID
     * @return 처리 상태 (pending, processing, completed, failed)
     */
    @Transactional
    public String checkAndSaveAiResult(Long studyId, Long meetingId, String jobId) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);

        com.ssafy.domain.ai.service.AiService.MeetingProcessResult result = aiService.getMeetingProcessResult(jobId);

        if ("completed".equals(result.getStatus())) {
            // 1. STT 저장
            if (result.getTranscript() != null) {
                String sttFileUrl = localFileStorageService.saveMeetingTextContent(
                        meetingId, null, true, "stt.txt", result.getTranscript());
                meetingSttFileRepository.findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                        .ifPresentOrElse(
                                existing -> existing.updateFileUrl(sttFileUrl),
                                () -> meetingSttFileRepository.save(MeetingSttFile.builder()
                                        .meetingId(meetingId)
                                        .trackType(MeetingTextTrackType.MIXED)
                                        .fileUrl(sttFileUrl)
                                        .build())
                        );
            }

            // 2. 요약 + 키워드 + 액션아이템 저장
            MeetingSttSummary summary = meetingSttSummaryRepository
                    .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                    .orElseGet(() -> MeetingSttSummary.builder()
                            .meetingId(meetingId)
                            .trackType(MeetingTextTrackType.MIXED)
                            .fileUrl("")
                            .build());

            if (result.getSummary() != null) {
                String summaryFileUrl = localFileStorageService.saveMeetingTextContent(
                        meetingId, null, true, "summary.txt", result.getSummary());
                summary.updateFileUrl(summaryFileUrl);
            }

            if (result.getKeywords() != null && !result.getKeywords().isEmpty()) {
                summary.updateKeywordsJson(writeJson(result.getKeywords()));
            }

            if (result.getActionItems() != null && !result.getActionItems().isEmpty()) {
                // JSON으로 저장
                List<MeetingActionItemResponse> actionItemResponses = new java.util.ArrayList<>();
                for (var item : result.getActionItems()) {
                    actionItemResponses.add(new MeetingActionItemResponse(
                            null, item.getContent(), item.getUserId(), ActionItemStatus.TODO));
                    // 개별 엔티티로도 저장
                    meetingActionItemRepository.save(MeetingActionItem.builder()
                            .meetingId(meetingId)
                            .content(item.getContent())
                            .assigneeId(item.getUserId())
                            .status(ActionItemStatus.TODO)
                            .build());
                }
                summary.updateActionItemsJson(writeJson(actionItemResponses));
            }

            meetingSttSummaryRepository.save(summary);

            // 3. 상태 업데이트
            meeting.updateSummaryStatus(SummaryStatus.DONE);
            log.info("AI 처리 완료 - meetingId: {}", meetingId);

        } else if ("failed".equals(result.getStatus())) {
            meeting.updateSummaryStatus(SummaryStatus.PENDING);
            log.error("AI 처리 실패 - meetingId: {}, error: {}", meetingId, result.getError());
        }

        return result.getStatus();
    }

    // ============================================================
    // 실시간 STT 트랜스크립트 관련
    // ============================================================

    /**
     * 실시간 STT 결과 저장 (화자별 발언 단위)
     */
    @Transactional
    public MeetingTranscriptItemResponse addTranscript(
            Long studyId, Long meetingId, com.ssafy.domain.meeting.dto.request.MeetingTranscriptRequest request) {
        Meeting meeting = meetingRepository.findByIdAndStudyId(meetingId, studyId)
                .orElseThrow(() -> new IllegalArgumentException("미팅을 찾을 수 없습니다."));

        if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 미팅에만 트랜스크립트를 추가할 수 있습니다.");
        }

        com.ssafy.domain.meeting.entity.MeetingTranscript transcript =
                com.ssafy.domain.meeting.entity.MeetingTranscript.create(
                        meetingId,
                        request.userId(),
                        request.content(),
                        request.timestampSeconds()
                );

        transcript = meetingTranscriptRepository.save(transcript);

        MeetingUserResponse userResponse = new MeetingUserResponse(request.userId(), null);

        return new MeetingTranscriptItemResponse(
                transcript.getId(),
                userResponse,
                transcript.getContent(),
                transcript.getTimestampSeconds(),
                request.startMs(),
                request.endMs(),
                transcript.getCreatedAt()
        );
    }

    /**
     * 미팅의 전체 트랜스크립트 조회
     */
    @Transactional(readOnly = true)
    public java.util.List<MeetingTranscriptItemResponse> getTranscripts(Long studyId, Long meetingId) {
        meetingRepository.findByIdAndStudyId(meetingId, studyId)
                .orElseThrow(() -> new IllegalArgumentException("미팅을 찾을 수 없습니다."));

        return meetingTranscriptRepository.findByMeetingIdOrderByTimestampSecondsAsc(meetingId)
                .stream()
                .map(t -> new MeetingTranscriptItemResponse(
                        t.getId(),
                        new MeetingUserResponse(t.getUserId(), null),
                        t.getContent(),
                        t.getTimestampSeconds(),
                        null,
                        null,
                        t.getCreatedAt()
                ))
                .toList();
    }

    /**
     * 미팅의 전체 트랜스크립트 텍스트 조회 (요약용)
     */
    @Transactional(readOnly = true)
    public String getTranscriptText(Long meetingId) {
        java.util.List<String> contents = meetingTranscriptRepository.findAllContentByMeetingId(meetingId);
        return String.join(" ", contents);
    }
}


