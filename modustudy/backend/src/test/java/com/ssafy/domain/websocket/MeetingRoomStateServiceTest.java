package com.ssafy.domain.websocket;

import com.ssafy.common.websocket.MeetingRoomChatMessage;
import com.ssafy.common.websocket.MeetingRoomParticipantDto;
import com.ssafy.common.websocket.MeetingRoomStateService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingRoomStateServiceTest {

    private MeetingRoomStateService service;

    @BeforeEach
    void setUp() {
        service = new MeetingRoomStateService();
    }

    @Test
    @DisplayName("참가자 입장 시 활성 목록에 포함")
    void joinRoom_addsActiveParticipant() {
        service.joinRoom("room-1", "Alice", "s1");

        List<MeetingRoomParticipantDto> participants = service.getActiveParticipants("room-1");

        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).getDisplayName()).isEqualTo("Alice");
        assertThat(participants.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("세션 종료 시 참가자 비활성화")
    void markLeftBySession_marksInactive() {
        service.joinRoom("room-1", "Alice", "s1");

        MeetingRoomStateService.SessionLeaveResult result = service.markLeftBySession("s1");

        assertThat(result).isNotNull();
        assertThat(result.getRoomId()).isEqualTo("room-1");
        assertThat(service.getActiveParticipants("room-1")).isEmpty();
    }

    @Test
    @DisplayName("발표자 설정 및 해제")
    void presenterClaimAndRelease() {
        service.joinRoom("room-1", "Alice", "s1");

        MeetingRoomStateService.PresenterInfo claimed =
                service.claimPresenter("room-1", "s1", "Alice", 1L);

        assertThat(claimed.getDisplayName()).isEqualTo("Alice");
        assertThat(service.getPresenter("room-1")).isNotNull();

        MeetingRoomStateService.PresenterInfo released = service.releasePresenter("room-1", "s1");

        assertThat(released).isNull();
        assertThat(service.getPresenter("room-1")).isNull();
    }

    @Test
    @DisplayName("채팅 히스토리 최대 200개 유지")
    void chatHistoryLimitsTo200() {
        for (int i = 1; i <= 205; i += 1) {
            MeetingRoomChatMessage message = new MeetingRoomChatMessage();
            message.setSender("user");
            message.setText("msg-" + i);
            service.addChatMessage("room-1", message);
        }

        List<MeetingRoomChatMessage> history = service.getChatHistory("room-1");

        assertThat(history).hasSize(200);
        assertThat(history.get(0).getText()).isEqualTo("msg-6");
        assertThat(history.get(history.size() - 1).getText()).isEqualTo("msg-205");
    }

    @Test
    @DisplayName("발언/프레즌스 상태 업데이트")
    void updateSpeakingAndPresence() {
        service.joinRoom("room-1", "Alice", "s1");

        service.updateSpeaking("room-1", "s1", true);
        service.updatePresence("room-1", "s1", true);

        List<MeetingRoomParticipantDto> participants = service.getActiveParticipants("room-1");

        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).isSpeaking()).isTrue();
        assertThat(participants.get(0).isPresent()).isTrue();
    }
}
