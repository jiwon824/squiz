SET FOREIGN_KEY_CHECKS=0;
-- MyBatis ?꾩슜 ?뚯씠釉?(JPA ?뷀떚?곌? ?꾨땶 寃껊뱾)
-- ?뚯뒪????JPA ddl-auto=update ?댄썑???ㅽ뻾??

-- ?꾨줈??(JPA Entity媛 鍮꾩뼱?덉뼱??吏곸젒 ?앹꽦)
DROP TABLE IF EXISTS `profile`;
CREATE TABLE `profile` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `profile_image_url` VARCHAR(500),
    `profile_image_source` VARCHAR(20) DEFAULT 'UPLOAD',
    `bio` TEXT,
    `social_links` JSON,
    `tech` JSON,
    `favorite` JSON,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 移쒓뎄 愿怨?
DROP TABLE IF EXISTS `friendship`;
CREATE TABLE `friendship` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `requester_id` BIGINT NOT NULL,
    `addressee_id` BIGINT NOT NULL,
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_friendship` (`requester_id`, `addressee_id`),
    INDEX `idx_friendship_requester` (`requester_id`),
    INDEX `idx_friendship_addressee` (`addressee_id`)
);

-- ?ъ슜??李⑤떒
DROP TABLE IF EXISTS `user_block`;
CREATE TABLE `user_block` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `blocker_id` BIGINT NOT NULL,
    `blocked_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_block` (`blocker_id`, `blocked_id`),
    INDEX `idx_user_block_blocker` (`blocker_id`),
    INDEX `idx_user_block_blocked` (`blocked_id`)
);

-- DM ??붾갑
DROP TABLE IF EXISTS `direct_message`;
DROP TABLE IF EXISTS `dm_conversation`;
CREATE TABLE `dm_conversation` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user1_id` BIGINT NOT NULL,
    `user2_id` BIGINT NOT NULL,
    `user1_deleted` BOOLEAN DEFAULT FALSE,
    `user2_deleted` BOOLEAN DEFAULT FALSE,
    `user1_last_read_message_id` BIGINT,
    `user2_last_read_message_id` BIGINT,
    `last_message_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_dm_conversation` (`user1_id`, `user2_id`),
    INDEX `idx_dm_conversation_user1` (`user1_id`),
    INDEX `idx_dm_conversation_user2` (`user2_id`)
);

-- DM 硫붿떆吏
CREATE TABLE `direct_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` VARCHAR(2000) NOT NULL,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_direct_message_conversation` (`conversation_id`)
);

-- =============================================
-- ?뚰겕?ㅽ럹?댁뒪/梨꾪똿 (JPA Entity)
-- =============================================

DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `workspace`;

CREATE TABLE `workspace` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL UNIQUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_workspace_study_id` (`study_id`)
);

CREATE TABLE `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `workspace_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `message_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    `file_url` VARCHAR(500),
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `is_pinned` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_message_workspace_id` (`workspace_id`),
    INDEX `idx_message_user_id` (`user_id`),
    INDEX `idx_message_created_at` (`created_at`),
    INDEX `idx_message_pinned` (`workspace_id`, `is_pinned`)
);
SET FOREIGN_KEY_CHECKS=1;

