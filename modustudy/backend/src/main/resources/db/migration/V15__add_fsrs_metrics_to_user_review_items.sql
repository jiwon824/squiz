-- ============================================================================
-- V15: FSRS 자동 난이도 조절을 위한 데이터 컬럼 추가 (Binary 모델 전용)
-- ============================================================================

-- 1. 현재 상태 테이블 고도화
ALTER TABLE `user_review_items`
    ADD COLUMN `last_elapsed_days` INT DEFAULT 0 COMMENT '마지막 복습 후 실제 경과일',
    ADD COLUMN `last_response_time_ms` BIGINT DEFAULT 0 COMMENT '마지막 응답 시간(ms)',
    ADD COLUMN `retrievability` DOUBLE DEFAULT 0.0 COMMENT '복습 직전 기억 회수 확률(R)';

-- 2. 복습 이력 로그 (내부 계산용이 아닌, 순수 기록용)
CREATE TABLE IF NOT EXISTS `user_review_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `review_item_id` BIGINT NOT NULL,
    `is_correct` BOOLEAN NOT NULL COMMENT '정답 여부 (Binary 피드백의 핵심)',
    `response_time_ms` BIGINT NOT NULL COMMENT '응답 시간(ms)',
    `stability` DOUBLE NOT NULL COMMENT '리뷰 전 안정성(S)',
    `difficulty` DOUBLE NOT NULL COMMENT '리뷰 전 난이도(D)',
    `reviewed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`review_item_id`) REFERENCES `user_review_items`(`id`) ON DELETE CASCADE
) COMMENT '사용자 개입 없는 순수 학습 이력 로그';

CREATE INDEX `idx_review_log_item` ON `user_review_log` (`review_item_id`, `reviewed_at`);

-- ============================================================================
-- 로그 테이블은 **"이 사용자가 3일 전에는 이 문제를 2초 만에 틀렸는데, 오늘은 1초 만에 맞혔다"**는 성장 곡선을 그리기 위해 필요할 뿐, 사용자에게 무언가를 묻기 위함이 아닙니다.
-- ============================================================================
