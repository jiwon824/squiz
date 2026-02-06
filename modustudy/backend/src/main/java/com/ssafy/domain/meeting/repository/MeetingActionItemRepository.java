package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.MeetingActionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingActionItemRepository extends JpaRepository<MeetingActionItem, Long> {
    List<MeetingActionItem> findByMeetingId(Long meetingId);
}
