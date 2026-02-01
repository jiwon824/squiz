package com.ssafy.domain.meeting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실시간 발화 세그먼트 응답 DTO
 * WebSocket 브로드캐스트 시에도 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeechSegmentResponse {
    private Long meetingId;     // 미팅 ID
    private String speakerId;   // 발화자 ID
    private String speakerName; // 발화자 이름 (매핑 가능 시)
    private Long timestamp;     // 발화 시작 시간
    private Integer durationMs; // 발화 길이
    private String text;        // STT 텍스트
}
