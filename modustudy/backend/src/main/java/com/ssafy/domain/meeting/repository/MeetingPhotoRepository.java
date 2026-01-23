package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.MeetingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingPhotoRepository extends JpaRepository<MeetingPhoto, Long> {
    List<MeetingPhoto> findByMeetingIdOrderByCapturedAtDesc(Long meetingId);

    int countByMeetingId(Long meetingId);

    Optional<MeetingPhoto> findFirstByMeetingIdAndIsSelectedTrue(Long meetingId);
}
