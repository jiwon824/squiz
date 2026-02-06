package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.MeetingSttFile;
import com.ssafy.domain.meeting.entity.MeetingTextTrackType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSttFileRepository extends JpaRepository<MeetingSttFile, Long> {
    Optional<MeetingSttFile> findByMeetingIdAndTrackTypeAndUserId(Long meetingId,
                                                                  MeetingTextTrackType trackType,
                                                                  Long userId);

    Optional<MeetingSttFile> findByMeetingIdAndTrackTypeAndUserIdIsNull(Long meetingId,
                                                                        MeetingTextTrackType trackType);

    boolean existsByMeetingIdAndTrackTypeAndUserIdIsNull(Long meetingId, MeetingTextTrackType trackType);
}
