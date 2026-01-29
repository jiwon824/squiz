package com.ssafy.common.websocket;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.stereotype.Controller;
import com.ssafy.domain.meeting.service.MeetingChatService;
import java.util.Optional;

@Controller
public class MeetingRoomWebSocketController {
    // Handles room join/chat/presenter events over STOMP.
    private final MeetingRoomStateService roomStateService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeetingChatService meetingChatService;

    public MeetingRoomWebSocketController(MeetingRoomStateService roomStateService,
                                          SimpMessagingTemplate messagingTemplate,
                                          MeetingChatService meetingChatService) {
        this.roomStateService = roomStateService;
        this.messagingTemplate = messagingTemplate;
        this.meetingChatService = meetingChatService;
    }

    @MessageMapping("/rooms/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId,
                         @Valid MeetingRoomJoinRequest request,
                         SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        // Track participants in-memory; a database-backed model can be added later.
        MeetingRoomStateService.Participant participant =
                roomStateService.joinRoom(roomId, request.getDisplayName(), sessionId);
        List<MeetingRoomParticipantDto> participants = roomStateService.getActiveParticipants(roomId);

        MeetingRoomEvent event = new MeetingRoomEvent(MeetingRoomEvent.Type.JOIN, roomId);
        event.setParticipant(participant.toDto());
        event.setParticipants(participants);
        MeetingRoomStateService.PresenterInfo presenter = roomStateService.getPresenter(roomId);
        if (presenter != null) {
            event.setPresenterName(presenter.getDisplayName());
            event.setPresenterId(presenter.getParticipantId());
        }
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", event);

        List<MeetingRoomChatMessage> history = roomStateService.getChatHistory(roomId);
        if (!history.isEmpty()) {
            MeetingRoomEvent historyEvent = new MeetingRoomEvent(MeetingRoomEvent.Type.CHAT_HISTORY, roomId);
            historyEvent.setChatHistory(history);
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/rooms/" + roomId + "/history",
                    historyEvent,
                    createHeaders(sessionId)
            );
        }
    }

    @MessageMapping("/rooms/{roomId}/chat")
    public void sendChat(@DestinationVariable String roomId, @Valid MeetingRoomChatMessage chatMessage) {
        roomStateService.addChatMessage(roomId, chatMessage);
        parseMeetingId(roomId).ifPresent(meetingId -> meetingChatService.addChatMessage(
                meetingId,
                chatMessage.getUserId(),
                chatMessage.getSender(),
                chatMessage.getText(),
                chatMessage.getSentAt()
        ));
        MeetingRoomEvent event = new MeetingRoomEvent(MeetingRoomEvent.Type.CHAT, roomId);
        event.setChat(chatMessage);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", event);
    }

    @MessageMapping("/rooms/{roomId}/presenter")
    public void setPresenter(@DestinationVariable String roomId,
                             @Valid MeetingRoomPresenterRequest request,
                             SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String action = request.getAction() == null ? "claim" : request.getAction();
        MeetingRoomEvent event = new MeetingRoomEvent(MeetingRoomEvent.Type.PRESENTER, roomId);
        if ("release".equalsIgnoreCase(action)) {
            MeetingRoomStateService.PresenterInfo info = roomStateService.releasePresenter(roomId, sessionId);
            if (info != null && request.getDisplayName() != null) {
                info = roomStateService.releasePresenterByDisplayName(roomId, request.getDisplayName());
            }
            if (info != null) {
                event.setPresenterName(info.getDisplayName());
                event.setPresenterId(info.getParticipantId());
            }
        } else {
            Long participantId = roomStateService.getParticipantIdBySession(sessionId);
            MeetingRoomStateService.PresenterInfo info =
                    roomStateService.claimPresenter(roomId, sessionId, request.getDisplayName(), participantId);
            event.setPresenterName(info.getDisplayName());
            event.setPresenterId(info.getParticipantId());
        }
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", event);
    }

    @MessageMapping("/rooms/{roomId}/speaking")
    public void updateSpeaking(@DestinationVariable String roomId,
                               @Valid MeetingRoomSpeakingRequest request,
                               SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        MeetingRoomStateService.Participant participant =
                roomStateService.updateSpeaking(roomId, sessionId, Boolean.TRUE.equals(request.getSpeaking()));
        if (participant == null) {
            return;
        }
        MeetingRoomEvent event = new MeetingRoomEvent(MeetingRoomEvent.Type.SPEAKING, roomId);
        event.setParticipant(participant.toDto());
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", event);
    }

    @MessageMapping("/rooms/{roomId}/presence")
    public void updatePresence(@DestinationVariable String roomId,
                               @Valid MeetingRoomPresenceRequest request,
                               SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        MeetingRoomStateService.Participant participant =
                roomStateService.updatePresence(roomId, sessionId, Boolean.TRUE.equals(request.getPresent()));
        if (participant == null) {
            return;
        }
        MeetingRoomEvent event = new MeetingRoomEvent(MeetingRoomEvent.Type.PRESENCE, roomId);
        event.setParticipant(participant.toDto());
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", event);
    }

    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        return accessor.getMessageHeaders();
    }

    private Optional<Long> parseMeetingId(String roomId) {
        if (roomId == null || !roomId.startsWith("meeting-")) {
            return Optional.empty();
        }
        String rawId = roomId.substring("meeting-".length());
        try {
            return Optional.of(Long.parseLong(rawId));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
