package com.ssafy.domain.meeting.service;

import com.ssafy.domain.meeting.dto.response.MeetingChatMessagePageResponse;
import com.ssafy.domain.meeting.dto.response.MeetingChatMessageResponse;
import com.ssafy.domain.meeting.entity.MeetingChatMessage;
import com.ssafy.domain.meeting.repository.MeetingChatMessageRepository;
import com.ssafy.domain.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingChatService {

    private final MeetingRepository meetingRepository;
    private final MeetingChatMessageRepository meetingChatMessageRepository;
    private final MeetingServiceHelper helper;

    @Transactional(readOnly = true)
    public MeetingChatMessagePageResponse getChatMessages(Long studyId, Long meetingId, Pageable pageable) {
        helper.getMeetingOrThrow(studyId, meetingId);
        Page<MeetingChatMessage> page = meetingChatMessageRepository
                .findByMeetingIdOrderBySentAtAsc(meetingId, pageable);
        List<MeetingChatMessageResponse> content = page.stream()
                .map(message -> new MeetingChatMessageResponse(
                        message.getId(),
                        message.getUserId(),
                        message.getSenderName(),
                        message.getContent(),
                        message.getSentAt()))
                .toList();
        return new MeetingChatMessagePageResponse(content, page.getTotalElements(), page.hasNext());
    }

    @Transactional
    public void addChatMessage(Long meetingId, Long userId, String senderName, String content, Instant sentAt) {
        if (meetingRepository.findById(meetingId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND");
        }
        LocalDateTime sentAtTime = sentAt == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(sentAt, ZoneId.systemDefault());
        meetingChatMessageRepository.save(MeetingChatMessage.builder()
                .meetingId(meetingId)
                .userId(userId)
                .senderName(senderName)
                .content(content)
                .sentAt(sentAtTime)
                .build());
    }

    @Transactional
    public MeetingChatMessage addChatMessageAndReturn(Long meetingId, Long userId, String senderName, String content, Instant sentAt) {
        if (meetingRepository.findById(meetingId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND");
        }
        LocalDateTime sentAtTime = sentAt == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(sentAt, ZoneId.systemDefault());
        return meetingChatMessageRepository.save(MeetingChatMessage.builder()
                .meetingId(meetingId)
                .userId(userId)
                .senderName(senderName)
                .content(content)
                .sentAt(sentAtTime)
                .build());
    }

    @Transactional
    public void deleteChatMessage(Long studyId, Long meetingId, Long messageId, Long userId, String requesterName) {
        helper.getMeetingOrThrow(studyId, meetingId);
        MeetingChatMessage message = meetingChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CHAT_NOT_FOUND"));
        if (!meetingId.equals(message.getMeetingId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CHAT_MEETING_MISMATCH");
        }
        boolean allowed = false;
        if (userId != null && message.getUserId() != null && message.getUserId().equals(userId)) {
            allowed = true;
        } else if (requesterName != null && requesterName.equals(message.getSenderName())) {
            allowed = true;
        }
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CHAT_DELETE_FORBIDDEN");
        }
        meetingChatMessageRepository.delete(message);
    }
}
