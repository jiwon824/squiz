-- MyBatis 전용 테이블 (JPA 엔티티가 아닌 것들)
-- 테스트 시 JPA ddl-auto=update 이후에 실행됨

-- 친구 관계
CREATE TABLE IF NOT EXISTS `friendship` (
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
CREATE TABLE IF NOT EXISTS `user_block` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `blocker_id` BIGINT NOT NULL,
    `blocked_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_block` (`blocker_id`, `blocked_id`),
    INDEX `idx_user_block_blocker` (`blocker_id`),
    INDEX `idx_user_block_blocked` (`blocked_id`)
);

-- DM 대화방
CREATE TABLE IF NOT EXISTS `dm_conversation` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user1_id` BIGINT NOT NULL,
    `user2_id` BIGINT NOT NULL,
    `user1_deleted` BOOLEAN DEFAULT FALSE,
    `user2_deleted` BOOLEAN DEFAULT FALSE,
    `user1_last_read_message_id` BIGINT,
    `user2_last_read_message_id` BIGINT,
    `last_message_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_dm_conversation` (`user1_id`, `user2_id`),
    INDEX `idx_dm_conversation_user1` (`user1_id`),
    INDEX `idx_dm_conversation_user2` (`user2_id`)
);

-- DM 메시지
CREATE TABLE IF NOT EXISTS `direct_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` BIGINT NOT NULL,
    `sender_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_direct_message_conversation` (`conversation_id`)
);
