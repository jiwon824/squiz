-- =============================================
-- ModuStudy Database Schema
-- Flyway Migration V1 - Complete Schema (2024-02 Consolidated)
-- =============================================

SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 1. 사용자/인증
-- =============================================

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` VARCHAR(255) UNIQUE,
    `email` VARCHAR(100) UNIQUE,
    `password` VARCHAR(255),
    `name` VARCHAR(255),
    `department` VARCHAR(255),
    `position` VARCHAR(255),
    `nickname` VARCHAR(50) UNIQUE,
    `role` VARCHAR(20),
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `is_online` BOOLEAN NOT NULL DEFAULT FALSE,
    `last_seen_at` TIMESTAMP NULL,
    `is_searchable` BOOLEAN NOT NULL DEFAULT TRUE,
    `bio` TEXT,
    `interests` JSON,
    `tech_stacks` JSON,
    `available_days` JSON COMMENT '가능한 요일 (JSON 배열)',
    `preferred_time_slots` JSON COMMENT '선호 시간대 (JSON 배열)',
    `preferred_duration_weeks` INT COMMENT '선호 스터디 기간 (주)',
    `leader_rating` FLOAT DEFAULT 0.0,
    `leader_review_count` INT DEFAULT 0,
    `last_login_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `user_social_account` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `provider` ENUM('GOOGLE', 'KAKAO', 'NAVER') NOT NULL,
    `provider_user_id` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100),
    `access_token` VARCHAR(2048) COMMENT 'OAuth Access Token',
    `refresh_token` VARCHAR(512) COMMENT 'OAuth Refresh Token',
    `token_expires_at` TIMESTAMP NULL COMMENT '토큰 만료 시간',
    `calendar_id` VARCHAR(255) DEFAULT 'primary' COMMENT '연동된 Google Calendar ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_provider_user` (`provider`, `provider_user_id`)
);

CREATE TABLE IF NOT EXISTS `login_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `provider` ENUM('GOOGLE', 'KAKAO', 'NAVER', 'EMAIL') NOT NULL,
    `login_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `ip_address` VARCHAR(45),
    `device_info` VARCHAR(200),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `user_schedule` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `day_of_week` ENUM('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN') NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `is_available` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `user_organization` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `org_type` VARCHAR(50) NOT NULL,
    `org_data` JSON NOT NULL,
    `verified_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `last_checked_at` TIMESTAMP NULL,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `profile` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `profile_image_url` VARCHAR(500),
    `profile_image_source` ENUM('KAKAO', 'GOOGLE', 'NAVER', 'UPLOAD') DEFAULT 'UPLOAD',
    `bio` TEXT,
    `social_links` JSON,
    `tech` JSON,
    `favorite` JSON,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `refresh_token` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(500) NOT NULL,
    `expires_at` TIMESTAMP NOT NULL,
    `is_revoked` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_refresh_token` (`token`)
);

CREATE TABLE IF NOT EXISTS `password_reset_token` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(255) NOT NULL,
    `expires_at` TIMESTAMP NOT NULL,
    `is_used` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

-- =============================================
-- 1-1. 친구/DM
-- =============================================

CREATE TABLE IF NOT EXISTS `friendship` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `requester_id` BIGINT NOT NULL,
    `addressee_id` BIGINT NOT NULL,
    `status` ENUM('PENDING', 'ACCEPTED') DEFAULT 'PENDING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`requester_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`addressee_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_friendship` (`requester_id`, `addressee_id`),
    INDEX `idx_friendship_requester` (`requester_id`),
    INDEX `idx_friendship_addressee` (`addressee_id`),
    INDEX `idx_friendship_status` (`status`)
);

CREATE TABLE IF NOT EXISTS `user_block` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `blocker_id` BIGINT NOT NULL,
    `blocked_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`blocker_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`blocked_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_block` (`blocker_id`, `blocked_id`),
    INDEX `idx_user_block_blocker` (`blocker_id`),
    INDEX `idx_user_block_blocked` (`blocked_id`)
);

CREATE TABLE IF NOT EXISTS `dm_conversation` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user1_id` BIGINT NOT NULL,
    `user2_id` BIGINT NOT NULL,
    `user1_last_read_message_id` BIGINT,
    `user2_last_read_message_id` BIGINT,
    `user1_deleted` BOOLEAN DEFAULT FALSE,
    `user2_deleted` BOOLEAN DEFAULT FALSE,
    `last_message_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user1_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user2_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_dm_conversation_users` (`user1_id`, `user2_id`),
    INDEX `idx_dm_conversation_user1` (`user1_id`),
    INDEX `idx_dm_conversation_user2` (`user2_id`),
    INDEX `idx_dm_conversation_last_message` (`last_message_at` DESC)
);

CREATE TABLE IF NOT EXISTS `direct_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` VARCHAR(2000) NOT NULL,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`conversation_id`) REFERENCES `dm_conversation`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX `idx_direct_message_conversation_created` (`conversation_id`, `created_at` DESC)
);

-- =============================================
-- 2. 스터디 (Topic/Format 정규화 적용)
-- =============================================

CREATE TABLE IF NOT EXISTS `region` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `code` VARCHAR(100) NOT NULL UNIQUE,
    `name` VARCHAR(50) NOT NULL,
    `full_name` VARCHAR(100),
    `level` INT NOT NULL DEFAULT 1,
    `parent_id` BIGINT,
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`parent_id`) REFERENCES `region`(`id`) ON DELETE SET NULL,
    INDEX `idx_region_parent` (`parent_id`),
    INDEX `idx_region_level` (`level`)
);

-- Topic 테이블 (계층 구조: 대분류 - 소분류)
CREATE TABLE IF NOT EXISTS `topic` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `parent_id` BIGINT,
    `icon` VARCHAR(50),
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`parent_id`) REFERENCES `topic`(`id`) ON DELETE CASCADE,
    INDEX `idx_topic_parent` (`parent_id`),
    INDEX `idx_topic_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Format 테이블
CREATE TABLE IF NOT EXISTS `format` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(200),
    `icon` VARCHAR(50),
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_format_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `study` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `leader_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `topic_id` BIGINT NOT NULL,
    `format_id` BIGINT,
    `study_type` ENUM('PLANNED', 'LIGHTNING') NOT NULL,
    `meeting_type` ENUM('ONLINE', 'OFFLINE', 'HYBRID') DEFAULT 'ONLINE',
    `region_id` BIGINT,
    `location_detail` VARCHAR(200),
    `schedule_summary` VARCHAR(100),
    `schedule_days` VARCHAR(50),
    `schedule_time` TIME,
    `max_members` INT DEFAULT 10,
    `is_public` BOOLEAN DEFAULT TRUE,
    `status` ENUM('DRAFT', 'SCHEDULED', 'RECRUITING', 'RECRUIT_CLOSED', 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'DRAFT',
    `penalty_policy` ENUM('STRICT', 'NORMAL', 'LENIENT', 'RATIO', 'NONE') DEFAULT 'NORMAL',
    `start_date` DATE,
    `end_date` DATE,
    `total_sessions` INT,
    `recruit_start_date` DATE,
    `recruit_end_date` DATE,
    `extension_count` INT DEFAULT 0,
    `textbook` VARCHAR(500),
    `goal` VARCHAR(500),
    `difficulty` ENUM('BEGINNER', 'ELEMENTARY', 'INTERMEDIATE', 'ADVANCED') DEFAULT 'INTERMEDIATE',
    `prerequisites` TEXT,
    `process_detail` TEXT,
    `target_org_type` VARCHAR(50),
    `target_org_criteria` JSON,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`leader_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`region_id`) REFERENCES `region`(`id`),
    FOREIGN KEY (`topic_id`) REFERENCES `topic`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`format_id`) REFERENCES `format`(`id`) ON DELETE SET NULL,
    INDEX `idx_study_topic` (`topic_id`),
    INDEX `idx_study_format` (`format_id`)
);

CREATE TABLE IF NOT EXISTS `study_template` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT,
    `name` VARCHAR(100) NOT NULL,
    `intro` VARCHAR(200),
    `is_system` BOOLEAN DEFAULT FALSE,
    `template_type` VARCHAR(50),
    `topic_id` BIGINT,
    `format_id` BIGINT,
    `meeting_type` ENUM('ONLINE', 'OFFLINE', 'HYBRID'),
    `description` TEXT,
    `textbook` VARCHAR(500),
    `goal` VARCHAR(500),
    `difficulty` ENUM('BEGINNER', 'ELEMENTARY', 'INTERMEDIATE', 'ADVANCED'),
    `prerequisites` TEXT,
    `process_detail` TEXT,
    `penalty_policy` ENUM('STRICT', 'NORMAL', 'LENIENT', 'RATIO', 'NONE'),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`topic_id`) REFERENCES `topic`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`format_id`) REFERENCES `format`(`id`) ON DELETE SET NULL,
    INDEX `idx_study_template_topic` (`topic_id`),
    INDEX `idx_study_template_format` (`format_id`)
);

CREATE TABLE IF NOT EXISTS `study_comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT,
    `content` TEXT NOT NULL,
    `image_url` VARCHAR(500),
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`parent_id`) REFERENCES `study_comment`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `study_member` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role` ENUM('LEADER', 'MEMBER') DEFAULT 'MEMBER',
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'LEFT', 'KICKED') DEFAULT 'PENDING',
    `joined_at` TIMESTAMP NULL,
    `left_at` TIMESTAMP NULL,
    `is_probation` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_study_user` (`study_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `study_session` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `session_number` INT NOT NULL,
    `title` VARCHAR(200),
    `description` TEXT,
    `scheduled_at` TIMESTAMP NOT NULL,
    `duration_minutes` INT DEFAULT 60,
    `location` VARCHAR(200),
    `is_online` BOOLEAN DEFAULT TRUE,
    `status` ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    `completed_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_study_session` (`study_id`, `session_number`)
);

CREATE TABLE IF NOT EXISTS `study_bookmark` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `study_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_study` (`user_id`, `study_id`)
);

CREATE TABLE IF NOT EXISTS `study_application` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `message` TEXT,
    `matching_score` DECIMAL(5,2),
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    `rejected_reason` VARCHAR(500),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `processed_at` TIMESTAMP NULL,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `study_leader_review` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `reviewer_id` BIGINT NOT NULL,
    `leader_id` BIGINT NOT NULL,
    `rating` DECIMAL(2,1) NOT NULL,
    `comment` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`reviewer_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`leader_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_study_reviewer` (`study_id`, `reviewer_id`)
);

-- =============================================
-- 3. 워크스페이스/채팅 (channel -> workspace)
-- =============================================

CREATE TABLE IF NOT EXISTS `workspace` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL UNIQUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `workspace_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `message_type` ENUM('TEXT', 'IMAGE', 'FILE', 'SYSTEM') DEFAULT 'TEXT',
    `file_url` VARCHAR(500),
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `is_pinned` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`workspace_id`) REFERENCES `workspace`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX `idx_message_workspace_id` (`workspace_id`),
    INDEX `idx_message_user_id` (`user_id`),
    INDEX `idx_message_created_at` (`created_at`),
    INDEX `idx_message_pinned` (`workspace_id`, `is_pinned`)
);

-- =============================================
-- 4. 미팅
-- =============================================

CREATE TABLE IF NOT EXISTS `meeting` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `session_id` BIGINT,
    `workspace_id` BIGINT,
    `title` VARCHAR(200),
    `started_at` TIMESTAMP NULL,
    `ended_at` TIMESTAMP NULL,
    `duration_seconds` INT,
    `planned_duration_seconds` INT,
    `participant_count` INT DEFAULT 0,
    `status` ENUM('WAITING', 'IN_PROGRESS', 'ENDED') DEFAULT 'WAITING',
    `recording_status` ENUM('WAITING', 'RECORDING', 'READY', 'FAILED') DEFAULT 'WAITING',
    `stt_status` ENUM('PENDING', 'PROCESSING', 'DONE', 'FAILED') DEFAULT 'PENDING',
    `summary_status` ENUM('PENDING', 'PROCESSING', 'DONE', 'FAILED') DEFAULT 'PENDING',
    `auto_share_summary` BOOLEAN DEFAULT FALSE,
    `share_workspace_id` BIGINT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`),
    FOREIGN KEY (`workspace_id`) REFERENCES `workspace`(`id`)
);

CREATE TABLE IF NOT EXISTS `meeting_participant` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `joined_at` TIMESTAMP NOT NULL,
    `left_at` TIMESTAMP NULL,
    `is_muted` BOOLEAN DEFAULT FALSE,
    `is_camera_on` BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `meeting_participant_summary` (
    `id` VARCHAR(255) NOT NULL PRIMARY KEY,
    `meeting_id` BIGINT NULL,
    `user_id` BIGINT NOT NULL,
    `summary` TEXT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `meeting_transcript` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `timestamp_seconds` INT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `meeting_summary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL UNIQUE,
    `summary` TEXT,
    `action_items` JSON,
    `keywords` JSON,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `meeting_photo` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `image_url` VARCHAR(500) NOT NULL,
    `captured_at` TIMESTAMP NOT NULL,
    `is_selected` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `meeting_recording` (
    `meeting_recording_id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NULL,
    `recording_url` VARCHAR(500) NULL,
    `format` VARCHAR(20) NULL,
    `duration_seconds` INT NULL,
    `started_at` TIMESTAMP NULL,
    `ended_at` TIMESTAMP NULL,
    `file_size` BIGINT NULL,
    `status` ENUM('UPLOADING', 'READY', 'FAILED') DEFAULT 'UPLOADING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `meeting_action_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NULL,
    `status` ENUM('TODO', 'DONE') DEFAULT 'TODO',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `meeting_chat_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `user_id` BIGINT NULL,
    `sender_name` VARCHAR(100) NOT NULL,
    `content` TEXT NOT NULL,
    `sent_at` DATETIME NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX idx_meeting_chat_message_meeting_id_sent_at (`meeting_id`, `sent_at`)
);

CREATE TABLE IF NOT EXISTS `meeting_audio_recording` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `user_id` BIGINT NULL,
    `track_type` ENUM('MIXED', 'INDIVIDUAL') NOT NULL,
    `recording_url` VARCHAR(500) NOT NULL,
    `format` VARCHAR(20) NULL,
    `file_size` BIGINT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `meeting_stt_file` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `user_id` BIGINT NULL,
    `track_type` ENUM('MIXED', 'INDIVIDUAL') NOT NULL,
    `file_url` VARCHAR(500) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_meeting_stt_file` (`meeting_id`, `track_type`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `meeting_stt_summary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `user_id` BIGINT NULL,
    `track_type` ENUM('MIXED', 'INDIVIDUAL') NOT NULL,
    `file_url` VARCHAR(500) NOT NULL,
    `action_items` JSON NULL,
    `keywords` JSON NULL,
    `highlights_json` TEXT COMMENT '주요 내용 JSON 배열',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_meeting_stt_summary` (`meeting_id`, `track_type`, `user_id`)
);

-- =============================================
-- 5. 출석
-- =============================================

CREATE TABLE IF NOT EXISTS `attendance` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `check_type` ENUM('BLE', 'SELF', 'AUTO') DEFAULT 'SELF',
    `status` ENUM('PRESENT', 'LATE', 'ABSENT', 'EXCUSED') DEFAULT 'ABSENT',
    `checked_at` TIMESTAMP NULL,
    `checked_by` BIGINT,
    `excuse_reason` TEXT,
    `excuse_status` ENUM('PENDING', 'APPROVED', 'REJECTED'),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`checked_by`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_session_user` (`session_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `session_memo` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` VARCHAR(500) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_session_user_memo` (`session_id`, `user_id`)
);

-- =============================================
-- 6. 퀴즈
-- =============================================

CREATE TABLE IF NOT EXISTS `quiz_category` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `parent_id` BIGINT,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `depth` INT NOT NULL DEFAULT 0,
    `sort_order` INT DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`parent_id`) REFERENCES `quiz_category`(`id`) ON DELETE CASCADE,
    INDEX `idx_parent_depth` (`parent_id`, `depth`, `sort_order`)
);

CREATE TABLE IF NOT EXISTS `quiz_question_pool` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `category_id` BIGINT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('MULTIPLE_CHOICE', 'MULTIPLE_CHOICE_MULTIPLE', 'SHORT_ANSWER') DEFAULT 'MULTIPLE_CHOICE',
    `correct_answer` JSON NOT NULL,
    `explanation` TEXT,
    `difficulty` ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM',
    `tags` JSON,
    `usage_count` INT DEFAULT 0,
    `correct_rate` DECIMAL(5,2),
    `created_by` BIGINT NOT NULL,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`category_id`) REFERENCES `quiz_category`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by`) REFERENCES `user`(`id`),
    INDEX `idx_category_difficulty` (`category_id`, `difficulty`, `is_active`)
);

CREATE TABLE IF NOT EXISTS `quiz_question_pool_option` (
    `question_pool_id` BIGINT NOT NULL,
    `option_label` VARCHAR(10) NOT NULL,
    `option_text` VARCHAR(500) NOT NULL,
    `is_correct` BOOLEAN DEFAULT FALSE,
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`question_pool_id`, `option_label`),
    FOREIGN KEY (`question_pool_id`) REFERENCES `quiz_question_pool`(`id`) ON DELETE CASCADE,
    INDEX `idx_question_option_order` (`question_pool_id`, `sort_order`)
);

CREATE TABLE IF NOT EXISTS `quiz_practice_stats` (
    `user_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `total_attempted` INT DEFAULT 0,
    `total_correct` INT DEFAULT 0,
    `best_score` INT DEFAULT 0,
    `last_attempted_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `category_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`category_id`) REFERENCES `quiz_category`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `quiz_practice_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `total_questions` INT NOT NULL,
    `correct_count` INT NOT NULL,
    `score` INT NOT NULL,
    `time_spent_seconds` INT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`category_id`) REFERENCES `quiz_category`(`id`) ON DELETE CASCADE,
    INDEX `idx_user_created` (`user_id`, `created_at`)
);

CREATE TABLE IF NOT EXISTS `quiz_practice_answer` (
    `practice_record_id` BIGINT NOT NULL,
    `question_pool_id` BIGINT NOT NULL,
    `user_answer` JSON,
    `is_correct` BOOLEAN NOT NULL,
    `time_taken_seconds` INT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`practice_record_id`, `question_pool_id`),
    FOREIGN KEY (`practice_record_id`) REFERENCES `quiz_practice_record`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_pool_id`) REFERENCES `quiz_question_pool`(`id`)
);

CREATE TABLE IF NOT EXISTS `quiz_contest` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `category_id` BIGINT,
    `contest_type` ENUM('PUBLIC', 'STUDY') DEFAULT 'PUBLIC',
    `study_id` BIGINT,
    `created_by` BIGINT NOT NULL,
    `status` ENUM('DRAFT', 'SCHEDULED', 'IN_PROGRESS', 'ENDED') DEFAULT 'DRAFT',
    `scheduled_at` TIMESTAMP NULL,
    `started_at` TIMESTAMP NULL,
    `ended_at` TIMESTAMP NULL,
    `time_limit_seconds` INT DEFAULT 30,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`category_id`) REFERENCES `quiz_category`(`id`),
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`),
    FOREIGN KEY (`created_by`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `quiz_contest_state` (
    `contest_id` BIGINT NOT NULL PRIMARY KEY,
    `current_question_pool_id` BIGINT,
    `current_question_started_at` TIMESTAMP NULL,
    `is_showing_results` BOOLEAN DEFAULT FALSE,
    `phase` ENUM('WAITING', 'QUESTION', 'RESULT', 'ENDED') DEFAULT 'WAITING',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`current_question_pool_id`) REFERENCES `quiz_question_pool`(`id`)
);

CREATE TABLE IF NOT EXISTS `quiz_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `contest_id` BIGINT NOT NULL,
    `question_pool_id` BIGINT,
    `question_number` INT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('MULTIPLE_CHOICE', 'SHORT_ANSWER') DEFAULT 'MULTIPLE_CHOICE',
    `options` JSON,
    `correct_answer` VARCHAR(500) NOT NULL,
    `explanation` TEXT,
    `points` INT DEFAULT 10,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_pool_id`) REFERENCES `quiz_question_pool`(`id`)
);

CREATE TABLE IF NOT EXISTS `quiz_participant` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `contest_id` BIGINT NOT NULL,
    `user_id` BIGINT,
    `nickname` VARCHAR(50) NOT NULL,
    `total_score` INT DEFAULT 0,
    `correct_count` INT DEFAULT 0,
    `rank` INT,
    `last_answer_time` TIMESTAMP NULL,
    `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX `idx_contest_score` (`contest_id`, `total_score`, `last_answer_time`),
    INDEX `idx_participant_user` (`user_id`, `joined_at`)
);

CREATE TABLE IF NOT EXISTS `quiz_contest_chat` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `contest_id` BIGINT NOT NULL,
    `user_id` BIGINT,
    `participant_id` BIGINT NOT NULL,
    `message` VARCHAR(500) NOT NULL,
    `message_type` ENUM('TEXT', 'SYSTEM') NOT NULL DEFAULT 'TEXT',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`participant_id`) REFERENCES `quiz_participant`(`id`) ON DELETE CASCADE,
    INDEX `idx_contest_created` (`contest_id`, `created_at`)
);

CREATE TABLE IF NOT EXISTS `quiz_answer` (
    `participant_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `user_answer` JSON,
    `is_correct` BOOLEAN,
    `score` INT DEFAULT 0,
    `time_taken_seconds` INT,
    `answered_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`participant_id`, `question_id`),
    FOREIGN KEY (`participant_id`) REFERENCES `quiz_participant`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_id`) REFERENCES `quiz_question`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `study_quiz` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `session_id` BIGINT,
    `title` VARCHAR(200) NOT NULL,
    `source_type` ENUM('MEETING', 'MATERIAL', 'MANUAL') NOT NULL,
    `source_id` BIGINT,
    `status` ENUM('ACTIVE', 'DISABLED') DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`)
);

CREATE TABLE IF NOT EXISTS `study_quiz_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `quiz_id` BIGINT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('MULTIPLE_CHOICE', 'SHORT_ANSWER') DEFAULT 'MULTIPLE_CHOICE',
    `options` JSON,
    `correct_answer` VARCHAR(500) NOT NULL,
    `explanation` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`quiz_id`) REFERENCES `study_quiz`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `study_quiz_attempt` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `quiz_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `score` INT DEFAULT 0,
    `total_questions` INT,
    `correct_count` INT,
    `status` ENUM('IN_PROGRESS', 'COMPLETED', 'ABANDONED') DEFAULT 'IN_PROGRESS',
    `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `last_answered_at` TIMESTAMP NULL,
    `current_question_index` INT DEFAULT 0,
    `completed_at` TIMESTAMP NULL,
    FOREIGN KEY (`quiz_id`) REFERENCES `study_quiz`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX `idx_quiz_attempt_status` (`quiz_id`, `status`)
);

CREATE TABLE IF NOT EXISTS `study_quiz_answer` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `attempt_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `question_index` INT NOT NULL COMMENT '문제 순서 (0부터 시작)',
    `user_answer` JSON COMMENT '사용자 답변',
    `is_correct` BOOLEAN DEFAULT FALSE,
    `response_time_ms` BIGINT DEFAULT 0 COMMENT '응답 시간(ms)',
    `answered_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`attempt_id`) REFERENCES `study_quiz_attempt`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_id`) REFERENCES `study_quiz_question`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_attempt_question` (`attempt_id`, `question_id`),
    INDEX `idx_attempt_index` (`attempt_id`, `question_index`)
);

CREATE TABLE IF NOT EXISTS `quiz_course` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `badge_code` VARCHAR(50),
    `total_sections` INT DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `quiz_course_section` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `section_number` INT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `total_questions` INT DEFAULT 0,
    `pass_score` INT DEFAULT 70,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`course_id`) REFERENCES `quiz_course`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_course_section` (`course_id`, `section_number`)
);

CREATE TABLE IF NOT EXISTS `quiz_course_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `section_id` BIGINT NOT NULL,
    `question_number` INT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('MULTIPLE_CHOICE', 'SHORT_ANSWER', 'MULTIPLE_CHOICE_MULTIPLE') DEFAULT 'MULTIPLE_CHOICE',
    `options` JSON,
    `correct_answer` VARCHAR(500) NOT NULL,
    `explanation` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`section_id`) REFERENCES `quiz_course_section`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `user_course_progress` (
    `user_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `current_section` INT DEFAULT 1,
    `completed_sections` INT DEFAULT 0,
    `is_completed` BOOLEAN DEFAULT FALSE,
    `completed_at` TIMESTAMP NULL,
    `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `course_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`course_id`) REFERENCES `quiz_course`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `user_section_attempt` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `quiz_course_id` BIGINT NOT NULL,
    `section_number` INT NOT NULL,
    `score` INT DEFAULT 0,
    `correct_count` INT DEFAULT 0,
    `total_questions` INT DEFAULT 0,
    `is_passed` BOOLEAN DEFAULT FALSE,
    `status` ENUM('IN_PROGRESS', 'SUBMITTED', 'ABANDONED') DEFAULT 'IN_PROGRESS',
    `completed_at` TIMESTAMP NULL,
    `attempted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `version` BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`quiz_course_id`, `section_number`) REFERENCES `quiz_course_section`(`course_id`, `section_number`) ON DELETE CASCADE,
    INDEX `idx_attempt_user_section_status` (`user_id`, `quiz_course_id`, `section_number`, `status`)
);

CREATE TABLE IF NOT EXISTS `user_section_attempt_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `attempt_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `order_index` INT NOT NULL,
    `user_answer` VARCHAR(500),
    `is_correct` BOOLEAN,
    `response_time_ms` BIGINT DEFAULT 0 COMMENT '응답 시간(ms)',
    `answered_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (`attempt_id`) REFERENCES `user_section_attempt`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_id`) REFERENCES `quiz_course_question`(`id`),
    UNIQUE KEY `uk_attempt_question` (`attempt_id`, `question_id`),
    INDEX `idx_attempt_question_order` (`attempt_id`, `order_index`)
);

-- =============================================
-- 7. 게이미피케이션
-- =============================================

CREATE TABLE IF NOT EXISTS `daily_contribution` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `contribution_date` DATE NOT NULL,
    `has_activity` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_date` (`user_id`, `contribution_date`)
);

CREATE TABLE IF NOT EXISTS `contribution_detail` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `contribution_date` DATE NOT NULL,
    `activity_type` ENUM('STUDY_ATTENDANCE', 'QUIZ_CONTEST') NOT NULL,
    `reference_id` BIGINT NOT NULL,
    `reference_name` VARCHAR(200) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `user_stats` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `level` INT DEFAULT 1,
    `level_name` VARCHAR(50) DEFAULT '새싹',
    `total_activity_days` INT DEFAULT 0,
    `current_streak` INT DEFAULT 0,
    `max_streak` INT DEFAULT 0,
    `last_activity_date` DATE,
    `total_studies_joined` INT DEFAULT 0,
    `total_studies_led` INT DEFAULT 0,
    `total_chat_count` INT DEFAULT 0,
    `total_quiz_count` INT DEFAULT 0,
    `total_materials_uploaded` INT DEFAULT 0,
    `total_retrospectives` INT DEFAULT 0,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `level_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `level` INT NOT NULL UNIQUE,
    `level_name` VARCHAR(50) NOT NULL,
    `required_exp` INT NOT NULL,
    `level_icon_url` VARCHAR(500),
    `level_color` VARCHAR(20),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `reward_policy` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `action_type` ENUM('SIGNUP', 'DAILY_LOGIN', 'STUDY_CREATE', 'STUDY_JOIN', 'STUDY_COMPLETE', 'QUIZ_SOLVE', 'QUIZ_CREATE', 'REVIEW_WRITE', 'COMMENT_WRITE', 'ATTENDANCE') NOT NULL,
    `exp_amount` INT NOT NULL,
    `point_amount` INT NOT NULL,
    `description` VARCHAR(200),
    `is_active` BOOLEAN DEFAULT TRUE,
    `daily_limit` INT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `exp_transaction` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `exp_amount` INT NOT NULL,
    `exp_type` ENUM('SIGNUP', 'DAILY_LOGIN', 'STUDY_CREATE', 'STUDY_JOIN', 'STUDY_COMPLETE', 'QUIZ_SOLVE', 'QUIZ_CREATE', 'REVIEW_WRITE', 'COMMENT_WRITE', 'ATTENDANCE', 'LEVEL_UP', 'EVENT', 'ADMIN_GRANT', 'PENALTY') NOT NULL,
    `reference_type` ENUM('STUDY', 'QUIZ', 'REVIEW', 'COMMENT', 'ATTENDANCE', 'NONE') DEFAULT 'NONE',
    `reference_id` BIGINT,
    `description` VARCHAR(200),
    `balance_after` INT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `point_transaction` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `point_amount` INT NOT NULL,
    `transaction_type` ENUM('EARN', 'SPEND') NOT NULL,
    `point_type` ENUM('SIGNUP', 'DAILY_LOGIN', 'STUDY_CREATE', 'STUDY_JOIN', 'STUDY_COMPLETE', 'QUIZ_SOLVE', 'QUIZ_CREATE', 'REVIEW_WRITE', 'COMMENT_WRITE', 'ATTENDANCE', 'LEVEL_UP', 'EVENT', 'ADMIN_GRANT', 'HINT_USE', 'ITEM_PURCHASE') NOT NULL,
    `reference_type` ENUM('STUDY', 'QUIZ', 'REVIEW', 'COMMENT', 'ATTENDANCE', 'HINT', 'ITEM', 'NONE') DEFAULT 'NONE',
    `reference_id` BIGINT,
    `description` VARCHAR(200),
    `balance_after` INT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `hint_cost` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `hint_type` VARCHAR(50) NOT NULL,
    `cost_points` INT NOT NULL,
    `description` VARCHAR(200),
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `badge` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(200) NOT NULL,
    `icon` VARCHAR(10),
    `category` ENUM('ACTIVITY', 'STREAK', 'STUDY', 'ATTENDANCE', 'PARTICIPATION', 'QUIZ', 'MASTER', 'SPECIAL') NOT NULL,
    `condition_type` VARCHAR(50) NOT NULL,
    `condition_value` INT NOT NULL,
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `user_badge` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `badge_id` BIGINT NOT NULL,
    `badge_type` ENUM('ACTIVITY', 'STREAK', 'STUDY', 'QUIZ_KING', 'SPECIAL'),
    `reference_type` ENUM('CONTEST', 'STUDY', 'GENERAL'),
    `reference_id` BIGINT,
    `rank` INT,
    `earned_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`badge_id`) REFERENCES `badge`(`id`),
    UNIQUE KEY `uk_user_badge` (`user_id`, `badge_id`),
    INDEX `idx_user_badge_type` (`user_id`, `badge_type`, `reference_type`)
);

CREATE TABLE IF NOT EXISTS `penalty` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(200) NOT NULL,
    `icon` VARCHAR(10),
    `grant_condition` VARCHAR(200),
    `removal_condition` VARCHAR(200),
    `removal_required` INT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `user_penalty` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `penalty_id` BIGINT NOT NULL,
    `study_id` BIGINT,
    `is_active` BOOLEAN DEFAULT TRUE,
    `removal_progress` INT DEFAULT 0,
    `granted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `removed_at` TIMESTAMP NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`penalty_id`) REFERENCES `penalty`(`id`),
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE SET NULL
);

-- =============================================
-- 8. 자료실
-- =============================================

CREATE TABLE IF NOT EXISTS `material` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `uploader_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `material_type` ENUM('LINK', 'FILE', 'IMAGE', 'VIDEO') NOT NULL,
    `url` VARCHAR(500),
    `file_path` VARCHAR(500),
    `file_size` BIGINT,
    `week_number` INT,
    `view_count` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`uploader_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `material_comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `material_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`material_id`) REFERENCES `material`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `curriculum` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `week_number` INT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_study_week` (`study_id`, `week_number`)
);

CREATE TABLE IF NOT EXISTS `progress` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `curriculum_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `is_completed` BOOLEAN DEFAULT FALSE,
    `completed_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`curriculum_id`) REFERENCES `curriculum`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_curriculum_user` (`curriculum_id`, `user_id`)
);

-- =============================================
-- 9. 회고
-- =============================================

CREATE TABLE IF NOT EXISTS `retrospective` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `session_id` BIGINT,
    `title` VARCHAR(200) NOT NULL,
    `retrospective_type` ENUM('KPT', 'FREE') DEFAULT 'KPT',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`)
);

CREATE TABLE IF NOT EXISTS `retrospective_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `retrospective_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `category` ENUM('KEEP', 'PROBLEM', 'TRY') NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`retrospective_id`) REFERENCES `retrospective`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- =============================================
-- 10. 알림
-- =============================================

CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` ENUM('CHAT', 'SCHEDULE', 'ATTENDANCE', 'STUDY_UPDATE', 'STUDY_APPLICATION', 'QUIZ', 'SYSTEM') NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `reference_type` VARCHAR(50),
    `reference_id` BIGINT,
    `is_read` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `notification_setting` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `notification_type` VARCHAR(50) NOT NULL,
    `is_enabled` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_type` (`user_id`, `notification_type`)
);

CREATE TABLE IF NOT EXISTS `fcm_token` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(500) NOT NULL,
    `device_type` ENUM('ANDROID', 'IOS', 'WEB') NOT NULL,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

-- =============================================
-- 11. SSAFY 보고서
-- =============================================

CREATE TABLE IF NOT EXISTS `report` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `report_type` ENUM('APPLICATION', 'MONTHLY_RESULT') NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` JSON,
    `report_month` DATE,
    `status` ENUM('DRAFT', 'SUBMITTED') DEFAULT 'DRAFT',
    `created_by` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by`) REFERENCES `user`(`id`)
);

-- =============================================
-- 12. AI 피드백
-- =============================================

CREATE TABLE IF NOT EXISTS `ai_feedback` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `feature_type` VARCHAR(50) NOT NULL,
    `reference_id` BIGINT,
    `feedback` ENUM('POSITIVE', 'NEGATIVE') NOT NULL,
    `comment` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- =============================================
-- 13. 데일리
-- =============================================

CREATE TABLE IF NOT EXISTS `daily_report` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `report_date` DATE NOT NULL,
    `summary` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_study_date` (`study_id`, `report_date`)
);

CREATE TABLE IF NOT EXISTS `daily_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `daily_report_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `category` ENUM('YESTERDAY', 'TODAY', 'BLOCKER') NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`daily_report_id`) REFERENCES `daily_report`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- =============================================
-- 14. 자유게시판
-- =============================================

CREATE TABLE IF NOT EXISTS `board_post` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category` ENUM('PROJECT', 'COMPETITION', 'JOB', 'FREE') NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `view_count` INT DEFAULT 0,
    `like_count` INT DEFAULT 0,
    `comment_count` INT DEFAULT 0,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `board_comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT,
    `content` TEXT NOT NULL,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`post_id`) REFERENCES `board_post`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`parent_id`) REFERENCES `board_comment`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `board_like` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`post_id`) REFERENCES `board_post`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
);

-- =============================================
-- 15. 팀원모집
-- =============================================

CREATE TABLE IF NOT EXISTS `team_recruit` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category` ENUM('HACKATHON', 'PROJECT', 'COMPETITION') NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `required_roles` JSON,
    `tech_stack` JSON,
    `max_members` INT DEFAULT 5,
    `current_members` INT DEFAULT 1,
    `deadline` DATE,
    `start_date` DATE,
    `duration` VARCHAR(100),
    `meeting_type` ENUM('ONLINE', 'OFFLINE', 'HYBRID') DEFAULT 'ONLINE',
    `region_id` BIGINT,
    `status` ENUM('RECRUITING', 'CLOSED', 'COMPLETED') DEFAULT 'RECRUITING',
    `view_count` INT DEFAULT 0,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`region_id`) REFERENCES `region`(`id`)
);

CREATE TABLE IF NOT EXISTS `team_application` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `recruit_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `applied_role` VARCHAR(50),
    `message` TEXT,
    `portfolio_url` VARCHAR(500),
    `status` ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    `rejected_reason` VARCHAR(500),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `processed_at` TIMESTAMP NULL,
    FOREIGN KEY (`recruit_id`) REFERENCES `team_recruit`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- =============================================
-- 16. 꼬멘틀 (IT 용어 추측 게임)
-- =============================================

CREATE TABLE IF NOT EXISTS `comendle_word` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `word` VARCHAR(100) NOT NULL UNIQUE,
    `hint` VARCHAR(500),
    `category` VARCHAR(50),
    `difficulty` ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM',
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `comendle_daily` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `word_id` BIGINT NOT NULL,
    `game_date` DATE NOT NULL UNIQUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`word_id`) REFERENCES `comendle_word`(`id`)
);

CREATE TABLE IF NOT EXISTS `comendle_attempt` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `daily_id` BIGINT NOT NULL,
    `user_id` BIGINT,
    `session_id` VARCHAR(100),
    `is_solved` BOOLEAN DEFAULT FALSE,
    `guess_count` INT DEFAULT 0,
    `solved_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`daily_id`) REFERENCES `comendle_daily`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `comendle_guess` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `attempt_id` BIGINT NOT NULL,
    `guess_number` INT NOT NULL,
    `guessed_word` VARCHAR(100) NOT NULL,
    `similarity` DECIMAL(5,2),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`attempt_id`) REFERENCES `comendle_attempt`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `comendle_streak` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `current_streak` INT DEFAULT 0,
    `max_streak` INT DEFAULT 0,
    `total_solved` INT DEFAULT 0,
    `total_played` INT DEFAULT 0,
    `last_played_date` DATE,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

-- =============================================
-- 17. IT 뉴스
-- =============================================

CREATE TABLE IF NOT EXISTS `it_news` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(300) NOT NULL,
    `summary` TEXT,
    `source_url` VARCHAR(500) NOT NULL,
    `source_name` VARCHAR(100),
    `thumbnail_url` VARCHAR(500),
    `category` VARCHAR(50),
    `published_at` TIMESTAMP NULL,
    `view_count` INT DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `news_bookmark` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `news_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`news_id`) REFERENCES `it_news`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_news` (`user_id`, `news_id`)
);

-- =============================================
-- 18. Google Calendar 연동
-- =============================================

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
    UNIQUE KEY `uk_calendar_watch_channel` (`channel_id`),
    INDEX `idx_calendar_watch_user` (`user_id`),
    INDEX `idx_calendar_watch_expires` (`expires_at`)
);

CREATE TABLE IF NOT EXISTS `study_session_calendar_mapping` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `google_event_id` VARCHAR(255) NOT NULL COMMENT 'Google Calendar Event ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_session_user_mapping` (`session_id`, `user_id`),
    INDEX `idx_session_mapping_session` (`session_id`),
    INDEX `idx_session_mapping_user` (`user_id`)
);

-- =============================================
-- 19. 템플릿 사용 로그
-- =============================================

CREATE TABLE IF NOT EXISTS `template_usage_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `template_id` BIGINT NOT NULL,
    `study_id` BIGINT,
    `used_as_is` BOOLEAN DEFAULT FALSE,
    `modifications` JSON,
    `user_tech_stack` JSON,
    `user_schedule` JSON,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`template_id`) REFERENCES `study_template`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE SET NULL,
    INDEX `idx_template_usage_user` (`user_id`),
    INDEX `idx_template_usage_template` (`template_id`)
);

-- =============================================
-- 20. 스터디 추천 로그 (파인튜닝 데이터 수집)
-- =============================================

CREATE TABLE IF NOT EXISTS `study_recommend_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `recommend_type` ENUM('GENERAL', 'TOPIC') NOT NULL DEFAULT 'GENERAL',
    `topic_id` BIGINT,
    `result_count` INT NOT NULL DEFAULT 0,
    `user_tech_snapshot` JSON,
    `user_schedule_snapshot` JSON,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`topic_id`) REFERENCES `topic`(`id`) ON DELETE SET NULL,
    INDEX `idx_recommend_log_user` (`user_id`),
    INDEX `idx_recommend_log_created` (`created_at`)
);

CREATE TABLE IF NOT EXISTS `study_recommend_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `log_id` BIGINT NOT NULL,
    `study_id` BIGINT NOT NULL,
    `rank_position` INT NOT NULL,
    `matching_score` DECIMAL(7,2),
    `tech_match_count` INT DEFAULT 0,
    `schedule_match_count` INT DEFAULT 0,
    `topic_match_count` INT DEFAULT 0,
    `match_reason` VARCHAR(500),
    FOREIGN KEY (`log_id`) REFERENCES `study_recommend_log`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    INDEX `idx_recommend_item_log` (`log_id`)
);

CREATE TABLE IF NOT EXISTS `study_recommend_action` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `log_id` BIGINT NOT NULL,
    `item_id` BIGINT,
    `study_id` BIGINT NOT NULL,
    `action_type` ENUM('CLICK', 'APPLY', 'BOOKMARK', 'DISMISS') NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`log_id`) REFERENCES `study_recommend_log`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`item_id`) REFERENCES `study_recommend_item`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    INDEX `idx_recommend_action_log` (`log_id`),
    INDEX `idx_recommend_action_type` (`action_type`)
);

-- =============================================
-- 21. FSRS 복습 시스템
-- =============================================

CREATE TABLE IF NOT EXISTS `user_review_items` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `content_type` VARCHAR(20) NOT NULL,
    `content_id` BIGINT NOT NULL,
    `stability` DOUBLE NOT NULL DEFAULT 0.0,
    `difficulty` DOUBLE NOT NULL DEFAULT 5.0,
    `elapsed_days` INT DEFAULT 0,
    `scheduled_days` INT DEFAULT 0,
    `reps` INT DEFAULT 0,
    `lapses` INT DEFAULT 0,
    `state` INT DEFAULT 0,
    `last_elapsed_days` INT DEFAULT 0,
    `last_response_time_ms` BIGINT DEFAULT 0,
    `retrievability` DOUBLE DEFAULT 0.0,
    `last_reviewed_at` TIMESTAMP NULL,
    `next_review_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_content` (`user_id`, `content_type`, `content_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX `idx_user_next_review` (`user_id`, `next_review_at`)
);

CREATE TABLE IF NOT EXISTS `user_review_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `review_item_id` BIGINT NOT NULL,
    `is_correct` BOOLEAN NOT NULL,
    `response_time_ms` BIGINT NOT NULL,
    `stability` DOUBLE NOT NULL,
    `difficulty` DOUBLE NOT NULL,
    `reviewed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`review_item_id`) REFERENCES `user_review_items`(`id`) ON DELETE CASCADE,
    INDEX `idx_review_log_item` (`review_item_id`, `reviewed_at`)
);

SET FOREIGN_KEY_CHECKS = 1;
