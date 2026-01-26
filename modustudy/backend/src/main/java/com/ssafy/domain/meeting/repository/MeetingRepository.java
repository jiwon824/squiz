package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingStatus;
import com.ssafy.domain.meeting.entity.MeetingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Page<Meeting> findByStudyId(Long studyId, Pageable pageable);

    @Query("""
            select m from Meeting m
            where m.studyId = :studyId
              and (:meetingType is null or m.meetingType = :meetingType)
              and (:startAt is null or m.startedAt >= :startAt)
              and (:endAt is null or m.startedAt <= :endAt)
            order by m.startedAt desc
            """)
    Page<Meeting> searchMeetings(@Param("studyId") Long studyId,
                                 @Param("meetingType") MeetingType meetingType,
                                 @Param("startAt") LocalDateTime startAt,
                                 @Param("endAt") LocalDateTime endAt,
                                 Pageable pageable);

    Optional<Meeting> findByIdAndStudyId(Long meetingId, Long studyId);

    boolean existsByStudyIdAndStatus(Long studyId, MeetingStatus status);
}
