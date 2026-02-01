-- =============================================
-- V3: meeting_stt_summary에 summary_source 컬럼 추가
-- REALTIME_STT: 실시간 STT 세그먼트 기반 요약
-- FULL_AUDIO: 전체 녹음파일 STT 기반 요약
-- =============================================

ALTER TABLE meeting_stt_summary
ADD COLUMN summary_source ENUM('REALTIME_STT', 'FULL_AUDIO') NOT NULL DEFAULT 'FULL_AUDIO'
AFTER highlights_json;
