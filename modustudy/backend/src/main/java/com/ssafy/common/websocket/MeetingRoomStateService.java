package com.ssafy.common.websocket;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class MeetingRoomStateService {
    // In-memory room state for local/dev use; replace with persistent store if needed.
    private static final int CHAT_HISTORY_LIMIT = 200;
    private final AtomicLong participantIdSequence = new AtomicLong(1);
    private final Map<String, Map<String, Participant>> participantsByRoom = new ConcurrentHashMap<>();
    private final Map<String, RoomSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, PresenterInfo> presenters = new ConcurrentHashMap<>();
    private final Map<String, ArrayDeque<MeetingRoomChatMessage>> chatHistory = new ConcurrentHashMap<>();

    public Participant joinRoom(String roomId, String displayName, String sessionId) {
        long participantId = participantIdSequence.getAndIncrement();
        Participant participant = new Participant(participantId, displayName, sessionId);
        participantsByRoom
                .computeIfAbsent(roomId, key -> new ConcurrentHashMap<>())
                .put(sessionId, participant);
        sessions.put(sessionId, new RoomSession(roomId, participant));
        return participant;
    }

    public List<MeetingRoomParticipantDto> getActiveParticipants(String roomId) {
        Map<String, Participant> room = participantsByRoom.get(roomId);
        if (room == null) {
            return List.of();
        }
        List<MeetingRoomParticipantDto> participants = new ArrayList<>();
        for (Participant participant : room.values()) {
            if (participant.active) {
                participants.add(participant.toDto());
            }
        }
        return participants;
    }

    public SessionLeaveResult markLeftBySession(String sessionId) {
        RoomSession session = sessions.remove(sessionId);
        if (session == null) {
            return null;
        }
        session.participant.active = false;
        return new SessionLeaveResult(session.roomId, session.participant);
    }

    public PresenterInfo claimPresenter(String roomId, String sessionId, String displayName, Long participantId) {
        PresenterInfo info = new PresenterInfo(sessionId, displayName, participantId);
        presenters.put(roomId, info);
        return info;
    }

    public Long getParticipantIdBySession(String sessionId) {
        RoomSession session = sessions.get(sessionId);
        return session == null ? null : session.participant.id;
    }

    public PresenterInfo releasePresenter(String roomId, String sessionId) {
        PresenterInfo info = presenters.get(roomId);
        if (info != null && info.sessionId.equals(sessionId)) {
            presenters.remove(roomId);
            return null;
        }
        return info;
    }

    public PresenterInfo releasePresenterByDisplayName(String roomId, String displayName) {
        PresenterInfo info = presenters.get(roomId);
        if (info != null && info.displayName.equals(displayName)) {
            presenters.remove(roomId);
            return null;
        }
        return info;
    }

    public PresenterUpdate clearPresenterBySession(String sessionId) {
        for (Map.Entry<String, PresenterInfo> entry : presenters.entrySet()) {
            PresenterInfo info = entry.getValue();
            if (info.sessionId.equals(sessionId)) {
                presenters.remove(entry.getKey());
                return new PresenterUpdate(entry.getKey(), info.displayName, info.participantId);
            }
        }
        return null;
    }

    public PresenterInfo getPresenter(String roomId) {
        return presenters.get(roomId);
    }

    public Participant updateSpeaking(String roomId, String sessionId, boolean speaking) {
        RoomSession session = sessions.get(sessionId);
        if (session == null || !session.roomId.equals(roomId)) {
            return null;
        }
        session.participant.isSpeaking = speaking;
        return session.participant;
    }

    public Participant updatePresence(String roomId, String sessionId, boolean present) {
        RoomSession session = sessions.get(sessionId);
        if (session == null || !session.roomId.equals(roomId)) {
            return null;
        }
        session.participant.isPresent = present;
        return session.participant;
    }

    public void addChatMessage(String roomId, MeetingRoomChatMessage chatMessage) {
        ArrayDeque<MeetingRoomChatMessage> history = chatHistory.computeIfAbsent(roomId, key -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(chatMessage);
            while (history.size() > CHAT_HISTORY_LIMIT) {
                history.removeFirst();
            }
        }
    }

    public void removeChatMessage(String roomId, Long messageId) {
        if (messageId == null) {
            return;
        }
        ArrayDeque<MeetingRoomChatMessage> history = chatHistory.get(roomId);
        if (history == null) {
            return;
        }
        synchronized (history) {
            history.removeIf(message -> messageId.equals(message.getId()));
        }
    }

    public List<MeetingRoomChatMessage> getChatHistory(String roomId) {
        ArrayDeque<MeetingRoomChatMessage> history = chatHistory.get(roomId);
        if (history == null) {
            return List.of();
        }
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public static class PresenterUpdate {
        private final String roomId;
        private final String displayName;
        private final Long participantId;

        public PresenterUpdate(String roomId, String displayName, Long participantId) {
            this.roomId = roomId;
            this.displayName = displayName;
            this.participantId = participantId;
        }

        public String getRoomId() {
            return roomId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Long getParticipantId() {
            return participantId;
        }
    }

    public static class PresenterInfo {
        private final String sessionId;
        private final String displayName;
        private final Long participantId;

        public PresenterInfo(String sessionId, String displayName, Long participantId) {
            this.sessionId = sessionId;
            this.displayName = displayName;
            this.participantId = participantId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Long getParticipantId() {
            return participantId;
        }
    }

    public static class Participant {
        private final Long id;
        private final String displayName;
        private final String sessionId;
        private volatile boolean active = true;
        private volatile boolean isSpeaking = false;
        private volatile boolean isPresent = false;

        public Participant(Long id, String displayName, String sessionId) {
            this.id = id;
            this.displayName = displayName;
            this.sessionId = sessionId;
        }

        public MeetingRoomParticipantDto toDto() {
            return new MeetingRoomParticipantDto(id, displayName, active, isSpeaking, isPresent);
        }
    }

    private static class RoomSession {
        private final String roomId;
        private final Participant participant;

        private RoomSession(String roomId, Participant participant) {
            this.roomId = roomId;
            this.participant = participant;
        }
    }

    public static class SessionLeaveResult {
        private final String roomId;
        private final Participant participant;

        public SessionLeaveResult(String roomId, Participant participant) {
            this.roomId = roomId;
            this.participant = participant;
        }

        public String getRoomId() {
            return roomId;
        }

        public Participant getParticipant() {
            return participant;
        }
    }
}
