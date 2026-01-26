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
import org.springframework.web.client.RestTemplate;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingPhotoRepository meetingPhotoRepository;
    private final MeetingRecordingRepository meetingRecordingRepository;
    private final MeetingActionItemRepository meetingActionItemRepository;
    private final MeetingAudioRecordingRepository meetingAudioRecordingRepository;
    private final MeetingChatMessageRepository meetingChatMessageRepository;
    private final MeetingSttFileRepository meetingSttFileRepository;
    private final MeetingSttSummaryRepository meetingSttSummaryRepository;
    private final ObjectMapper objectMapper;
    private final SfuProperties sfuProperties;
    private final LocalFileStorageService localFileStorageService;
    private final RestTemplate restTemplate;
    @Value("${meeting.pdf.font-path:}")
    private String pdfFontPath;

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
                meeting.getChannelId() == null ? null : new MeetingChannelResponse(meeting.getChannelId(), null),
                (meeting.getMeetingType() == null ? MeetingType.OTHER : meeting.getMeetingType()).name(),
                meeting.getStartedAt(),
                meeting.getEndedAt(),
                meeting.getDurationSeconds(),
                meeting.getStatus().name(),
                meeting.getRecordingStatus().name(),
                meeting.getSttStatus().name(),
                summaryStatus.name(),
                meeting.getAutoShareSummary(),
                meeting.getShareChannelId(),
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
        Meeting meeting = Meeting.start(studyId, request.sessionId(), request.channelId(),
                request.title(), meetingType, autoShareSummary, request.shareChannelId(), LocalDateTime.now());
        Meeting saved = meetingRepository.save(meeting);
        triggerSfuRecordingStart(saved);
        return new MeetingResponse(saved.getId(), saved.getTitle(), buildRoomToken(saved), saved.getStatus().name(),
                saved.getMeetingType().name(), saved.getRecordingStatus().name(), saved.getSttStatus().name(),
                resolveSummaryStatus(saved).name());
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
        completeSfuRecording(meeting);
        return new MeetingEndResponse(meeting.getDurationSeconds(), meeting.getParticipantCount(),
                meeting.getSummaryStatus().name());
    }

    @Transactional
    public MeetingJoinResponse joinMeeting(Long studyId, Long meetingId, Long userId) {
        Meeting meeting = getMeetingOrThrow(studyId, meetingId);
        if (meeting.getStatus() == MeetingStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEETING_ALREADY_ENDED");
        }
        boolean firstJoin = meeting.getParticipantCount() == null || meeting.getParticipantCount() == 0;
        MeetingParticipant participant = meetingParticipantRepository.findTopByMeetingIdAndUserIdOrderByJoinedAtDesc(meetingId, userId)
                .orElseGet(() -> MeetingParticipant.join(meetingId, userId, LocalDateTime.now()));
        if (participant.getId() != null) {
            participant.rejoin(LocalDateTime.now());
        }
        meetingParticipantRepository.save(participant);
        meeting.updateParticipantCount(meetingParticipantRepository.countByMeetingId(meetingId));
        if (firstJoin) {
            triggerSfuRecordingStart(meeting);
        }
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
    public List<MeetingPhotoResponse> getPhotos(Long studyId, Long meetingId) {
        getMeetingOrThrow(studyId, meetingId);
        return meetingPhotoRepository.findByMeetingIdOrderByCapturedAtDesc(meetingId).stream()
                .map(photo -> new MeetingPhotoResponse(
                        photo.getId(),
                        photo.getImageUrl(),
                        photo.getCapturedAt(),
                        photo.getIsSelected()))
                .toList();
    }

    @Transactional
    public MeetingPhotoResponse addPhoto(Long studyId, Long meetingId, MultipartFile image) {
        getMeetingOrThrow(studyId, meetingId);
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMAGE_REQUIRED");
        }
        // Save to local filesystem and return a public URL path.
        String imageUrl = localFileStorageService.saveMeetingPhoto(meetingId, image);
        MeetingPhoto saved = meetingPhotoRepository.save(
                MeetingPhoto.capture(meetingId, imageUrl, LocalDateTime.now()));
        return new MeetingPhotoResponse(saved.getId(), saved.getImageUrl(), saved.getCapturedAt(), saved.getIsSelected());
    }

    @Transactional
    public MeetingPhotoResponse selectPhoto(Long studyId, Long meetingId, Long photoId) {
        getMeetingOrThrow(studyId, meetingId);
        MeetingPhoto target = meetingPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND"));
        if (!target.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PHOTO_MEETING_MISMATCH");
        }
        List<MeetingPhoto> photos = meetingPhotoRepository.findByMeetingIdOrderByCapturedAtDesc(meetingId);
        for (MeetingPhoto photo : photos) {
            photo.updateSelected(photo.getId().equals(photoId));
        }
        meetingPhotoRepository.saveAll(photos);
        return new MeetingPhotoResponse(target.getId(), target.getImageUrl(), target.getCapturedAt(), true);
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
        if (trackType == MeetingAudioTrackType.MIXED && userId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_ID_NOT_ALLOWED");
        }
        boolean mixed = trackType == MeetingAudioTrackType.MIXED;
        String recordingUrl = localFileStorageService.saveMeetingRecordingAudio(
                meetingId, userId, mixed, audio);
        String format = extractFileExtension(audio.getOriginalFilename());
        MeetingAudioRecording saved = meetingAudioRecordingRepository.save(MeetingAudioRecording.builder()
                .meetingId(meetingId)
                .userId(userId)
                .trackType(trackType)
                .recordingUrl(recordingUrl)
                .format(format)
                .fileSize(audio.getSize())
                .build());
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
        MeetingPhoto selectedPhoto = meetingPhotoRepository.findFirstByMeetingIdAndIsSelectedTrue(meetingId)
                .orElse(null);
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
        if (selectedPhoto != null) {
            builder.append("\n## 회의 사진\n");
            builder.append("![").append("회의 사진").append("](").append(selectedPhoto.getImageUrl()).append(")\n");
        }
        return builder.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exportMeetingPdf(Long studyId, Long meetingId) {
        String markdown = exportMeetingMarkdown(studyId, meetingId);
        MeetingPhoto selectedPhoto = meetingPhotoRepository.findFirstByMeetingIdAndIsSelectedTrue(meetingId)
                .orElse(null);
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
            appendSelectedPhoto(document, sectionFont, bodyFont, selectedPhoto);
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

    private void triggerSfuRecordingStart(Meeting meeting) {
        String controlUrl = resolveSfuControlUrl();
        if (controlUrl == null || controlUrl.isBlank()) {
            log.warn("SFU control URL is not configured. Skip recording start. meetingId={}", meeting.getId());
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", buildRoomToken(meeting));
        payload.put("meetingId", meeting.getId());
        try {
            Map response = restTemplate.postForObject(controlUrl + "/recordings/start", payload, Map.class);
            log.info("SFU recording started. meetingId={} response={}", meeting.getId(), response);
        } catch (Exception e) {
            log.warn("SFU recording start failed. meetingId={} controlUrl={} error={}", meeting.getId(), controlUrl, e.toString());
            meeting.updateRecordingStatus(RecordingStatus.FAILED);
        }
    }

    private void completeSfuRecording(Meeting meeting) {
        String controlUrl = resolveSfuControlUrl();
        if (controlUrl == null || controlUrl.isBlank()) {
            log.warn("SFU control URL is not configured. Skip recording stop. meetingId={}", meeting.getId());
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", buildRoomToken(meeting));
        try {
            Map response = restTemplate.postForObject(controlUrl + "/recordings/stop", payload, Map.class);
            if (response == null) {
                log.warn("SFU recording stop returned null response. meetingId={}", meeting.getId());
                meeting.updateRecordingStatus(RecordingStatus.FAILED);
                return;
            }
            Object outputPathValue = response.get("outputPath");
            Object fileSizeValue = response.get("fileSize");
            log.info("SFU recording stopped. meetingId={} response={}", meeting.getId(), response);
            String recordingUrl = resolveRecordingUrl(outputPathValue == null ? null : String.valueOf(outputPathValue));
            Long fileSize = fileSizeValue == null ? null : Long.parseLong(String.valueOf(fileSizeValue));
            if (recordingUrl == null) {
                log.warn("SFU recording output path invalid. meetingId={} outputPath={}", meeting.getId(), outputPathValue);
                meeting.updateRecordingStatus(RecordingStatus.FAILED);
                return;
            }
            MeetingRecordingRequest request = new MeetingRecordingRequest(
                    recordingUrl,
                    "webm",
                    meeting.getDurationSeconds(),
                    meeting.getStartedAt(),
                    meeting.getEndedAt(),
                    fileSize,
                    RecordingStatus.READY
            );
            upsertRecording(meeting.getStudyId(), meeting.getId(), request);
        } catch (Exception e) {
            log.warn("SFU recording stop failed. meetingId={} controlUrl={} error={}", meeting.getId(), controlUrl, e.toString());
            meeting.updateRecordingStatus(RecordingStatus.FAILED);
        }
    }

    private String resolveSfuControlUrl() {
        String controlUrl = sfuProperties.getControlUrl();
        if (controlUrl != null && !controlUrl.isBlank()) {
            return controlUrl;
        }
        String baseUrl = sfuProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        if (baseUrl.startsWith("wss://")) {
            return "https://" + baseUrl.substring(6);
        }
        if (baseUrl.startsWith("ws://")) {
            return "http://" + baseUrl.substring(5);
        }
        return baseUrl;
    }

    private String resolveRecordingUrl(String outputPath) {
        if (outputPath == null || outputPath.isBlank()) {
            return null;
        }
        Path basePath = localFileStorageService.getBasePath().toAbsolutePath().normalize();
        Path resolved = Paths.get(outputPath).toAbsolutePath().normalize();
        if (!resolved.startsWith(basePath)) {
            return null;
        }
        Path relative = basePath.relativize(resolved);
        String normalized = relative.toString().replace("\\", "/");
        return "/uploads/" + normalized;
    }

    private BaseFont resolvePdfBaseFont() throws IOException, DocumentException {
        if (pdfFontPath != null && !pdfFontPath.isBlank()) {
            return BaseFont.createFont(pdfFontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private void appendSelectedPhoto(Document document, Font sectionFont, Font bodyFont, MeetingPhoto selectedPhoto)
            throws DocumentException {
        if (selectedPhoto == null) {
            return;
        }
        Paragraph paragraph = new Paragraph("회의 사진", sectionFont);
        paragraph.setSpacingBefore(10f);
        document.add(paragraph);
        Path imagePath = localFileStorageService.resolveUploadedPath(selectedPhoto.getImageUrl());
        if (imagePath == null || !Files.exists(imagePath)) {
            document.add(new Paragraph(selectedPhoto.getImageUrl(), bodyFont));
            return;
        }
        try {
            Image image = Image.getInstance(imagePath.toAbsolutePath().toString());
            float maxWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
            image.scaleToFit(maxWidth, 360f);
            image.setSpacingBefore(6f);
            document.add(image);
        } catch (Exception e) {
            document.add(new Paragraph(selectedPhoto.getImageUrl(), bodyFont));
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
}
