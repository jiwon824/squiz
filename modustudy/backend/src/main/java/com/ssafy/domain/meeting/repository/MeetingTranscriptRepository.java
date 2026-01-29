package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.MeetingTranscript;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingTranscriptRepository extends JpaRepository<MeetingTranscript, Long> {

    /**
     * 미팅의 전체 트랜스크립트 조회 (시간순)
     */
    List<MeetingTranscript> findByMeetingIdOrderByTimestampSecondsAsc(Long meetingId);

    /**
     * 미팅의 전체 트랜스크립트 조회 (페이징)
     */
    Page<MeetingTranscript> findByMeetingIdOrderByTimestampSecondsAsc(Long meetingId, Pageable pageable);

    /**
     * 특정 사용자의 발언만 조회
     */
    List<MeetingTranscript> findByMeetingIdAndUserIdOrderByTimestampSecondsAsc(Long meetingId, Long userId);

    /**
     * 미팅의 트랜스크립트 개수
     */
    long countByMeetingId(Long meetingId);

    /**
     * 미팅의 전체 텍스트 조회 (요약용)
     */
    @Query("SELECT t.content FROM MeetingTranscript t WHERE t.meetingId = :meetingId ORDER BY t.timestampSeconds ASC")
    List<String> findAllContentByMeetingId(@Param("meetingId") Long meetingId);

    /**
     * 미팅 삭제 시 트랜스크립트도 삭제
     */
    void deleteByMeetingId(Long meetingId);
}
