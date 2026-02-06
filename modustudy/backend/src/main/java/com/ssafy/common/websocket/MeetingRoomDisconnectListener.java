package com.ssafy.common.websocket;

import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class MeetingRoomDisconnectListener {
    // Cleans up in-memory room state on disconnect.
    private final MeetingRoomStateService roomStateService;
    private final SimpMessagingTemplate messagingTemplate;

    public MeetingRoomDisconnectListener(MeetingRoomStateService roomStateService,
                                         SimpMessagingTemplate messagingTemplate) {
        this.roomStateService = roomStateService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        MeetingRoomStateService.SessionLeaveResult leaveResult = roomStateService.markLeftBySession(sessionId);
        if (leaveResult == null) {
            return;
        }
        String roomId = leaveResult.getRoomId();
        MeetingRoomStateService.Participant participant = leaveResult.getParticipant();
        // Broadcast leave events so active clients can update UI state.
        List<MeetingRoomParticipantDto> participants = roomStateService.getActiveParticipants(roomId);
        MeetingRoomEvent leaveEvent = new MeetingRoomEvent(MeetingRoomEvent.Type.LEAVE, roomId);
        leaveEvent.setParticipant(participant.toDto());
        leaveEvent.setParticipants(participants);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", leaveEvent);

        MeetingRoomStateService.PresenterUpdate presenterUpdate = roomStateService.clearPresenterBySession(sessionId);
        if (presenterUpdate != null && roomId.equals(presenterUpdate.getRoomId())) {
            MeetingRoomEvent presenterEvent = new MeetingRoomEvent(MeetingRoomEvent.Type.PRESENTER, roomId);
            presenterEvent.setPresenterName(null);
            presenterEvent.setPresenterId(null);
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", presenterEvent);
        }
    }
}
