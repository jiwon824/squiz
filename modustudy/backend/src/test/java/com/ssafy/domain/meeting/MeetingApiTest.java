package com.ssafy.domain.meeting;

import com.ssafy.common.auth.SsafyUserDetails;
import com.ssafy.config.SfuProperties;
import com.ssafy.domain.meeting.dto.request.MeetingActionItemRequest;
import com.ssafy.domain.meeting.dto.request.MeetingKeywordUpdateRequest;
import com.ssafy.domain.meeting.dto.request.MeetingPhotoSelectionRequest;
import com.ssafy.domain.meeting.dto.request.MeetingPlannedDurationRequest;
import com.ssafy.domain.meeting.dto.request.MeetingRecordingRequest;
import com.ssafy.domain.meeting.dto.request.MeetingRequest;
import com.ssafy.domain.meeting.dto.request.MeetingSummaryUpdateRequest;
import com.ssafy.domain.meeting.dto.response.MeetingActionItemResponse;
import com.ssafy.domain.meeting.dto.response.MeetingAudioRecordingResponse;
import com.ssafy.domain.meeting.dto.response.MeetingChatMessagePageResponse;
import com.ssafy.domain.meeting.dto.response.MeetingChatMessageResponse;
import com.ssafy.domain.meeting.dto.response.MeetingWorkspaceResponse;
import com.ssafy.domain.meeting.dto.response.MeetingDetailResponse;
import com.ssafy.domain.meeting.dto.response.MeetingEndResponse;
import com.ssafy.domain.meeting.dto.response.MeetingJoinResponse;
import com.ssafy.domain.meeting.dto.response.MeetingListItemResponse;
import com.ssafy.domain.meeting.dto.response.MeetingParticipantResponse;
import com.ssafy.domain.meeting.dto.response.MeetingPhotoResponse;
import com.ssafy.domain.meeting.dto.response.MeetingRecordingResponse;
import com.ssafy.domain.meeting.dto.response.MeetingResponse;
import com.ssafy.domain.meeting.dto.response.MeetingSessionResponse;
import com.ssafy.domain.meeting.dto.response.MeetingSttFileResponse;
import com.ssafy.domain.meeting.dto.response.MeetingSttSummaryResponse;
import com.ssafy.domain.meeting.dto.response.MeetingSummaryResponse;
import com.ssafy.domain.meeting.entity.ActionItemStatus;
import com.ssafy.domain.meeting.entity.MeetingAudioTrackType;
import com.ssafy.domain.meeting.entity.MeetingTextTrackType;
import com.ssafy.domain.meeting.entity.MeetingType;
import com.ssafy.domain.meeting.entity.SummaryStatus;
import com.ssafy.domain.meeting.service.MeetingService;
import com.ssafy.domain.meeting.service.MeetingChatService;
import com.ssafy.domain.meeting.service.MeetingAiScheduler;
import com.ssafy.common.websocket.MeetingRoomStateService;
import com.ssafy.domain.meeting.service.MeetingAudioService;
import com.ssafy.domain.meeting.service.MeetingRecordingService;
import com.ssafy.domain.meeting.service.MeetingPhotoService;
import com.ssafy.domain.meeting.service.MeetingActionItemService;
import com.ssafy.domain.meeting.service.MeetingSttService;
import com.ssafy.domain.meeting.service.MeetingExportService;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(MeetingApiTest.TestConfig.class)
class MeetingApiTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private MeetingChatService meetingChatService;

    @MockBean
    private MeetingAudioService meetingAudioService;

    @MockBean
    private MeetingRecordingService meetingRecordingService;

    @MockBean
    private MeetingPhotoService meetingPhotoService;

    @MockBean
    private MeetingActionItemService meetingActionItemService;

    @MockBean
    private MeetingSttService meetingSttService;

    @MockBean
    private MeetingExportService meetingExportService;

    @MockBean
    private MeetingRoomStateService meetingRoomStateService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private MeetingAiScheduler meetingAiScheduler;

    @MockBean
    private SfuProperties sfuProperties;

    @Test
    @DisplayName("미팅 목록 조회: 필터 포함")
    void listMeetings_withFilters() throws Exception {
        // given
        MeetingListItemResponse item = new MeetingListItemResponse(
                1L,
                "weekly",
                new MeetingSessionResponse(10L, 1, "session"),
                "WEEKLY",
                LocalDateTime.of(2025, 1, 15, 19, 0),
                LocalDateTime.of(2025, 1, 15, 20, 30),
                5400,
                5,
                true,
                true,
                2
        );
        PageImpl<MeetingListItemResponse> page = new PageImpl<>(
                List.of(item),
                PageRequest.of(0, 20),
                1
        );
        when(meetingService.listMeetings(eq(1L), eq(MeetingType.DAILY),
                eq(LocalDate.parse("2025-01-01")), eq(LocalDate.parse("2025-01-31")),
                any())).thenReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/studies/1/meetings")
                        .param("meetingType", "DAILY")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].meetingType").value("WEEKLY"));
    }

    @Test
    @DisplayName("SFU 설정 조회")
    void sfuConfigEndpoint() throws Exception {
        // given
        SfuProperties.IceServer iceServer = new SfuProperties.IceServer();
        iceServer.setUrls("stun:stun.l.google.com:19302");
        iceServer.setUsername("user");
        iceServer.setCredential("pass");
        when(sfuProperties.getBaseUrl()).thenReturn("https://sfu.local");
        when(sfuProperties.getIceServers()).thenReturn(List.of(iceServer));

        // when & then
        mockMvc.perform(get("/api/v1/sfu/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseUrl").value("https://sfu.local"))
                .andExpect(jsonPath("$.iceServers[0].urls").value("stun:stun.l.google.com:19302"))
                .andExpect(jsonPath("$.iceServers[0].username").value("user"))
                .andExpect(jsonPath("$.iceServers[0].credential").value("pass"));
    }

    @Test
    @DisplayName("사진 선택")
    void selectPhotoEndpoint() throws Exception {
        // given
        MeetingPhotoResponse selected = new MeetingPhotoResponse(3L, "meeting/2/photo-3.png",
                LocalDateTime.of(2025, 1, 15, 20, 12), true);
        when(meetingPhotoService.selectPhoto(1L, 2L, 1L, 3L)).thenReturn(selected);

        // when & then
        mockMvc.perform(put("/api/v1/studies/1/meetings/2/photos/3/select")
                        .with(authentication(authUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.isSelected").value(true));
    }

    @Test
    @DisplayName("미팅 상세 조회: 상태/요약/참여자 포함")
    void getMeetingDetail() throws Exception {
        // given
        MeetingSummaryResponse summary = new MeetingSummaryResponse(
                1L,
                "summary",
                List.of(new MeetingActionItemResponse(1L, "todo", null, ActionItemStatus.TODO)),
                List.of("DP"),
                List.of("주요 내용 1", "주요 내용 2"),
                "DONE",
                LocalDateTime.of(2025, 1, 15, 20, 40)
        );
        MeetingDetailResponse detail = new MeetingDetailResponse(
                1L,
                "title",
                new MeetingSessionResponse(10L, 1, "session"),
                new MeetingWorkspaceResponse(2L, "voice"),
                "DAILY",
                LocalDateTime.of(2025, 1, 15, 19, 0),
                LocalDateTime.of(2025, 1, 15, 20, 30),
                5400,
                7200,
                "ENDED",
                "READY",
                "DONE",
                "DONE",
                true,
                12L,
                List.of(new MeetingParticipantResponse(1L, "user", LocalDateTime.now(), null)),
                List.of("DP"),
                summary
        );
        when(meetingService.getMeetingDetail(1L, 1L)).thenReturn(detail);

        // when & then
        mockMvc.perform(get("/api/v1/studies/1/meetings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.meetingType").value("DAILY"))
                .andExpect(jsonPath("$.data.summary.status").value("DONE"));
    }

    @Test
    @DisplayName("미팅 시작: 공유 옵션 포함")
    void startMeeting() throws Exception {
        // given
        MeetingResponse response = new MeetingResponse(
                1L,
                "title",
                "meeting-1",
                "IN_PROGRESS",
                "DAILY",
                "RECORDING",
                "PENDING",
                "PENDING"
        );
        when(meetingService.startMeeting(eq(1L), any())).thenReturn(response);

        // when & then
        MeetingRequest request = new MeetingRequest("title", 10L, 2L, MeetingType.DAILY, true, 12L, 3600);
        mockMvc.perform(post("/api/v1/studies/1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.recordingStatus").value("RECORDING"));
    }

    @Test
    @DisplayName("미팅 종료: 요약 상태 반환")
    void endMeeting() throws Exception {
        // given
        when(meetingService.endMeeting(1L, 2L, 1L)).thenReturn(new MeetingEndResponse(5400, 5, "PROCESSING"));

        // when & then
        mockMvc.perform(put("/api/v1/studies/1/meetings/2/end")
                        .with(authentication(authUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summaryStatus").value("PROCESSING"));
    }

    @Test
    @DisplayName("미팅 예정 시간 업데이트")
    void updatePlannedDuration() throws Exception {
        // given
        MeetingDetailResponse detail = new MeetingDetailResponse(
                2L,
                "title",
                new MeetingSessionResponse(10L, 1, "session"),
                new MeetingWorkspaceResponse(2L, "voice"),
                "DAILY",
                LocalDateTime.of(2025, 1, 15, 19, 0),
                null,
                null,
                7200,
                "IN_PROGRESS",
                "RECORDING",
                "PENDING",
                "PENDING",
                false,
                null,
                List.of(),
                List.of(),
                null
        );
        when(meetingService.updatePlannedDuration(1L, 2L, 1L, 7200)).thenReturn(detail);

        // when & then
        MeetingPlannedDurationRequest request = new MeetingPlannedDurationRequest(7200);
        mockMvc.perform(put("/api/v1/studies/1/meetings/2/duration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(authUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plannedDurationSeconds").value(7200));
    }

    @Test
    @DisplayName("미팅 참가/퇴장")
    void joinAndLeaveMeeting() throws Exception {
        // given
        when(meetingService.joinMeeting(1L, 2L, 1L))
                .thenReturn(new MeetingJoinResponse("meeting-2", List.of()));
        doNothing().when(meetingService).leaveMeeting(1L, 2L, 1L);

        // when & then
        mockMvc.perform(post("/api/v1/studies/1/meetings/2/join")
                        .with(authentication(authUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomToken").value("meeting-2"));

        mockMvc.perform(post("/api/v1/studies/1/meetings/2/leave")
                        .with(authentication(authUser(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("요약 조회/업데이트")
    void summaryEndpoints() throws Exception {
        // given
        MeetingSummaryResponse summary = new MeetingSummaryResponse(
                1L,
                "summary",
                List.of(new MeetingActionItemResponse(1L, "todo", null, ActionItemStatus.TODO)),
                List.of("DP"),
                List.of("주요 내용 1", "주요 내용 2"),
                "DONE",
                LocalDateTime.of(2025, 1, 15, 20, 40)
        );
        when(meetingSttService.getSummary(1L, 2L)).thenReturn(summary);
        when(meetingSttService.upsertSummary(eq(1L), eq(2L), any())).thenReturn(summary);

        // when & then
        mockMvc.perform(get("/api/v1/studies/1/meetings/2/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"));

        MeetingSummaryUpdateRequest updateRequest = new MeetingSummaryUpdateRequest(
                "summary",
                List.of(new MeetingActionItemRequest("todo", null, ActionItemStatus.TODO)),
                List.of("DP"),
                SummaryStatus.DONE
        );
        mockMvc.perform(put("/api/v1/studies/1/meetings/2/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value("summary"));
    }

    @Test
    @DisplayName("채팅 기록 조회")
    void chatHistoryEndpoint() throws Exception {
        // given
        MeetingChatMessageResponse message = new MeetingChatMessageResponse(
                1L,
                1L,
                "user",
                "hello",
                LocalDateTime.of(2025, 1, 15, 19, 2)
        );
        MeetingChatMessagePageResponse page = new MeetingChatMessagePageResponse(
                List.of(message),
                1,
                false
        );
        when(meetingChatService.getChatMessages(eq(1L), eq(2L), any())).thenReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/studies/1/meetings/2/chat")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].senderName").value("user"));
    }

    @Test
    @DisplayName("채팅 삭제")
    void deleteChatMessageEndpoint() throws Exception {
        // given
        doNothing().when(meetingChatService).deleteChatMessage(eq(1L), eq(2L), eq(3L), eq(1L), any());
        doNothing().when(meetingRoomStateService).removeChatMessage("meeting-2", 3L);

        // when & then
        mockMvc.perform(delete("/api/v1/studies/1/meetings/2/chat/3")
                        .with(authentication(authUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Chat deleted"));
    }

    @Test
    @DisplayName("오디오 녹음 업로드/조회")
    void audioRecordingEndpoints() throws Exception {
        // given
        MeetingAudioRecordingResponse response = new MeetingAudioRecordingResponse(
                11L,
                2L,
                1L,
                MeetingAudioTrackType.INDIVIDUAL,
                "/uploads/meetings/2/recordings/audio/users/1/audio.wav",
                "wav",
                123L,
                LocalDateTime.of(2025, 1, 15, 20, 31)
        );
        when(meetingAudioService.uploadRecordingAudio(eq(1L), eq(2L), eq(MeetingAudioTrackType.INDIVIDUAL), eq(1L), any()))
                .thenReturn(response);
        when(meetingAudioService.getAudioRecordings(1L, 2L, MeetingAudioTrackType.INDIVIDUAL, 1L))
                .thenReturn(List.of(response));

        // when & then
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "audio.wav",
                "audio/wav",
                "wav".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/studies/1/meetings/2/recording/audio")
                        .file(audio)
                        .param("trackType", "INDIVIDUAL")
                        .param("userId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.trackType").value("INDIVIDUAL"));

        mockMvc.perform(get("/api/v1/studies/1/meetings/2/recording/audio")
                        .param("trackType", "INDIVIDUAL")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].recordingUrl")
                        .value("/uploads/meetings/2/recordings/audio/users/1/audio.wav"));
    }

    @Test
    @DisplayName("오디오 세그먼트 업로드/병합")
    void audioSegmentEndpoints() throws Exception {
        // given
        MeetingAudioRecordingResponse response = new MeetingAudioRecordingResponse(
                21L,
                2L,
                1L,
                MeetingAudioTrackType.INDIVIDUAL,
                "/uploads/meetings/2/recordings/audio/users/1/audio.wav",
                "wav",
                456L,
                LocalDateTime.of(2025, 1, 15, 20, 40)
        );
        doNothing().when(meetingAudioService).uploadRecordingAudioSegment(eq(1L), eq(2L), eq(1L), any());
        when(meetingAudioService.concatRecordingAudioSegments(1L, 2L, 1L)).thenReturn(response);

        // when & then
        MockMultipartFile audio = new MockMultipartFile(
                "audio",
                "segment.wav",
                "audio/wav",
                "segment".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/studies/1/meetings/2/recording/audio/segment")
                        .file(audio))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.message").value("Audio segment uploaded"));

        mockMvc.perform(post("/api/v1/studies/1/meetings/2/recording/audio/concat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingUrl")
                        .value("/uploads/meetings/2/recordings/audio/users/1/audio.wav"));
    }

    @Test
    @DisplayName("STT 파일 업로드/조회")
    void sttFileEndpoints() throws Exception {
        // given
        MeetingSttFileResponse response = new MeetingSttFileResponse(
                1L,
                2L,
                null,
                MeetingTextTrackType.MIXED,
                "/uploads/meetings/2/stt/mixed/stt.txt",
                LocalDateTime.of(2025, 1, 15, 20, 31),
                LocalDateTime.of(2025, 1, 15, 20, 31)
        );
        when(meetingSttService.uploadSttTextFile(eq(1L), eq(2L), eq(MeetingTextTrackType.MIXED), eq(null), any()))
                .thenReturn(response);
        when(meetingSttService.getMeetingSttFile(1L, 2L, MeetingTextTrackType.MIXED, null)).thenReturn(response);

        // when & then
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "stt.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "text".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/studies/1/meetings/2/stt/file")
                        .file(file)
                        .param("trackType", "MIXED"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileUrl").value("/uploads/meetings/2/stt/mixed/stt.txt"));

        mockMvc.perform(get("/api/v1/studies/1/meetings/2/stt/file")
                        .param("trackType", "MIXED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackType").value("MIXED"));
    }

    @Test
    @DisplayName("요약 파일 업로드/조회")
    void summaryFileEndpoints() throws Exception {
        // given
        MeetingSttSummaryResponse response = new MeetingSttSummaryResponse(
                2L,
                2L,
                null,
                MeetingTextTrackType.MIXED,
                "/uploads/meetings/2/stt/mixed/summary.txt",
                LocalDateTime.of(2025, 1, 15, 20, 32),
                LocalDateTime.of(2025, 1, 15, 20, 32)
        );
        when(meetingSttService.uploadSummaryTextFile(eq(1L), eq(2L), eq(MeetingTextTrackType.MIXED), eq(null), any()))
                .thenReturn(response);
        when(meetingSttService.getMeetingSttSummary(1L, 2L, MeetingTextTrackType.MIXED, null)).thenReturn(response);

        // when & then
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "summary.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "summary".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/studies/1/meetings/2/summary/file")
                        .file(file)
                        .param("trackType", "MIXED"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileUrl").value("/uploads/meetings/2/stt/mixed/summary.txt"));

        mockMvc.perform(get("/api/v1/studies/1/meetings/2/summary/file")
                        .param("trackType", "MIXED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackType").value("MIXED"));
    }

    @Test
    @DisplayName("사진 조회/등록")
    void photoEndpoints() throws Exception {
        // given
        MeetingPhotoResponse photo = new MeetingPhotoResponse(1L, "meeting/2/photo.png",
                LocalDateTime.of(2025, 1, 15, 20, 10), false);
        when(meetingPhotoService.getPhotos(1L, 2L, 1L)).thenReturn(List.of(photo));
        when(meetingPhotoService.addPhoto(eq(1L), eq(2L), eq(1L), any())).thenReturn(photo);

        // when & then
        mockMvc.perform(get("/api/v1/studies/1/meetings/2/photos")
                        .with(authentication(authUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].imageUrl").value("meeting/2/photo.png"));

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "photo.png",
                MediaType.IMAGE_PNG_VALUE,
                "png".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/studies/1/meetings/2/photos")
                        .file(file)
                        .with(authentication(authUser(1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("사진 다중 선택")
    void selectPhotosEndpoint() throws Exception {
        // given
        MeetingPhotoResponse photo = new MeetingPhotoResponse(1L, "meeting/2/photo.png",
                LocalDateTime.of(2025, 1, 15, 20, 10), true);
        when(meetingPhotoService.selectPhotos(eq(1L), eq(2L), eq(1L), eq(List.of(1L, 2L))))
                .thenReturn(List.of(photo));

        // when & then
        MeetingPhotoSelectionRequest request = new MeetingPhotoSelectionRequest(List.of(1L, 2L));
        mockMvc.perform(put("/api/v1/studies/1/meetings/2/photos/selection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].isSelected").value(true));
    }

    @Test
    @DisplayName("키워드 업데이트/음소거")
    void updateKeywordsAndMute() throws Exception {
        // given
        doNothing().when(meetingSttService).updateKeywords(eq(1L), eq(2L), any());
        doNothing().when(meetingService).updateParticipantMute(1L, 2L, 1L, true);

        // when & then
        MeetingKeywordUpdateRequest request = new MeetingKeywordUpdateRequest(List.of("DP"));
        mockMvc.perform(put("/api/v1/studies/1/meetings/2/keywords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Keywords updated"));

        mockMvc.perform(put("/api/v1/studies/1/meetings/2/participants/1/mute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"muted\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Participant updated"));
    }

    @Test
    @DisplayName("녹음 메타 조회/업데이트")
    void recordingEndpoints() throws Exception {
        // given
        MeetingRecordingResponse response = new MeetingRecordingResponse(
                1L,
                "s3://bucket/meeting.wav",
                "wav",
                5400,
                LocalDateTime.of(2025, 1, 15, 19, 0),
                LocalDateTime.of(2025, 1, 15, 20, 30),
                123456L,
                "READY",
                LocalDateTime.of(2025, 1, 15, 20, 31)
        );
        when(meetingRecordingService.getRecording(1L, 2L)).thenReturn(response);
        when(meetingRecordingService.upsertRecording(eq(1L), eq(2L), any())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/studies/1/meetings/2/recording"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));

        MeetingRecordingRequest request = new MeetingRecordingRequest(
                "s3://bucket/meeting.wav",
                "wav",
                5400,
                LocalDateTime.of(2025, 1, 15, 19, 0),
                LocalDateTime.of(2025, 1, 15, 20, 30),
                123456L,
                null
        );
        mockMvc.perform(put("/api/v1/studies/1/meetings/2/recording")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordingUrl").value("s3://bucket/meeting.wav"));
    }

    @Test
    @DisplayName("녹화 영상 업로드")
    void uploadRecordingVideoEndpoint() throws Exception {
        // given
        MeetingRecordingResponse response = new MeetingRecordingResponse(
                1L,
                "/uploads/meetings/2/recordings/video/video.mp4",
                "mp4",
                5400,
                LocalDateTime.of(2025, 1, 15, 19, 0),
                LocalDateTime.of(2025, 1, 15, 20, 30),
                123456L,
                "READY",
                LocalDateTime.of(2025, 1, 15, 20, 31)
        );
        when(meetingRecordingService.uploadRecordingVideo(eq(1L), eq(2L), any())).thenReturn(response);

        // when & then
        MockMultipartFile video = new MockMultipartFile(
                "video",
                "video.mp4",
                "video/mp4",
                "mp4".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/studies/1/meetings/2/recording/video")
                        .file(video))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recordingUrl")
                        .value("/uploads/meetings/2/recordings/video/video.mp4"));
    }

    @Test
    @DisplayName("액션 아이템 조회/생성/수정")
    void actionItemEndpoints() throws Exception {
        // given
        MeetingActionItemResponse response = new MeetingActionItemResponse(
                11L,
                "todo",
                1L,
                ActionItemStatus.TODO
        );
        when(meetingActionItemService.getActionItems(1L, 2L)).thenReturn(List.of(response));
        when(meetingActionItemService.addActionItem(eq(1L), eq(2L), any())).thenReturn(response);
        when(meetingActionItemService.updateActionItem(eq(1L), eq(2L), eq(11L), any()))
                .thenReturn(new MeetingActionItemResponse(11L, "todo", 2L, ActionItemStatus.DONE));

        // when & then
        mockMvc.perform(get("/api/v1/studies/1/meetings/2/action-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(11));

        MeetingActionItemRequest request = new MeetingActionItemRequest("todo", 1L, ActionItemStatus.TODO);
        mockMvc.perform(post("/api/v1/studies/1/meetings/2/action-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assigneeId").value(1));

        mockMvc.perform(put("/api/v1/studies/1/meetings/2/action-items/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MeetingActionItemRequest(null, 2L, ActionItemStatus.DONE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

    @Test
    @DisplayName("내보내기: Markdown/PDF")
    void exportEndpoints() throws Exception {
        // given
        String markdown = "# Meeting Summary";
        byte[] pdf = "pdf".getBytes(StandardCharsets.UTF_8);
        when(meetingExportService.exportMeetingMarkdown(1L, 2L)).thenReturn(markdown);
        when(meetingExportService.exportMeetingPdf(1L, 2L)).thenReturn(pdf);

        // when & then
        mockMvc.perform(get("/api/v1/studies/1/meetings/2/export")
                        .param("format", "MARKDOWN"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/markdown"))
                .andExpect(content().bytes(markdown.getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/v1/studies/1/meetings/2/export")
                        .param("format", "PDF"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));
    }

    private UsernamePasswordAuthenticationToken authUser(Long userId) {
        User user = User.builder()
                .userId("user-" + userId)
                .email("user-" + userId + "@test.local")
                .role(Role.USER)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return new UsernamePasswordAuthenticationToken(new SsafyUserDetails(user), null, List.of());
    }

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new TestAuthenticationPrincipalResolver());
        }
    }

    static class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && SsafyUserDetails.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            User user = User.builder()
                    .userId("user-1")
                    .email("user-1@test.local")
                    .role(Role.USER)
                    .isActive(true)
                    .build();
            ReflectionTestUtils.setField(user, "id", 1L);
            return new SsafyUserDetails(user);
        }
    }
}



