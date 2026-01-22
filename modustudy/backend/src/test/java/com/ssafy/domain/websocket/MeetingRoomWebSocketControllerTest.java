package com.ssafy.domain.websocket;

import com.ssafy.common.websocket.MeetingRoomChatMessage;
import com.ssafy.common.websocket.MeetingRoomEvent;
import com.ssafy.common.websocket.MeetingRoomJoinRequest;
import com.ssafy.common.websocket.MeetingRoomParticipantDto;
import com.ssafy.common.websocket.MeetingRoomPresenceRequest;
import com.ssafy.common.websocket.MeetingRoomPresenterRequest;
import com.ssafy.common.websocket.MeetingRoomSpeakingRequest;
import com.ssafy.common.websocket.MeetingRoomStateService;
import com.ssafy.common.websocket.MeetingRoomWebSocketController;
import com.ssafy.domain.meeting.service.MeetingService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeetingRoomWebSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MeetingService meetingService;

    private MeetingRoomStateService stateService;
    private MeetingRoomWebSocketController controller;

    @BeforeEach
    void setUp() {
        stateService = new MeetingRoomStateService();
        controller = new MeetingRoomWebSocketController(stateService, messagingTemplate, meetingService);
    }

    @Test
    @DisplayName("입장 시 참가자 목록과 히스토리 전송")
    void joinRoom_sendsJoinAndHistory() {
        String roomId = "room-1";
        String sessionId = "s1";
        MeetingRoomChatMessage history = new MeetingRoomChatMessage();
        history.setSender("Bob");
        history.setText("hi");
        stateService.addChatMessage(roomId, history);

        MeetingRoomJoinRequest request = new MeetingRoomJoinRequest();
        request.setDisplayName("Alice");
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);

        controller.joinRoom(roomId, request, accessor);

        ArgumentCaptor<MeetingRoomEvent> eventCaptor = ArgumentCaptor.forClass(MeetingRoomEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + roomId + "/events"), eventCaptor.capture());
        verify(messagingTemplate).convertAndSendToUser(eq(sessionId), eq("/queue/rooms/" + roomId + "/history"),
                any(MeetingRoomEvent.class), any(MessageHeaders.class));

        MeetingRoomEvent event = eventCaptor.getValue();
        MeetingRoomParticipantDto participant = event.getParticipant();
        assertThat(event.getType()).isEqualTo(MeetingRoomEvent.Type.JOIN);
        assertThat(participant.getDisplayName()).isEqualTo("Alice");
        assertThat(event.getParticipants()).hasSize(1);
    }

    @Test
    @DisplayName("채팅 전송 시 브로드캐스트 및 저장 요청")
    void sendChat_broadcastsAndPersists() {
        MeetingRoomChatMessage message = new MeetingRoomChatMessage();
        message.setUserId(5L);
        message.setSender("Alice");
        message.setText("hello");
        message.setSentAt(Instant.parse("2025-01-01T00:00:00Z"));

        controller.sendChat("meeting-2", message);

        verify(meetingService).addChatMessage(2L, 5L, "Alice", "hello", message.getSentAt());
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/meeting-2/events"), any(MeetingRoomEvent.class));
    }

    @Test
    @DisplayName("채팅 전송 - 미팅 룸이 아니면 저장 요청 없음")
    void sendChat_nonMeetingRoomDoesNotPersist() {
        MeetingRoomChatMessage message = new MeetingRoomChatMessage();
        message.setSender("Alice");
        message.setText("hello");

        controller.sendChat("room-1", message);

        verify(meetingService, never()).addChatMessage(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("발표자 설정 이벤트")
    void setPresenter_claimsPresenter() {
        String roomId = "room-1";
        String sessionId = "s1";
        stateService.joinRoom(roomId, "Alice", sessionId);

        MeetingRoomPresenterRequest request = new MeetingRoomPresenterRequest();
        request.setDisplayName("Alice");
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);

        controller.setPresenter(roomId, request, accessor);

        ArgumentCaptor<MeetingRoomEvent> eventCaptor = ArgumentCaptor.forClass(MeetingRoomEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + roomId + "/events"), eventCaptor.capture());
        MeetingRoomEvent event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(MeetingRoomEvent.Type.PRESENTER);
        assertThat(event.getPresenterName()).isEqualTo("Alice");
        assertThat(event.getPresenterId()).isNotNull();
    }

    @Test
    @DisplayName("발표자 해제 이벤트")
    void setPresenter_releasePresenter() {
        String roomId = "room-1";
        String sessionId = "s1";
        stateService.joinRoom(roomId, "Alice", sessionId);
        stateService.claimPresenter(roomId, sessionId, "Alice", 1L);

        MeetingRoomPresenterRequest request = new MeetingRoomPresenterRequest();
        request.setDisplayName("Alice");
        request.setAction("release");
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);

        controller.setPresenter(roomId, request, accessor);

        ArgumentCaptor<MeetingRoomEvent> eventCaptor = ArgumentCaptor.forClass(MeetingRoomEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + roomId + "/events"), eventCaptor.capture());
        MeetingRoomEvent event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(MeetingRoomEvent.Type.PRESENTER);
        assertThat(event.getPresenterName()).isNull();
        assertThat(event.getPresenterId()).isNull();
    }

    @Test
    @DisplayName("발언/프레즌스 업데이트 이벤트")
    void updateSpeakingAndPresence_broadcasts() {
        String roomId = "room-1";
        String sessionId = "s1";
        stateService.joinRoom(roomId, "Alice", sessionId);

        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);

        MeetingRoomSpeakingRequest speakingRequest = new MeetingRoomSpeakingRequest();
        speakingRequest.setSpeaking(true);
        controller.updateSpeaking(roomId, speakingRequest, accessor);

        MeetingRoomPresenceRequest presenceRequest = new MeetingRoomPresenceRequest();
        presenceRequest.setPresent(true);
        controller.updatePresence(roomId, presenceRequest, accessor);

        ArgumentCaptor<MeetingRoomEvent> eventCaptor = ArgumentCaptor.forClass(MeetingRoomEvent.class);
        verify(messagingTemplate, org.mockito.Mockito.times(2))
                .convertAndSend(eq("/topic/rooms/" + roomId + "/events"), eventCaptor.capture());

        List<MeetingRoomEvent> events = eventCaptor.getAllValues();
        assertThat(events).extracting(MeetingRoomEvent::getType)
                .contains(MeetingRoomEvent.Type.SPEAKING, MeetingRoomEvent.Type.PRESENCE);
    }
}
