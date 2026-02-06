package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.MeetingSttSummary;
import com.ssafy.domain.meeting.entity.MeetingTextTrackType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSttSummaryRepository extends JpaRepository<MeetingSttSummary, Long> {
    Optional<MeetingSttSummary> findByMeetingIdAndTrackTypeAndUserId(Long meetingId,
                                                                     MeetingTextTrackType trackType,
                                                                     Long userId);

    Optional<MeetingSttSummary> findByMeetingIdAndTrackTypeAndUserIdIsNull(Long meetingId,
                                                                           MeetingTextTrackType trackType);

    boolean existsByMeetingIdAndTrackTypeAndUserIdIsNull(Long meetingId, MeetingTextTrackType trackType);
}
