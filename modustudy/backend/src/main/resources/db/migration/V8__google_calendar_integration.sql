-- =============================================
-- V8: Google Calendar Integration
-- =============================================
-- 1. UserSocialAccount에 토큰 필드 추가
-- 2. CalendarWatch 테이블 생성 (Webhook 구독 관리)
-- 3. StudySessionCalendarMapping 테이블 생성 (세션-이벤트 매핑)

-- 1. UserSocialAccount 토큰 필드 추가
ALTER TABLE `user_social_account`
ADD COLUMN `access_token` VARCHAR(2048) NULL COMMENT 'OAuth Access Token',
ADD COLUMN `refresh_token` VARCHAR(512) NULL COMMENT 'OAuth Refresh Token',
ADD COLUMN `token_expires_at` TIMESTAMP NULL COMMENT '토큰 만료 시간',
ADD COLUMN `calendar_id` VARCHAR(255) DEFAULT 'primary' COMMENT '연동된 Google Calendar ID';

-- 2. CalendarWatch 테이블 (Google Calendar Webhook 구독 관리)
CREATE TABLE IF NOT EXISTS `calendar_watch` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `channel_id` VARCHAR(255) NOT NULL COMMENT 'Google Channel ID',
    `resource_id` VARCHAR(255) NOT NULL COMMENT 'Google Resource ID',
    `expires_at` TIMESTAMP NOT NULL COMMENT 'Watch 만료 시간',
    `sync_token` VARCHAR(255) NULL COMMENT 'Delta Sync용 토큰',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_calendar_watch_channel` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. StudySessionCalendarMapping 테이블 (세션-캘린더 이벤트 매핑)
CREATE TABLE IF NOT EXISTS `study_session_calendar_mapping` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `google_event_id` VARCHAR(255) NOT NULL COMMENT 'Google Calendar Event ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_session_user_mapping` (`session_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 추가
CREATE INDEX `idx_calendar_watch_user` ON `calendar_watch`(`user_id`);
CREATE INDEX `idx_calendar_watch_expires` ON `calendar_watch`(`expires_at`);
CREATE INDEX `idx_session_mapping_session` ON `study_session_calendar_mapping`(`session_id`);
CREATE INDEX `idx_session_mapping_user` ON `study_session_calendar_mapping`(`user_id`);
