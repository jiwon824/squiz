-- 1. 코스 퀴즈 답변 테이블: 컬럼 추가(응답시간 단위 통일)
ALTER TABLE `user_section_attempt_question` 
ADD COLUMN `response_time_ms` BIGINT DEFAULT 0 COMMENT '응답 시간(ms)';

-- 2. 스터디 퀴즈 답변 테이블: 컬럼명 및 타입 변경(응답시간 단위 통일)
ALTER TABLE `study_quiz_answer` 
CHANGE COLUMN `time_taken_seconds` `response_time_ms` BIGINT DEFAULT 0 COMMENT '응답 시간(ms)';

-- 3. FSRS 아이템 테이블: 이 데이터는 계산용이므로 절대 비어있으면 안 된다
ALTER TABLE `user_review_items` 
MODIFY COLUMN `stability` DOUBLE NOT NULL DEFAULT 0.0,
MODIFY COLUMN `difficulty` DOUBLE NOT NULL DEFAULT 5.0;
