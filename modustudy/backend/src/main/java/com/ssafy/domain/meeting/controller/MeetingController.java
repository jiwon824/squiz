package com.ssafy.domain.meeting.controller;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.common.response.ApiResponse;
import com.ssafy.common.response.MessageResponse;
import com.ssafy.common.response.PageResponse;
import com.ssafy.domain.meeting.dto.request.MeetingActionItemRequest;
import com.ssafy.domain.meeting.dto.request.MeetingKeywordUpdateRequest;
import com.ssafy.domain.meeting.dto.request.MeetingMuteRequest;
import com.ssafy.domain.meeting.dto.request.MeetingPhotoSelectionRequest;
import com.ssafy.domain.meeting.dto.request.MeetingPlannedDurationRequest;
import com.ssafy.domain.meeting.dto.request.MeetingRecordingRequest;
import com.ssafy.domain.meeting.dto.request.MeetingRequest;
import com.ssafy.domain.meeting.dto.request.MeetingSummaryUpdateRequest;
import com.ssafy.domain.meeting.dto.response.*;
import com.ssafy.domain.meeting.entity.MeetingType;
import com.ssafy.domain.meeting.service.MeetingActionItemService;
import com.ssafy.domain.meeting.service.MeetingAudioService;
import com.ssafy.domain.meeting.service.MeetingChatService;
import com.ssafy.domain.meeting.service.MeetingExportService;
import com.ssafy.domain.meeting.service.MeetingPhotoService;
import com.ssafy.domain.meeting.service.MeetingRecordingService;
import com.ssafy.domain.meeting.service.MeetingService;
import com.ssafy.domain.meeting.service.MeetingAiScheduler;
import com.ssafy.domain.meeting.service.MeetingSttService;
import com.ssafy.domain.meeting.service.MeetingTranscriptService;
import com.ssafy.common.websocket.MeetingRoomEvent;
import com.ssafy.common.websocket.MeetingRoomStateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/studies/{studyId}/meetings")
@Tag(name = "Meeting", description = "미팅 API")
public class MeetingController {

    private final MeetingService meetingService;
    private final MeetingTranscriptService meetingTranscriptService;
    private final MeetingChatService meetingChatService;
    private final MeetingPhotoService meetingPhotoService;
    private final MeetingActionItemService meetingActionItemService;
    private final MeetingExportService meetingExportService;
    private final MeetingAudioService meetingAudioService;
    private final MeetingRecordingService meetingRecordingService;
    private final MeetingSttService meetingSttService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeetingAiScheduler meetingAiScheduler;
    private final MeetingRoomStateService roomStateService;

    @GetMapping
    public ResponseEntity<PageResponse<MeetingListItemResponse>> list(
            @PathVariable Long studyId,
            @RequestParam(required = false) MeetingType meetingType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @PageableDefault(sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MeetingListItemResponse> page = meetingService.listMeetings(studyId, meetingType, startDate, endDate, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingDetailResponse>> detail(
            @PathVariable Long studyId,
            @PathVariable Long meetingId
    ) {
        return ResponseEntity.ok(ApiResponse.success(meetingService.getMeetingDetail(studyId, meetingId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponse>> start(
            @PathVariable Long studyId,
            @Valid @RequestBody MeetingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(meetingService.startMeeting(studyId, request)));
    }

    /**
     * 오프라인 녹음 업로드 (회의 생성 + 오디오 업로드 한번에 처리)
     * 기존 회의 생성 API와 달리 IN_PROGRESS 회의 체크 없이 바로 ENDED 상태로 생성
     * @param sessionId 연결할 세션 ID (선택사항, 오프라인 미팅이 어느 세션에 해당하는지 지정)
     */
    @PostMapping("/offline/audio")
    @Operation(summary = "Offline recording upload", description = "Upload offline recording and create meeting automatically. Optionally link to a specific session.")
    public ResponseEntity<ApiResponse<MeetingResponse>> uploadOfflineRecording(
            @PathVariable Long studyId,
            @RequestParam(value = "sessionId", required = false) Long sessionId,
            @RequestParam(value = "title", required = false) String title,
            @RequestPart("audio") MultipartFile audio
    ) {
        MeetingResponse response = meetingService.createOfflineMeetingWithAudio(studyId, sessionId, title, audio);
        meetingAiScheduler.triggerProcessing(response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{meetingId}/end")
    public ResponseEntity<ApiResponse<MeetingEndResponse>> end(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        MeetingEndResponse response = meetingService.endMeeting(studyId, meetingId, requireUserId(userId));
        String roomId = "meeting-" + meetingId;
        MeetingRoomEvent event = new MeetingRoomEvent(MeetingRoomEvent.Type.MEETING_ENDED, roomId);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", event);
        meetingAiScheduler.triggerProcessing(meetingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{meetingId}/end/start")
    public ResponseEntity<ApiResponse<MessageResponse>> startEnding(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        meetingService.startMeetingEnding(studyId, meetingId, requireUserId(userId));
        String roomId = "meeting-" + meetingId;
        MeetingRoomEvent event = new MeetingRoomEvent(MeetingRoomEvent.Type.MEETING_ENDING, roomId);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", event);
        return ResponseEntity.ok(ApiResponse.success("Meeting ending started"));
    }

    @PutMapping("/{meetingId}/duration")
    public ResponseEntity<ApiResponse<MeetingDetailResponse>> updatePlannedDuration(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @Valid @RequestBody MeetingPlannedDurationRequest request
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        MeetingDetailResponse response = meetingService.updatePlannedDuration(
                studyId, meetingId, requireUserId(userId), request.plannedDurationSeconds()
        );
        String roomId = "meeting-" + meetingId;
        MeetingRoomEvent event = new MeetingRoomEvent(MeetingRoomEvent.Type.MEETING_DURATION_UPDATED, roomId);
        event.setPlannedDurationSeconds(response.plannedDurationSeconds());
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", event);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{meetingId}/join")
    public ResponseEntity<ApiResponse<MeetingJoinResponse>> join(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(meetingService.joinMeeting(studyId, meetingId, requireUserId(userId))));
    }

    @PostMapping("/{meetingId}/leave")
    public ResponseEntity<ApiResponse<MessageResponse>> leave(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        meetingService.leaveMeeting(studyId, meetingId, requireUserId(userId));
        return ResponseEntity.ok(ApiResponse.success("Left meeting"));
    }

    @GetMapping("/{meetingId}/summary")
    public ResponseEntity<ApiResponse<MeetingSummaryResponse>> summary(
            @PathVariable Long studyId,
            @PathVariable Long meetingId
    ) {
        return ResponseEntity.ok(ApiResponse.success(meetingSttService.getSummary(studyId, meetingId)));
    }

    @PutMapping("/{meetingId}/summary")
    public ResponseEntity<ApiResponse<MeetingSummaryResponse>> upsertSummary(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestBody MeetingSummaryUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(meetingSttService.upsertSummary(studyId, meetingId, request)));
    }



    @GetMapping("/{meetingId}/chat")
    @Operation(summary = "미팅 채팅 조회", description = "미팅 채팅 히스토리를 페이지 형태로 조회합니다.")
    public ResponseEntity<ApiResponse<MeetingChatMessagePageResponse>> chatHistory(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(meetingChatService.getChatMessages(studyId, meetingId, pageable)));
    }

    @DeleteMapping("/{meetingId}/chat/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteChatMessage(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        String requesterName = null;
        if (userDetails != null && userDetails.getUser() != null) {
            requesterName = userDetails.getUser().getNickname();
            if (requesterName == null || requesterName.isBlank()) {
                requesterName = userDetails.getUser().getName();
            }
        }
        meetingChatService.deleteChatMessage(studyId, meetingId, messageId, userId, requesterName);
        String roomId = "meeting-" + meetingId;
        roomStateService.removeChatMessage(roomId, messageId);
        MeetingRoomEvent event = new MeetingRoomEvent(MeetingRoomEvent.Type.CHAT_DELETED, roomId);
        event.setDeletedChatId(messageId);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", event);
        return ResponseEntity.ok(ApiResponse.success("Chat deleted"));
    }

    @GetMapping("/{meetingId}/recording")
    public ResponseEntity<ApiResponse<MeetingRecordingResponse>> getRecording(
            @PathVariable Long studyId,
            @PathVariable Long meetingId
    ) {
        return ResponseEntity.ok(ApiResponse.success(meetingRecordingService.getRecording(studyId, meetingId)));
    }

    @PutMapping("/{meetingId}/recording")
    public ResponseEntity<ApiResponse<MeetingRecordingResponse>> upsertRecording(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestBody MeetingRecordingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(meetingRecordingService.upsertRecording(studyId, meetingId, request)));
    }

    @PostMapping("/{meetingId}/recording/video")
    @Operation(summary = "Meeting recording video upload", description = "Upload meeting recording video file.")
    public ResponseEntity<ApiResponse<MeetingRecordingResponse>> uploadRecordingVideo(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestPart("video") MultipartFile video
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(meetingRecordingService.uploadRecordingVideo(studyId, meetingId, video)));
    }

    @PostMapping("/{meetingId}/recording/audio")
    @Operation(summary = "Meeting recording audio upload", description = "Upload meeting recording audio file.")
    public ResponseEntity<ApiResponse<MeetingAudioRecordingResponse>> uploadRecordingAudio(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestParam("trackType") com.ssafy.domain.meeting.entity.MeetingAudioTrackType trackType,
            @RequestParam(value = "userId", required = false) Long userId,
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @RequestPart("audio") MultipartFile audio
    ) {
        if (trackType == com.ssafy.domain.meeting.entity.MeetingAudioTrackType.INDIVIDUAL) {
            Long authUserId = userDetails == null ? null : userDetails.getUser().getId();
            userId = requireUserId(authUserId);
        } else {
            userId = null;
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        meetingAudioService.uploadRecordingAudio(studyId, meetingId, trackType, userId, audio)));
    }

    @PostMapping("/{meetingId}/recording/audio/segment")
    @Operation(summary = "Meeting recording audio segment upload", description = "Upload individual audio segment.")
    public ResponseEntity<ApiResponse<MessageResponse>> uploadRecordingAudioSegment(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @RequestPart("audio") MultipartFile audio
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        meetingAudioService.uploadRecordingAudioSegment(studyId, meetingId, requireUserId(userId), audio);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Audio segment uploaded"));
    }

    @PostMapping("/{meetingId}/recording/audio/concat")
    @Operation(summary = "Meeting recording audio concat", description = "Concat individual audio segments.")
    public ResponseEntity<ApiResponse<MeetingAudioRecordingResponse>> concatRecordingAudio(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                meetingAudioService.concatRecordingAudioSegments(studyId, meetingId, requireUserId(userId))));
    }

    @GetMapping("/{meetingId}/recording/audio")
    @Operation(summary = "Meeting recording audio list", description = "List uploaded audio recordings.")
    public ResponseEntity<ApiResponse<List<MeetingAudioRecordingResponse>>> getAudioRecordings(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestParam(value = "trackType", required = false)
            com.ssafy.domain.meeting.entity.MeetingAudioTrackType trackType,
            @RequestParam(value = "userId", required = false) Long userId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long authUserId = userDetails == null ? null : userDetails.getUser().getId();
        if (trackType == com.ssafy.domain.meeting.entity.MeetingAudioTrackType.INDIVIDUAL) {
            return ResponseEntity.ok(ApiResponse.success(
                    meetingAudioService.getAudioRecordings(studyId, meetingId, trackType, requireUserId(authUserId))));
        }
        if (trackType == com.ssafy.domain.meeting.entity.MeetingAudioTrackType.MIXED) {
            return ResponseEntity.ok(ApiResponse.success(
                    meetingAudioService.getAudioRecordings(studyId, meetingId, trackType, null)));
        }
        if (authUserId != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    meetingAudioService.getAudioRecordingsForUser(studyId, meetingId, authUserId)));
        }
        return ResponseEntity.ok(ApiResponse.success(
                meetingAudioService.getAudioRecordings(studyId, meetingId,
                        com.ssafy.domain.meeting.entity.MeetingAudioTrackType.MIXED, null)));
    }

    @PostMapping("/{meetingId}/stt/file")
    @Operation(summary = "Meeting STT text upload", description = "Upload STT text file (stt.txt).")
    public ResponseEntity<ApiResponse<MeetingSttFileResponse>> uploadSttTextFile(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestParam("trackType") com.ssafy.domain.meeting.entity.MeetingTextTrackType trackType,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        meetingSttService.uploadSttTextFile(studyId, meetingId, trackType, userId, file)));
    }

    @PostMapping("/{meetingId}/summary/file")
    @Operation(summary = "Meeting summary text upload", description = "Upload summary text file (summary.txt).")
    public ResponseEntity<ApiResponse<MeetingSttSummaryResponse>> uploadSummaryTextFile(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestParam("trackType") com.ssafy.domain.meeting.entity.MeetingTextTrackType trackType,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        meetingSttService.uploadSummaryTextFile(studyId, meetingId, trackType, userId, file)));
    }

    @GetMapping("/{meetingId}/stt/file")
    @Operation(summary = "Meeting STT text file info", description = "Get STT text file metadata.")
    public ResponseEntity<ApiResponse<MeetingSttFileResponse>> getSttTextFile(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestParam("trackType") com.ssafy.domain.meeting.entity.MeetingTextTrackType trackType,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                meetingSttService.getMeetingSttFile(studyId, meetingId, trackType, userId)));
    }

    @GetMapping("/{meetingId}/summary/file")
    @Operation(summary = "Meeting summary text file info", description = "Get summary text file metadata.")
    public ResponseEntity<ApiResponse<MeetingSttSummaryResponse>> getSummaryTextFile(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestParam("trackType") com.ssafy.domain.meeting.entity.MeetingTextTrackType trackType,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                meetingSttService.getMeetingSttSummary(studyId, meetingId, trackType, userId)));
    }

    @GetMapping("/{meetingId}/photos")
    public ResponseEntity<ApiResponse<List<MeetingPhotoResponse>>> photos(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(meetingPhotoService.getPhotos(studyId, meetingId, requireUserId(userId))));
    }

    @PostMapping("/{meetingId}/photos")
    public ResponseEntity<ApiResponse<MeetingPhotoResponse>> addPhoto(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @RequestPart("image") MultipartFile image
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(meetingPhotoService.addPhoto(studyId, meetingId, requireUserId(userId), image)));
    }

    @PutMapping("/{meetingId}/photos/{photoId}/select")
    public ResponseEntity<ApiResponse<MeetingPhotoResponse>> selectPhoto(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @PathVariable Long photoId,
            @AuthenticationPrincipal SsafyUserDetails userDetails
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                meetingPhotoService.selectPhoto(studyId, meetingId, requireUserId(userId), photoId)));
    }

    @PutMapping("/{meetingId}/photos/selection")
    public ResponseEntity<ApiResponse<List<MeetingPhotoResponse>>> selectPhotos(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SsafyUserDetails userDetails,
            @RequestBody MeetingPhotoSelectionRequest request
    ) {
        Long userId = userDetails == null ? null : userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(
                meetingPhotoService.selectPhotos(studyId, meetingId, requireUserId(userId), request.photoIds())));
    }

    @PutMapping("/{meetingId}/keywords")
    public ResponseEntity<ApiResponse<MessageResponse>> updateKeywords(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingKeywordUpdateRequest request
    ) {
        meetingSttService.updateKeywords(studyId, meetingId, request);
        return ResponseEntity.ok(ApiResponse.success("Keywords updated"));
    }

    @PutMapping("/{meetingId}/participants/{userId}/mute")
    @Operation(summary = "참여자 음소거 변경", description = "미팅 참여자의 음소거 상태를 변경합니다.")
    public ResponseEntity<ApiResponse<MessageResponse>> muteParticipant(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @PathVariable Long userId,
            @Valid @RequestBody MeetingMuteRequest request
    ) {
        meetingService.updateParticipantMute(studyId, meetingId, userId, request.muted());
        return ResponseEntity.ok(ApiResponse.success("Participant updated"));
    }

    @GetMapping("/{meetingId}/action-items")
    @Operation(summary = "액션아이템 조회", description = "미팅 액션아이템 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<MeetingActionItemResponse>>> actionItems(
            @PathVariable Long studyId,
            @PathVariable Long meetingId
    ) {
        return ResponseEntity.ok(ApiResponse.success(meetingActionItemService.getActionItems(studyId, meetingId)));
    }

    @PostMapping("/{meetingId}/action-items")
    @Operation(summary = "액션아이템 추가", description = "미팅 액션아이템을 추가합니다.")
    public ResponseEntity<ApiResponse<MeetingActionItemResponse>> addActionItem(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestBody MeetingActionItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(meetingActionItemService.addActionItem(studyId, meetingId, request)));
    }

    @PutMapping("/{meetingId}/action-items/{actionItemId}")
    @Operation(summary = "액션아이템 수정", description = "액션아이템 내용을 수정합니다.")
    public ResponseEntity<ApiResponse<MeetingActionItemResponse>> updateActionItem(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @PathVariable Long actionItemId,
            @RequestBody MeetingActionItemRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                meetingActionItemService.updateActionItem(studyId, meetingId, actionItemId, request)));
    }

    @GetMapping("/{meetingId}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @RequestParam(defaultValue = "MARKDOWN") String format
    ) {
        String normalized = format.toUpperCase();
        if ("MARKDOWN".equals(normalized)) {
            byte[] bytes = meetingExportService.exportMeetingMarkdown(studyId, meetingId).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("text/markdown"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meeting-" + meetingId + ".md")
                    .body(bytes);
        }
        if ("PDF".equals(normalized)) {
            byte[] bytes = meetingExportService.exportMeetingPdf(studyId, meetingId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=meeting-" + meetingId + ".pdf")
                    .body(bytes);
        }
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_EXPORT_FORMAT");
    }

    private Long requireUserId(Long userId) {
        if (userId == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        return userId;
    }

    // ============================================================
    // 실시간 STT 트랜스크립트 API
    // ============================================================

    @Operation(summary = "실시간 STT 결과 저장", description = "화자 발언 단위로 STT 결과를 저장합니다 (SFU 서버에서 호출)")
    @PostMapping("/{meetingId}/transcripts")
    public ResponseEntity<ApiResponse<MeetingTranscriptItemResponse>> addTranscript(
            @PathVariable Long studyId,
            @PathVariable Long meetingId,
            @Valid @RequestBody com.ssafy.domain.meeting.dto.request.MeetingTranscriptRequest request
    ) {
        MeetingTranscriptItemResponse response = meetingTranscriptService.addTranscript(studyId, meetingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "미팅 트랜스크립트 조회", description = "미팅의 전체 실시간 STT 결과를 조회합니다")
    @GetMapping("/{meetingId}/transcripts")
    public ResponseEntity<ApiResponse<List<MeetingTranscriptItemResponse>>> getTranscripts(
            @PathVariable Long studyId,
            @PathVariable Long meetingId
    ) {
        List<MeetingTranscriptItemResponse> response = meetingTranscriptService.getTranscripts(studyId, meetingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
