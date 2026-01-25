-- MyBatis 전용 테이블 (JPA 엔티티가 아닌 것들)
-- 테스트 시 JPA ddl-auto=update 이후에 실행됨

-- 프로필 (JPA Entity가 비어있어서 직접 생성)
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

-- 친구 관계
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

-- 사용자 차단
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

-- DM 대화방
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

-- DM 메시지
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
