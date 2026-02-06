package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    Optional<MeetingParticipant> findTopByMeetingIdAndUserIdOrderByJoinedAtDesc(Long meetingId, Long userId);

    List<MeetingParticipant> findByMeetingId(Long meetingId);

    int countByMeetingId(Long meetingId);
}
