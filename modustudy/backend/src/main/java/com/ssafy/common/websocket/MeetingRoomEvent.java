package com.ssafy.common.websocket;

import java.time.Instant;
import java.util.List;

public class MeetingRoomEvent {
    // WebSocket event envelope for room presence/chat/presenter updates.
    public enum Type { JOIN, LEAVE, CHAT, CHAT_HISTORY, CHAT_DELETED, PRESENTER, SPEAKING, PRESENCE, MEETING_ENDING, MEETING_ENDED, MEETING_DURATION_UPDATED }

    private Type type;
    private String roomId;
    private Instant createdAt = Instant.now();
    private MeetingRoomParticipantDto participant;
    private List<MeetingRoomParticipantDto> participants;
    private MeetingRoomChatMessage chat;
    private List<MeetingRoomChatMessage> chatHistory;
    private Long deletedChatId;
    private String presenterName;
    private Long presenterId;
    private Integer plannedDurationSeconds;

    public MeetingRoomEvent() {
    }

    public MeetingRoomEvent(Type type, String roomId) {
        this.type = type;
        this.roomId = roomId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public MeetingRoomParticipantDto getParticipant() {
        return participant;
    }

    public void setParticipant(MeetingRoomParticipantDto participant) {
        this.participant = participant;
    }

    public List<MeetingRoomParticipantDto> getParticipants() {
        return participants;
    }

    public void setParticipants(List<MeetingRoomParticipantDto> participants) {
        this.participants = participants;
    }

    public MeetingRoomChatMessage getChat() {
        return chat;
    }

    public void setChat(MeetingRoomChatMessage chat) {
        this.chat = chat;
    }

    public List<MeetingRoomChatMessage> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<MeetingRoomChatMessage> chatHistory) {
        this.chatHistory = chatHistory;
    }

    public String getPresenterName() {
        return presenterName;
    }

    public void setPresenterName(String presenterName) {
        this.presenterName = presenterName;
    }

    public Long getPresenterId() {
        return presenterId;
    }

    public void setPresenterId(Long presenterId) {
        this.presenterId = presenterId;
    }

    public Long getDeletedChatId() {
        return deletedChatId;
    }

    public void setDeletedChatId(Long deletedChatId) {
        this.deletedChatId = deletedChatId;
    }

    public Integer getPlannedDurationSeconds() {
        return plannedDurationSeconds;
    }

    public void setPlannedDurationSeconds(Integer plannedDurationSeconds) {
        this.plannedDurationSeconds = plannedDurationSeconds;
    }
}
