-- meeting_stt_summary 테이블에 highlights_json 컬럼 추가
ALTER TABLE meeting_stt_summary ADD COLUMN highlights_json TEXT NULL COMMENT '주요 내용 JSON 배열';
