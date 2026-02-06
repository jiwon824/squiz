package com.ssafy.domain.meeting.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 실시간 발화 세그먼트 (STT 결과)
 * 미팅 중 발화 단위로 저장되며, 미팅 종료 시 timestamp 순으로 정렬하여 전체 transcript 생성
 */
 @Entity
 @Table(name = "meeting_speech_segment", indexes = {
    @Index(name = "idx_speech_segment_meeting_timestamp", columnList = "meeting_id, speech_timestamp")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingSpeechSegment extends BaseEntity {

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "speaker_id", nullable = false, length = 100)
    private String speakerId;  // socket.id 또는 displayName

    @Column(name = "speaker_name", length = 50)
    private String speakerName;  // 표시 이름 (닉네임)

    @Column(name = "speech_timestamp", nullable = false)
    private Long speechTimestamp;  // 발화 시작 시간 (Unix timestamp ms)

    @Column(name = "duration_ms")
    private Integer durationMs;  // 발화 길이 (ms)

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;  // STT 변환된 텍스트

    @Builder
    private MeetingSpeechSegment(Long meetingId, String speakerId, String speakerName,
                                  Long speechTimestamp, Integer durationMs, String text) {
        this.meetingId = meetingId;
        this.speakerId = speakerId;
        this.speakerName = speakerName;
        this.speechTimestamp = speechTimestamp;
        this.durationMs = durationMs;
        this.text = text;
    }

    public static MeetingSpeechSegment create(Long meetingId, String speakerId, String speakerName,
                                               Long speechTimestamp, Integer durationMs, String text) {
        return MeetingSpeechSegment.builder()
                .meetingId(meetingId)
                .speakerId(speakerId)
                .speakerName(speakerName)
                .speechTimestamp(speechTimestamp)
                .durationMs(durationMs)
                .text(text)
                .build();
    }
}
