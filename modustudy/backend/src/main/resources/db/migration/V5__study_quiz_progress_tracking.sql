-- =============================================
-- V5: 스터디 퀴즈 진행 상태 추적 기능 추가
-- - 이어풀기 / 처음부터 다시풀기 지원
-- =============================================

-- 1. study_quiz_attempt 테이블에 진행 상태 컬럼 추가
ALTER TABLE `study_quiz_attempt`
ADD COLUMN `status` ENUM('IN_PROGRESS', 'COMPLETED', 'ABANDONED') DEFAULT 'IN_PROGRESS' AFTER `user_id`,
ADD COLUMN `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER `status`,
ADD COLUMN `last_answered_at` TIMESTAMP NULL AFTER `started_at`,
ADD COLUMN `current_question_index` INT DEFAULT 0 AFTER `last_answered_at`;

-- 기존 데이터는 완료된 것으로 처리
UPDATE `study_quiz_attempt` SET `status` = 'COMPLETED' WHERE `completed_at` IS NOT NULL;

-- 2. 스터디 퀴즈 개별 답변 기록 테이블 생성
CREATE TABLE IF NOT EXISTS `study_quiz_answer` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `attempt_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `question_index` INT NOT NULL COMMENT '문제 순서 (0부터 시작)',
    `user_answer` JSON COMMENT '사용자 답변 (객관식: ["A"], 단답형: "답")',
    `is_correct` BOOLEAN DEFAULT FALSE,
    `time_taken_seconds` INT DEFAULT 0 COMMENT '문제 풀이 소요 시간',
    `answered_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`attempt_id`) REFERENCES `study_quiz_attempt`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_id`) REFERENCES `study_quiz_question`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_attempt_question` (`attempt_id`, `question_id`),
    INDEX `idx_attempt_index` (`attempt_id`, `question_index`)
) COMMENT '스터디 퀴즈 개별 문제 답변 기록';

-- 3. 인덱스 추가 (이어풀기 조회 최적화)
CREATE INDEX `idx_quiz_attempt_user_status` ON `study_quiz_attempt` (`quiz_id`, `user_id`, `status`);
