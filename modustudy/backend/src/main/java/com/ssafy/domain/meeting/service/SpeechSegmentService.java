package com.ssafy.domain.meeting.service;

import com.ssafy.domain.meeting.dto.request.SpeechSegmentRequest;
import com.ssafy.domain.meeting.dto.response.SpeechSegmentResponse;
import com.ssafy.domain.meeting.entity.MeetingSpeechSegment;
import com.ssafy.domain.meeting.repository.MeetingSpeechSegmentRepository;
import com.ssafy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 실시간 발화 세그먼트 처리 서비스
 * AI 서버에서 STT 처리된 발화 세그먼트를 DB에 저장
 * 미팅 종료 시 timestamp 순으로 정렬하여 전체 transcript 제공
 */
 @Slf4j
 @Service
 @RequiredArgsConstructor
 public class SpeechSegmentService {

    private final MeetingSpeechSegmentRepository speechSegmentRepository;
    private final UserRepository userRepository;

    /**
     * 발화 세그먼트 처리 및 DB 저장
     * @param meetingId 미팅 ID
     * @param request AI 서버에서 전송한 발화 세그먼트 정보
     * @return 처리 결과
     */
    @Transactional
    public SpeechSegmentResponse processSpeechSegment(Long meetingId, SpeechSegmentRequest request) {
// userId로 닉네임 조회 (조회 실패 시 userId 그대로 사용)
        String speakerName = resolveSpeakerName(request.getUserId());

        // 길이 제한 검증
        String speakerId = truncateIfNeeded(request.getUserId(), 100);  // VARCHAR(100)
        String safeSpeakerName = truncateIfNeeded(speakerName, 50);     // VARCHAR(50) - 닉네임 기준

        // DB에 발화 세그먼트 저장
        MeetingSpeechSegment segment = MeetingSpeechSegment.create(
                meetingId,
                speakerId,
                safeSpeakerName,
                request.getTimestamp(),
                request.getDurationMs(),
                request.getText()
        );
        speechSegmentRepository.save(segment);

// 응답 객체 생성 (DB에 저장된 값과 동일하게)
        return SpeechSegmentResponse.builder()
                .meetingId(meetingId)
                .speakerId(speakerId)
                .speakerName(safeSpeakerName)
                .timestamp(request.getTimestamp())
                .durationMs(request.getDurationMs())
                .text(request.getText())
                .build();
    }

    /**
     * userId로 닉네임 조회
     * 조회 실패 시 userId 그대로 반환
     */
    private String resolveSpeakerName(String userIdStr) {
        try {
            Long userId = Long.parseLong(userIdStr);
            return userRepository.findById(userId)
                    .map(user -> user.getNickname() != null ? user.getNickname() : userIdStr)
                    .orElse(userIdStr);
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 그대로 반환
            return userIdStr;
        }
    }

    /**
     * 미팅의 전체 transcript 조회 (시간순 정렬)
     * 미팅 종료 시 Claude API 호출용
     * @param meetingId 미팅 ID
     * @return "화자: 텍스트" 형식의 발화 목록 (시간순)
     */
    @Transactional(readOnly = true)
    public List<String> getTranscriptByMeetingId(Long meetingId) {
        List<String> transcripts = speechSegmentRepository.findAllTextByMeetingIdOrderByTimestamp(meetingId);
        return transcripts;
    }

    /**
     * 미팅의 발화 세그먼트 개수 조회
     */
    @Transactional(readOnly = true)
    public long countByMeetingId(Long meetingId) {
        return speechSegmentRepository.countByMeetingId(meetingId);
    }

    /**
     * 미팅의 전체 발화 세그먼트 조회 (상세 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<MeetingSpeechSegment> getSegmentsByMeetingId(Long meetingId) {
        return speechSegmentRepository.findByMeetingIdOrderBySpeechTimestampAsc(meetingId);
    }

    /**
     * 텍스트 자르기 (로깅용)
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * DB 컬럼 길이 초과 방지
     */
    private String truncateIfNeeded(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength);
    }
}

