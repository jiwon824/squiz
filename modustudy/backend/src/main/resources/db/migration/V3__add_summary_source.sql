-- meeting_stt_summary에 summary_source 컬럼 추가
-- 요약 생성 시 사용된 소스를 구분: REALTIME_STT (실시간 STT) vs FULL_AUDIO (전체 녹음파일 STT)

ALTER TABLE meeting_stt_summary
ADD COLUMN summary_source VARCHAR(20) DEFAULT 'UNKNOWN'
COMMENT '요약 소스: REALTIME_STT (실시간 STT 기반), FULL_AUDIO (전체 녹음파일 STT), UNKNOWN';
