package com.ssafy.domain.meeting.repository;

import com.ssafy.domain.meeting.entity.MeetingSpeechSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingSpeechSegmentRepository extends JpaRepository<MeetingSpeechSegment, Long> {

    /**
     * 미팅의 전체 발화 세그먼트 조회 (시간순 정렬)
     * 미팅 종료 시 전체 transcript 생성에 사용
     */
    List<MeetingSpeechSegment> findByMeetingIdOrderBySpeechTimestampAsc(Long meetingId);

    /**
     * 미팅의 발화 세그먼트 개수
     */
    long countByMeetingId(Long meetingId);

    /**
     * 미팅의 전체 텍스트만 조회 (시간순)
     * Claude API 호출용 transcript 생성에 사용
     */
    @Query("SELECT CONCAT(s.speakerName, ': ', s.text) FROM MeetingSpeechSegment s " +
           "WHERE s.meetingId = :meetingId ORDER BY s.speechTimestamp ASC")
    List<String> findAllTextByMeetingIdOrderByTimestamp(@Param("meetingId") Long meetingId);

    /**
     * 미팅 삭제 시 발화 세그먼트도 삭제
     */
    void deleteByMeetingId(Long meetingId);
}
