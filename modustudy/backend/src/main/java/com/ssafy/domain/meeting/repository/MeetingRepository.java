package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingStatus;
import com.ssafy.domain.meeting.entity.MeetingType;
import com.ssafy.domain.meeting.entity.SummaryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    boolean existsBySessionId(Long sessionId);

    /**
     * AI 처리가 필요한 미팅 목록 조회
     * - status = ENDED
     * - summaryStatus = PROCESSING
     */
    List<Meeting> findByStatusAndSummaryStatus(MeetingStatus status, SummaryStatus summaryStatus);

    List<Meeting> findTop200ByStatusOrderByEndedAtDesc(MeetingStatus status);
}
