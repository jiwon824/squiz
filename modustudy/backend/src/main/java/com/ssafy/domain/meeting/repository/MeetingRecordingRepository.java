package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.MeetingRecording;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingRecordingRepository extends JpaRepository<MeetingRecording, Long> {
    Optional<MeetingRecording> findByMeetingId(Long meetingId);
}
