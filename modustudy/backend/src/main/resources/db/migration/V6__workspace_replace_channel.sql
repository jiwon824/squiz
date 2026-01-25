-- =============================================
-- V6: 채널(Channel) → 워크스페이스(Workspace) 전환
-- - workspace 테이블 생성
-- - message 테이블: channel_id → workspace_id 변경
-- - channel 테이블 삭제
-- =============================================

-- 1. workspace 테이블 생성
CREATE TABLE IF NOT EXISTS `workspace` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL UNIQUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE
);

-- 2. 기존 study마다 workspace 생성 (기존 데이터 마이그레이션)
INSERT INTO `workspace` (`study_id`, `created_at`)
SELECT `id`, NOW() FROM `study`
ON DUPLICATE KEY UPDATE `study_id` = `study_id`;

-- 3. message 테이블의 channel_id 외래키 제거
ALTER TABLE `message` DROP FOREIGN KEY `message_ibfk_1`;

-- 4. message 테이블에 workspace_id 컬럼 추가
ALTER TABLE `message` ADD COLUMN `workspace_id` BIGINT NULL AFTER `id`;

-- 5. 기존 channel_id 데이터를 workspace_id로 매핑
-- (channel.study_id → workspace.study_id로 매핑)
UPDATE `message` m
JOIN `channel` c ON m.channel_id = c.id
JOIN `workspace` w ON c.study_id = w.study_id
SET m.workspace_id = w.id;

-- 6. channel_id 컬럼 삭제
ALTER TABLE `message` DROP COLUMN `channel_id`;

-- 7. workspace_id NOT NULL로 변경 (기존 데이터 없으면 스킵)
ALTER TABLE `message` MODIFY COLUMN `workspace_id` BIGINT NOT NULL;

-- 8. workspace_id 외래키 추가
ALTER TABLE `message` ADD CONSTRAINT `fk_message_workspace`
    FOREIGN KEY (`workspace_id`) REFERENCES `workspace`(`id`) ON DELETE CASCADE;

-- 9. 인덱스 추가
CREATE INDEX `idx_message_workspace_id` ON `message` (`workspace_id`);
CREATE INDEX `idx_message_user_id` ON `message` (`user_id`);
CREATE INDEX `idx_message_created_at` ON `message` (`created_at`);

-- 10. channel 테이블 삭제
DROP TABLE IF EXISTS `channel`;
