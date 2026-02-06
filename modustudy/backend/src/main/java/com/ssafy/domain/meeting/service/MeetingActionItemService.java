package com.ssafy.domain.meeting.service;

import com.ssafy.domain.meeting.dto.request.MeetingActionItemRequest;
import com.ssafy.domain.meeting.dto.response.MeetingActionItemResponse;
import com.ssafy.domain.meeting.entity.ActionItemStatus;
import com.ssafy.domain.meeting.entity.MeetingActionItem;
import com.ssafy.domain.meeting.repository.MeetingActionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingActionItemService {

    private final MeetingActionItemRepository meetingActionItemRepository;
    private final MeetingServiceHelper helper;

    @Transactional(readOnly = true)
    public List<MeetingActionItemResponse> getActionItems(Long studyId, Long meetingId) {
        helper.getMeetingOrThrow(studyId, meetingId);
        return meetingActionItemRepository.findByMeetingId(meetingId).stream()
                .map(item -> new MeetingActionItemResponse(
                        item.getId(),
                        item.getContent(),
                        item.getAssigneeId(),
                        item.getStatus()))
                .toList();
    }

    @Transactional
    public MeetingActionItemResponse addActionItem(Long studyId, Long meetingId, MeetingActionItemRequest request) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ACTION_ITEM_CONTENT_REQUIRED");
        }
        ActionItemStatus status = request.status() == null ? ActionItemStatus.TODO : request.status();
        MeetingActionItem saved = meetingActionItemRepository.save(MeetingActionItem.builder()
                .meetingId(meetingId)
                .content(request.content())
                .assigneeId(request.assigneeId())
                .status(status)
                .build());
        return new MeetingActionItemResponse(
                saved.getId(),
                saved.getContent(),
                saved.getAssigneeId(),
                saved.getStatus());
    }

    @Transactional
    public MeetingActionItemResponse updateActionItem(Long studyId, Long meetingId, Long actionItemId,
                                                      MeetingActionItemRequest request) {
        helper.getMeetingOrThrow(studyId, meetingId);
        MeetingActionItem item = meetingActionItemRepository.findById(actionItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ACTION_ITEM_NOT_FOUND"));
        if (!item.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ACTION_ITEM_MISMATCH");
        }
        if (request.assigneeId() != null) {
            item.updateAssignee(request.assigneeId());
        }
        if (request.status() != null) {
            item.updateStatus(request.status());
        }
        if (request.content() != null) {
            item.updateContent(request.content());
        }
        return new MeetingActionItemResponse(
                item.getId(),
                item.getContent(),
                item.getAssigneeId(),
                item.getStatus());
    }

    @Transactional
    public MeetingActionItem saveActionItem(Long meetingId, String content, Long assigneeId, ActionItemStatus status) {
        return meetingActionItemRepository.save(MeetingActionItem.builder()
                .meetingId(meetingId)
                .content(content)
                .assigneeId(assigneeId)
                .status(status == null ? ActionItemStatus.TODO : status)
                .build());
    }
}
