package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.MeetingAudioRecording;
import com.ssafy.domain.meeting.entity.MeetingAudioTrackType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingAudioRecordingRepository extends JpaRepository<MeetingAudioRecording, Long> {
    List<MeetingAudioRecording> findByMeetingIdOrderByCreatedAtAsc(Long meetingId);

    List<MeetingAudioRecording> findByMeetingIdAndTrackTypeOrderByCreatedAtAsc(Long meetingId,
                                                                              MeetingAudioTrackType trackType);

    List<MeetingAudioRecording> findByMeetingIdAndUserIdOrderByCreatedAtAsc(Long meetingId, Long userId);

    List<MeetingAudioRecording> findByMeetingIdAndTrackTypeAndUserIdOrderByCreatedAtAsc(
            Long meetingId,
            MeetingAudioTrackType trackType,
            Long userId);

    Optional<MeetingAudioRecording> findTopByMeetingIdAndTrackTypeAndUserIdOrderByCreatedAtDesc(
            Long meetingId,
            MeetingAudioTrackType trackType,
            Long userId);

    Optional<MeetingAudioRecording> findTopByMeetingIdAndTrackTypeAndUserIdIsNullOrderByCreatedAtDesc(
            Long meetingId,
            MeetingAudioTrackType trackType);
}
