-- meeting_stt_summary 테이블의 AI 생성 데이터 컬럼 크기 확장
-- TINYTEXT(255 bytes) → TEXT(65KB)로 변경하여 AI가 생성하는 긴 데이터 저장 가능

ALTER TABLE meeting_stt_summary
    MODIFY COLUMN action_items TEXT,
    MODIFY COLUMN keywords TEXT,
    MODIFY COLUMN highlights_json TEXT;
