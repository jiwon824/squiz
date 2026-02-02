package com.ssafy.domain.meeting.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실시간 발화 세그먼트 요청 DTO
 * AI 서버에서 STT 처리 후 전송
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SpeechSegmentRequest {
    private String userId;      // 발화자 ID (socket.id 또는 displayName)
    private Long timestamp;     // 발화 시작 시간 (Unix timestamp ms)
    private Integer durationMs; // 발화 길이 (ms)
    private String text;        // STT 변환된 텍스트
}
