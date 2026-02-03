-- Board domain ERD (board_post, board_comment, board_like)
-- BaseEntity: id, created_at, updated_at

CREATE TABLE `board_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `user_id` BIGINT NOT NULL,
  `study_id` BIGINT NOT NULL,
  `category` VARCHAR(20) NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `view_count` INT NOT NULL DEFAULT 0,
  `like_count` INT NOT NULL DEFAULT 0,
  `comment_count` INT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_board_post_user_id` (`user_id`),
  KEY `idx_board_post_study_id` (`study_id`),
  CONSTRAINT `fk_board_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_board_post_study` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`)
);

CREATE TABLE `board_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `post_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `parent_id` BIGINT NULL,
  `content` TEXT NOT NULL,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_board_comment_post_id` (`post_id`),
  KEY `idx_board_comment_user_id` (`user_id`),
  KEY `idx_board_comment_parent_id` (`parent_id`),
  CONSTRAINT `fk_board_comment_post` FOREIGN KEY (`post_id`) REFERENCES `board_post` (`id`),
  CONSTRAINT `fk_board_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_board_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `board_comment` (`id`)
);

CREATE TABLE `board_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `post_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_board_like_post_id` (`post_id`),
  KEY `idx_board_like_user_id` (`user_id`),
  CONSTRAINT `fk_board_like_post` FOREIGN KEY (`post_id`) REFERENCES `board_post` (`id`),
  CONSTRAINT `fk_board_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);
