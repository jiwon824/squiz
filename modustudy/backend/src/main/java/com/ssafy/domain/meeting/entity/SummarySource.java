package com.ssafy.domain.meeting.entity;

/**
 * 미팅 요약 생성 시 사용된 소스
 */
public enum SummarySource {
    /** 실시간 STT 데이터 기반 (미팅 중 수집된 speech_segment) */
    REALTIME_STT,

    /** 전체 녹음파일 STT 기반 (미팅 종료 후 voice.webm에서 추출) */
    FULL_AUDIO,

    /** 알 수 없음 (기존 데이터 호환용) */
    UNKNOWN
}
