-- 메시지 고정 기능을 위한 is_pinned 컬럼 추가
ALTER TABLE `message` ADD COLUMN `is_pinned` BOOLEAN NOT NULL DEFAULT FALSE AFTER `is_deleted`;

-- 고정된 메시지 조회 성능 향상을 위한 인덱스 추가
CREATE INDEX `idx_message_pinned` ON `message` (`workspace_id`, `is_pinned`);
