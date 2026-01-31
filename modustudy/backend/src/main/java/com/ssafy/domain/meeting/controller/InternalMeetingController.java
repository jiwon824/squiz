package com.ssafy.domain.meeting.controller;

import com.ssafy.common.response.ApiResponse;
import com.ssafy.domain.meeting.dto.request.MeetingTextFileUpsertRequest;
import com.ssafy.domain.meeting.dto.request.SpeechSegmentRequest;
import com.ssafy.domain.meeting.dto.response.MeetingRecordingResponse;
import com.ssafy.domain.meeting.dto.response.MeetingSttFileResponse;
import com.ssafy.domain.meeting.dto.response.MeetingSttSummaryResponse;
import com.ssafy.domain.meeting.dto.response.SpeechSegmentResponse;
import com.ssafy.domain.meeting.service.MeetingRecordingService;
import com.ssafy.domain.meeting.service.MeetingSttService;
import com.ssafy.domain.meeting.service.SpeechSegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 내부 서버 간 통신용 API
 * AI 서버에서 녹음 파일 업로드 시 사용
 * studyId 없이 meetingId만으로 처리 (향후 스터디 외 미팅 지원)
 */
@Tag(name = "Internal Meeting API", description = "서버 간 내부 통신용 API")
@RestController
@RequestMapping("/api/internal/meetings")
@RequiredArgsConstructor
public class InternalMeetingController {

    private final MeetingRecordingService meetingRecordingService;
    private final MeetingSttService meetingSttService;
    private final SpeechSegmentService speechSegmentService;

    @PostMapping("/{meetingId}/recording/video")
    @Operation(summary = "녹음 파일 업로드 (내부용)", description = "AI 서버에서 전처리된 녹음 파일 업로드")
    public ResponseEntity<ApiResponse<MeetingRecordingResponse>> uploadRecordingVideo(
            @PathVariable Long meetingId,
            @RequestPart("video") MultipartFile video
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(meetingRecordingService.uploadRecordingVideoInternal(meetingId, video)));
    }

    @PostMapping("/{meetingId}/speech-segments")
    @Operation(summary = "실시간 발화 세그먼트 수신 (내부용)", description = "AI 서버에서 STT 처리된 발화 세그먼트 수신 및 WebSocket 브로드캐스트")
    public ResponseEntity<ApiResponse<SpeechSegmentResponse>> receiveSpeechSegment(
            @PathVariable Long meetingId,
            @RequestBody SpeechSegmentRequest request
    ) {
        SpeechSegmentResponse response = speechSegmentService.processSpeechSegment(meetingId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/{meetingId}/stt/file")
    @Operation(summary = "STT 파일 정보 저장 (내부용)", description = "AI 서버에서 생성한 STT 파일 경로를 저장")
    public ResponseEntity<ApiResponse<MeetingSttFileResponse>> upsertSttFile(
            @PathVariable Long meetingId,
            @RequestBody(required = false) MeetingTextFileUpsertRequest request
    ) {
        String fileUrl = request == null ? null : request.fileUrl();
        MeetingSttFileResponse response = meetingSttService.upsertSttFileInternal(meetingId, fileUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/{meetingId}/summary/file")
    @Operation(summary = "Summary 파일 정보 저장 (내부용)", description = "AI 서버에서 생성한 summary 파일 경로를 저장")
    public ResponseEntity<ApiResponse<MeetingSttSummaryResponse>> upsertSummaryFile(
            @PathVariable Long meetingId,
            @RequestBody(required = false) MeetingTextFileUpsertRequest request
    ) {
        String fileUrl = request == null ? null : request.fileUrl();
        MeetingSttSummaryResponse response = meetingSttService.upsertSummaryFileInternal(meetingId, fileUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
