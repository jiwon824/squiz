package com.ssafy.domain.websocket;

import com.ssafy.common.websocket.MeetingRoomDisconnectListener;
import com.ssafy.common.websocket.MeetingRoomEvent;
import com.ssafy.common.websocket.MeetingRoomStateService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MeetingRoomDisconnectListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private MeetingRoomStateService stateService;
    private MeetingRoomDisconnectListener listener;

    @BeforeEach
    void setUp() {
        stateService = new MeetingRoomStateService();
        listener = new MeetingRoomDisconnectListener(stateService, messagingTemplate);
    }

    @Test
    @DisplayName("세션 종료 시 LEAVE/PRESENTER 이벤트 전송")
    void handleSessionDisconnect_sendsEvents() {
        String roomId = "room-1";
        String sessionId = "s1";
        stateService.joinRoom(roomId, "Alice", sessionId);
        stateService.claimPresenter(roomId, sessionId, "Alice", 1L);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL);

        listener.handleSessionDisconnect(event);

        ArgumentCaptor<MeetingRoomEvent> captor = ArgumentCaptor.forClass(MeetingRoomEvent.class);
        verify(messagingTemplate, times(2))
                .convertAndSend(eq("/topic/rooms/" + roomId + "/events"), captor.capture());

        List<MeetingRoomEvent> events = captor.getAllValues();
        assertThat(events).extracting(MeetingRoomEvent::getType)
                .contains(MeetingRoomEvent.Type.LEAVE, MeetingRoomEvent.Type.PRESENTER);
    }

    @Test
    @DisplayName("알 수 없는 세션 종료 시 이벤트 없음")
    void handleSessionDisconnect_unknownSession() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "missing", CloseStatus.NORMAL);

        listener.handleSessionDisconnect(event);

        verifyNoInteractions(messagingTemplate);
    }
}
