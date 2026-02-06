package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.MeetingChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingChatMessageRepository extends JpaRepository<MeetingChatMessage, Long> {
    Page<MeetingChatMessage> findByMeetingIdOrderBySentAtAsc(Long meetingId, Pageable pageable);
}
