-- V6: Replace channel with workspace
-- - create workspace table
-- - message.channel_id -> workspace_id
-- - meeting.channel_id -> workspace_id
-- - drop channel table

-- 1. create workspace table
CREATE TABLE IF NOT EXISTS `workspace` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL UNIQUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE
);

-- 2. backfill workspace rows for existing studies
INSERT INTO `workspace` (`study_id`, `created_at`)
SELECT `id`, NOW() FROM `study`
ON DUPLICATE KEY UPDATE `study_id` = `study_id`;

-- 3. drop message.channel_id FK
ALTER TABLE `message` DROP FOREIGN KEY `message_ibfk_1`;

-- 4. add message.workspace_id
ALTER TABLE `message` ADD COLUMN `workspace_id` BIGINT NULL AFTER `id`;

-- 5. map existing message.channel_id -> workspace_id
UPDATE `message` m
JOIN `channel` c ON m.channel_id = c.id
JOIN `workspace` w ON c.study_id = w.study_id
SET m.workspace_id = w.id;

-- 6. drop message.channel_id
ALTER TABLE `message` DROP COLUMN `channel_id`;

-- 7. enforce NOT NULL on message.workspace_id
ALTER TABLE `message` MODIFY COLUMN `workspace_id` BIGINT NOT NULL;

-- 8. add message.workspace_id FK
ALTER TABLE `message` ADD CONSTRAINT `fk_message_workspace`
    FOREIGN KEY (`workspace_id`) REFERENCES `workspace`(`id`) ON DELETE CASCADE;

-- 9. add indexes
CREATE INDEX `idx_message_workspace_id` ON `message` (`workspace_id`);
CREATE INDEX `idx_message_user_id` ON `message` (`user_id`);
CREATE INDEX `idx_message_created_at` ON `message` (`created_at`);

-- 9-1. meeting table channel_id -> workspace_id
SET @fk_meeting_channel := (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'meeting'
      AND REFERENCED_TABLE_NAME = 'channel'
    LIMIT 1
);
SET @sql := IF(
    @fk_meeting_channel IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `meeting` DROP FOREIGN KEY `', @fk_meeting_channel, '`')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `meeting`
    ADD COLUMN `workspace_id` BIGINT NULL AFTER `session_id`,
    ADD COLUMN `share_workspace_id` BIGINT NULL AFTER `auto_share_summary`;

UPDATE `meeting` m
JOIN `workspace` w ON m.study_id = w.study_id
SET m.workspace_id = w.id;

UPDATE `meeting` m
JOIN `workspace` w ON m.study_id = w.study_id
SET m.share_workspace_id = w.id
WHERE m.share_channel_id IS NOT NULL;

ALTER TABLE `meeting`
    DROP COLUMN `channel_id`,
    DROP COLUMN `share_channel_id`;

ALTER TABLE `meeting`
    ADD CONSTRAINT `fk_meeting_workspace`
    FOREIGN KEY (`workspace_id`) REFERENCES `workspace`(`id`);

-- 10. drop channel table
DROP TABLE IF EXISTS `channel`;
