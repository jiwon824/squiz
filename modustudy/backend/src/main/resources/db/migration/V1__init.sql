
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `ai_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `feature_type` varchar(50) NOT NULL,
  `reference_id` bigint DEFAULT NULL,
  `feedback` enum('POSITIVE','NEGATIVE') NOT NULL,
  `comment` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `ai_feedback_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `check_type` enum('BLE','SELF','AUTO') DEFAULT 'SELF',
  `status` enum('PRESENT','LATE','ABSENT','EXCUSED') DEFAULT 'ABSENT',
  `checked_at` timestamp NULL DEFAULT NULL,
  `checked_by` bigint DEFAULT NULL,
  `excuse_reason` text,
  `excuse_status` enum('PENDING','APPROVED','REJECTED') DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_user` (`session_id`,`user_id`),
  KEY `user_id` (`user_id`),
  KEY `checked_by` (`checked_by`),
  CONSTRAINT `attendance_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `study_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `attendance_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `attendance_ibfk_3` FOREIGN KEY (`checked_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `badge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `badge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(200) NOT NULL,
  `icon` varchar(10) DEFAULT NULL,
  `category` enum('ACTIVITY','STREAK','STUDY','ATTENDANCE','PARTICIPATION','QUIZ','MASTER','SPECIAL') NOT NULL,
  `condition_type` varchar(50) NOT NULL,
  `condition_value` int NOT NULL,
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `board_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `board_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `content` text NOT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `post_id` (`post_id`),
  KEY `user_id` (`user_id`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `board_comment_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `board_post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `board_comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `board_comment_ibfk_3` FOREIGN KEY (`parent_id`) REFERENCES `board_comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `board_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `board_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `board_like_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `board_post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `board_like_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `board_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `board_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `category` enum('PROJECT','COMPETITION','JOB','FREE') NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `view_count` int DEFAULT '0',
  `like_count` int DEFAULT '0',
  `comment_count` int DEFAULT '0',
  `is_deleted` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `board_post_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `calendar_watch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `calendar_watch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `channel_id` varchar(255) NOT NULL COMMENT 'Google Channel ID',
  `resource_id` varchar(255) NOT NULL COMMENT 'Google Resource ID',
  `expires_at` timestamp NOT NULL COMMENT 'Watch 만료 시간',
  `sync_token` varchar(255) DEFAULT NULL COMMENT 'Delta Sync용 토큰',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_calendar_watch_channel` (`channel_id`),
  KEY `idx_calendar_watch_user` (`user_id`),
  KEY `idx_calendar_watch_expires` (`expires_at`),
  CONSTRAINT `calendar_watch_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `comendle_attempt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comendle_attempt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `daily_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `session_id` varchar(100) DEFAULT NULL,
  `is_solved` tinyint(1) DEFAULT '0',
  `guess_count` int DEFAULT '0',
  `solved_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `daily_id` (`daily_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `comendle_attempt_ibfk_1` FOREIGN KEY (`daily_id`) REFERENCES `comendle_daily` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comendle_attempt_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `comendle_daily`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comendle_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word_id` bigint NOT NULL,
  `game_date` date NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `game_date` (`game_date`),
  KEY `word_id` (`word_id`),
  CONSTRAINT `comendle_daily_ibfk_1` FOREIGN KEY (`word_id`) REFERENCES `comendle_word` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `comendle_guess`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comendle_guess` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_id` bigint NOT NULL,
  `guess_number` int NOT NULL,
  `guessed_word` varchar(100) NOT NULL,
  `similarity` decimal(5,2) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `attempt_id` (`attempt_id`),
  CONSTRAINT `comendle_guess_ibfk_1` FOREIGN KEY (`attempt_id`) REFERENCES `comendle_attempt` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `comendle_streak`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comendle_streak` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `current_streak` int DEFAULT '0',
  `max_streak` int DEFAULT '0',
  `total_solved` int DEFAULT '0',
  `total_played` int DEFAULT '0',
  `last_played_date` date DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `comendle_streak_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `comendle_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comendle_word` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word` varchar(100) NOT NULL,
  `hint` varchar(500) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `difficulty` enum('EASY','MEDIUM','HARD') DEFAULT 'MEDIUM',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `contribution_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contribution_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `contribution_date` date NOT NULL,
  `activity_type` enum('STUDY_ATTENDANCE','QUIZ_CONTEST') NOT NULL,
  `reference_id` bigint NOT NULL,
  `reference_name` varchar(200) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `contribution_detail_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `curriculum`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `curriculum` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `week_number` int NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_study_week` (`study_id`,`week_number`),
  CONSTRAINT `curriculum_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `daily_contribution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_contribution` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `contribution_date` date NOT NULL,
  `has_activity` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `activity_count` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`contribution_date`),
  CONSTRAINT `daily_contribution_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `daily_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `daily_report_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `category` enum('YESTERDAY','TODAY','BLOCKER') NOT NULL,
  `content` text NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `daily_report_id` (`daily_report_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `daily_item_ibfk_1` FOREIGN KEY (`daily_report_id`) REFERENCES `daily_report` (`id`) ON DELETE CASCADE,
  CONSTRAINT `daily_item_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `daily_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `report_date` date NOT NULL,
  `summary` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_study_date` (`study_id`,`report_date`),
  UNIQUE KEY `UKl0kfn930jvmabfocko0fix309` (`study_id`,`report_date`),
  CONSTRAINT `daily_report_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `direct_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `direct_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  `content` varchar(2000) NOT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `sender_id` (`sender_id`),
  KEY `idx_direct_message_conversation_created` (`conversation_id`,`created_at` DESC),
  CONSTRAINT `direct_message_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `dm_conversation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `direct_message_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dm_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dm_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user1_id` bigint NOT NULL,
  `user2_id` bigint NOT NULL,
  `user1_last_read_message_id` bigint DEFAULT NULL,
  `user2_last_read_message_id` bigint DEFAULT NULL,
  `user1_deleted` tinyint(1) DEFAULT '0',
  `user2_deleted` tinyint(1) DEFAULT '0',
  `last_message_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dm_conversation_users` (`user1_id`,`user2_id`),
  KEY `idx_dm_conversation_user1` (`user1_id`),
  KEY `idx_dm_conversation_user2` (`user2_id`),
  KEY `idx_dm_conversation_last_message` (`last_message_at` DESC),
  CONSTRAINT `dm_conversation_ibfk_1` FOREIGN KEY (`user1_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `dm_conversation_ibfk_2` FOREIGN KEY (`user2_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `exp_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exp_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `exp_amount` int NOT NULL,
  `exp_type` enum('SIGNUP','DAILY_LOGIN','STUDY_CREATE','STUDY_JOIN','STUDY_COMPLETE','QUIZ_SOLVE','QUIZ_CREATE','REVIEW_WRITE','COMMENT_WRITE','ATTENDANCE','LEVEL_UP','EVENT','ADMIN_GRANT','PENALTY') NOT NULL,
  `reference_type` enum('STUDY','QUIZ','REVIEW','COMMENT','ATTENDANCE','NONE') DEFAULT 'NONE',
  `reference_id` bigint DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `balance_after` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `exp_transaction_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `fcm_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fcm_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(500) NOT NULL,
  `device_type` enum('ANDROID','IOS','WEB') NOT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `fcm_token_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `format`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `format` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_format_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `friendship`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friendship` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `requester_id` bigint NOT NULL,
  `addressee_id` bigint NOT NULL,
  `status` enum('PENDING','ACCEPTED') DEFAULT 'PENDING',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friendship` (`requester_id`,`addressee_id`),
  KEY `idx_friendship_requester` (`requester_id`),
  KEY `idx_friendship_addressee` (`addressee_id`),
  KEY `idx_friendship_status` (`status`),
  CONSTRAINT `friendship_ibfk_1` FOREIGN KEY (`requester_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `friendship_ibfk_2` FOREIGN KEY (`addressee_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `hint_cost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hint_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `hint_type` varchar(50) NOT NULL,
  `cost_points` int NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `it_news`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `it_news` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(300) NOT NULL,
  `summary` text,
  `source_url` text NOT NULL,
  `source_name` varchar(100) DEFAULT NULL,
  `thumbnail_url` text,
  `category` varchar(50) DEFAULT NULL,
  `published_at` timestamp NULL DEFAULT NULL,
  `view_count` int DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `level_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `level_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `level` int NOT NULL,
  `level_name` varchar(50) NOT NULL,
  `required_exp` int NOT NULL,
  `level_icon_url` varchar(500) DEFAULT NULL,
  `level_color` varchar(20) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `login_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `provider` enum('GOOGLE','KAKAO','NAVER','EMAIL') NOT NULL,
  `login_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `ip_address` varchar(45) DEFAULT NULL,
  `device_info` varchar(200) DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `login_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `uploader_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text,
  `material_type` enum('LINK','FILE','IMAGE','VIDEO') NOT NULL,
  `url` varchar(500) DEFAULT NULL,
  `file_path` varchar(500) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `week_number` int DEFAULT NULL,
  `view_count` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `file_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `study_id` (`study_id`),
  KEY `uploader_id` (`uploader_id`),
  CONSTRAINT `material_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE,
  CONSTRAINT `material_ibfk_2` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `material_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `material_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `material_id` (`material_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `material_comment_ibfk_1` FOREIGN KEY (`material_id`) REFERENCES `material` (`id`) ON DELETE CASCADE,
  CONSTRAINT `material_comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `session_id` bigint DEFAULT NULL,
  `workspace_id` bigint DEFAULT NULL,
  `title` varchar(200) DEFAULT NULL,
  `started_at` timestamp NULL DEFAULT NULL,
  `ended_at` timestamp NULL DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `planned_duration_seconds` int DEFAULT NULL,
  `participant_count` int DEFAULT '0',
  `status` enum('WAITING','IN_PROGRESS','ENDED') DEFAULT 'WAITING',
  `recording_status` enum('WAITING','RECORDING','READY','FAILED') DEFAULT 'WAITING',
  `stt_status` enum('PENDING','PROCESSING','DONE','FAILED') DEFAULT 'PENDING',
  `summary_status` enum('PENDING','PROCESSING','DONE','FAILED') DEFAULT 'PENDING',
  `auto_share_summary` tinyint(1) DEFAULT '0',
  `share_workspace_id` bigint DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `meeting_type` enum('DAILY','WEEKLY','FREE','OTHER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `study_id` (`study_id`),
  KEY `session_id` (`session_id`),
  KEY `workspace_id` (`workspace_id`),
  CONSTRAINT `meeting_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE,
  CONSTRAINT `meeting_ibfk_2` FOREIGN KEY (`session_id`) REFERENCES `study_session` (`id`),
  CONSTRAINT `meeting_ibfk_3` FOREIGN KEY (`workspace_id`) REFERENCES `workspace` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_action_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_action_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `content` tinytext NOT NULL,
  `status` enum('TODO','DONE') DEFAULT 'TODO',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `assignee_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `meeting_id` (`meeting_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `meeting_action_item_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE,
  CONSTRAINT `meeting_action_item_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_audio_recording`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_audio_recording` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `track_type` enum('MIXED','INDIVIDUAL') NOT NULL,
  `recording_url` varchar(500) NOT NULL,
  `format` varchar(20) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `meeting_id` (`meeting_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `meeting_audio_recording_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE,
  CONSTRAINT `meeting_audio_recording_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `sender_name` varchar(100) NOT NULL,
  `content` tinytext NOT NULL,
  `sent_at` datetime NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `idx_meeting_chat_message_meeting_id_sent_at` (`meeting_id`,`sent_at`),
  CONSTRAINT `meeting_chat_message_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE,
  CONSTRAINT `meeting_chat_message_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `joined_at` timestamp NOT NULL,
  `left_at` timestamp NULL DEFAULT NULL,
  `is_muted` tinyint(1) DEFAULT '0',
  `is_camera_on` tinyint(1) DEFAULT '0',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `meeting_id` (`meeting_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `meeting_participant_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE,
  CONSTRAINT `meeting_participant_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_participant_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_participant_summary` (
  `id` varchar(255) NOT NULL,
  `meeting_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `summary` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `meeting_id` (`meeting_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `meeting_participant_summary_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE,
  CONSTRAINT `meeting_participant_summary_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_photo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_photo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `image_url` varchar(500) NOT NULL,
  `captured_at` timestamp NOT NULL,
  `is_selected` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `meeting_id` (`meeting_id`),
  CONSTRAINT `meeting_photo_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_recording`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_recording` (
  `meeting_recording_id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint DEFAULT NULL,
  `recording_url` varchar(500) DEFAULT NULL,
  `format` varchar(20) DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `started_at` timestamp NULL DEFAULT NULL,
  `ended_at` timestamp NULL DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `status` enum('UPLOADING','READY','FAILED') DEFAULT 'UPLOADING',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`meeting_recording_id`),
  KEY `meeting_id` (`meeting_id`),
  CONSTRAINT `meeting_recording_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_speech_segment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_speech_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `speaker_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `speaker_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `speech_timestamp` bigint NOT NULL,
  `duration_ms` int DEFAULT NULL,
  `text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_speech_segment_meeting_timestamp` (`meeting_id`,`speech_timestamp`),
  CONSTRAINT `meeting_speech_segment_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_stt_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_stt_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `track_type` enum('MIXED','INDIVIDUAL') NOT NULL,
  `file_url` varchar(500) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_meeting_stt_file` (`meeting_id`,`track_type`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `meeting_stt_file_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE,
  CONSTRAINT `meeting_stt_file_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_stt_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_stt_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `track_type` enum('MIXED','INDIVIDUAL') NOT NULL,
  `file_url` varchar(500) NOT NULL,
  `action_items` tinytext,
  `keywords` tinytext,
  `highlights_json` tinytext,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `summary_source` enum('REALTIME_STT','FULL_AUDIO','UNKNOWN') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_meeting_stt_summary` (`meeting_id`,`track_type`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `meeting_stt_summary_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE,
  CONSTRAINT `meeting_stt_summary_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_summary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `summary` text,
  `action_items` json DEFAULT NULL,
  `keywords` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `meeting_id` (`meeting_id`),
  CONSTRAINT `meeting_summary_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `meeting_transcript`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_transcript` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text NOT NULL,
  `timestamp_seconds` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `meeting_id` (`meeting_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `meeting_transcript_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`) ON DELETE CASCADE,
  CONSTRAINT `meeting_transcript_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `workspace_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text NOT NULL,
  `message_type` enum('TEXT','IMAGE','FILE','SYSTEM') DEFAULT 'TEXT',
  `file_url` varchar(500) DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  `is_pinned` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_message_workspace_id` (`workspace_id`),
  KEY `idx_message_user_id` (`user_id`),
  KEY `idx_message_created_at` (`created_at`),
  KEY `idx_message_pinned` (`workspace_id`,`is_pinned`),
  CONSTRAINT `message_ibfk_1` FOREIGN KEY (`workspace_id`) REFERENCES `workspace` (`id`) ON DELETE CASCADE,
  CONSTRAINT `message_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `news_bookmark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `news_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `news_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_news` (`user_id`,`news_id`),
  KEY `news_id` (`news_id`),
  CONSTRAINT `news_bookmark_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `news_bookmark_ibfk_2` FOREIGN KEY (`news_id`) REFERENCES `it_news` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` enum('CHAT','SCHEDULE','ATTENDANCE','STUDY_UPDATE','STUDY_APPLICATION','QUIZ','SYSTEM','FRIEND') NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text,
  `reference_type` varchar(50) DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `notification_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `notification_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `notification_type` varchar(50) NOT NULL,
  `is_enabled` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_type` (`user_id`,`notification_type`),
  CONSTRAINT `notification_setting_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `password_reset_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(255) NOT NULL,
  `expires_at` timestamp NOT NULL,
  `is_used` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `used` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `password_reset_token_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `penalty`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `penalty` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(200) NOT NULL,
  `icon` varchar(10) DEFAULT NULL,
  `grant_condition` varchar(200) DEFAULT NULL,
  `removal_condition` varchar(200) DEFAULT NULL,
  `removal_required` int DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `point_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `point_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `point_amount` int NOT NULL,
  `transaction_type` enum('EARN','SPEND') NOT NULL,
  `point_type` enum('SIGNUP','DAILY_LOGIN','STUDY_CREATE','STUDY_JOIN','STUDY_COMPLETE','QUIZ_SOLVE','QUIZ_CREATE','REVIEW_WRITE','COMMENT_WRITE','ATTENDANCE','LEVEL_UP','EVENT','ADMIN_GRANT','HINT_USE','ITEM_PURCHASE') NOT NULL,
  `reference_type` enum('STUDY','QUIZ','REVIEW','COMMENT','ATTENDANCE','HINT','ITEM','NONE') DEFAULT 'NONE',
  `reference_id` bigint DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `balance_after` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `point_transaction_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `profile_image_url` varchar(500) DEFAULT NULL,
  `profile_image_source` enum('KAKAO','GOOGLE','NAVER','UPLOAD') DEFAULT 'UPLOAD',
  `bio` text,
  `social_links` json DEFAULT NULL,
  `tech` json DEFAULT NULL,
  `favorite` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `profile_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `curriculum_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `is_completed` tinyint(1) DEFAULT '0',
  `completed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_curriculum_user` (`curriculum_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `progress_ibfk_1` FOREIGN KEY (`curriculum_id`) REFERENCES `curriculum` (`id`) ON DELETE CASCADE,
  CONSTRAINT `progress_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_blob_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `BLOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_calendars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_calendars` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `CALENDAR_NAME` varchar(190) NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_cron_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `CRON_EXPRESSION` varchar(120) NOT NULL,
  `TIME_ZONE_ID` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_fired_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `ENTRY_ID` varchar(95) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `INSTANCE_NAME` varchar(190) NOT NULL,
  `FIRED_TIME` bigint NOT NULL,
  `SCHED_TIME` bigint NOT NULL,
  `PRIORITY` int NOT NULL,
  `STATE` varchar(16) NOT NULL,
  `JOB_NAME` varchar(190) DEFAULT NULL,
  `JOB_GROUP` varchar(190) DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`),
  KEY `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
  KEY `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_job_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_job_details` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `JOB_NAME` varchar(190) NOT NULL,
  `JOB_GROUP` varchar(190) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) NOT NULL,
  `IS_DURABLE` varchar(1) NOT NULL,
  `IS_NONCONCURRENT` varchar(1) NOT NULL,
  `IS_UPDATE_DATA` varchar(1) NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) NOT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_locks` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `LOCK_NAME` varchar(40) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_paused_trigger_grps` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_scheduler_state` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `INSTANCE_NAME` varchar(190) NOT NULL,
  `LAST_CHECKIN_TIME` bigint NOT NULL,
  `CHECKIN_INTERVAL` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simple_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `REPEAT_COUNT` bigint NOT NULL,
  `REPEAT_INTERVAL` bigint NOT NULL,
  `TIMES_TRIGGERED` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simprop_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `STR_PROP_1` varchar(512) DEFAULT NULL,
  `STR_PROP_2` varchar(512) DEFAULT NULL,
  `STR_PROP_3` varchar(512) DEFAULT NULL,
  `INT_PROP_1` int DEFAULT NULL,
  `INT_PROP_2` int DEFAULT NULL,
  `LONG_PROP_1` bigint DEFAULT NULL,
  `LONG_PROP_2` bigint DEFAULT NULL,
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL,
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `qrtz_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `JOB_NAME` varchar(190) NOT NULL,
  `JOB_GROUP` varchar(190) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint DEFAULT NULL,
  `PREV_FIRE_TIME` bigint DEFAULT NULL,
  `PRIORITY` int DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint NOT NULL,
  `END_TIME` bigint DEFAULT NULL,
  `CALENDAR_NAME` varchar(190) DEFAULT NULL,
  `MISFIRE_INSTR` smallint DEFAULT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
  KEY `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `qrtz_job_details` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_answer` (
  `participant_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `user_answer` json DEFAULT NULL,
  `is_correct` tinyint(1) DEFAULT NULL,
  `score` int DEFAULT '0',
  `time_taken_seconds` int DEFAULT NULL,
  `answered_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`participant_id`,`question_id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `quiz_answer_ibfk_1` FOREIGN KEY (`participant_id`) REFERENCES `quiz_participant` (`id`) ON DELETE CASCADE,
  CONSTRAINT `quiz_answer_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `quiz_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `code` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text,
  `depth` int NOT NULL DEFAULT '0',
  `sort_order` int DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_parent_depth` (`parent_id`,`depth`,`sort_order`),
  CONSTRAINT `quiz_category_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `quiz_category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_contest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_contest` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `description` text,
  `category_id` bigint DEFAULT NULL,
  `contest_type` enum('PUBLIC','STUDY') DEFAULT 'PUBLIC',
  `study_id` bigint DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `status` enum('DRAFT','SCHEDULED','IN_PROGRESS','ENDED') DEFAULT 'DRAFT',
  `scheduled_at` timestamp NULL DEFAULT NULL,
  `started_at` timestamp NULL DEFAULT NULL,
  `ended_at` timestamp NULL DEFAULT NULL,
  `time_limit_seconds` int DEFAULT '30',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `category_id` (`category_id`),
  KEY `study_id` (`study_id`),
  KEY `created_by` (`created_by`),
  CONSTRAINT `quiz_contest_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `quiz_category` (`id`),
  CONSTRAINT `quiz_contest_ibfk_2` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`),
  CONSTRAINT `quiz_contest_ibfk_3` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_contest_chat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_contest_chat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contest_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `participant_id` bigint NOT NULL,
  `message` varchar(500) NOT NULL,
  `message_type` enum('TEXT','SYSTEM') NOT NULL DEFAULT 'TEXT',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `participant_id` (`participant_id`),
  KEY `idx_contest_created` (`contest_id`,`created_at`),
  CONSTRAINT `quiz_contest_chat_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest` (`id`) ON DELETE CASCADE,
  CONSTRAINT `quiz_contest_chat_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `quiz_contest_chat_ibfk_3` FOREIGN KEY (`participant_id`) REFERENCES `quiz_participant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_contest_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_contest_state` (
  `contest_id` bigint NOT NULL,
  `current_question_pool_id` bigint DEFAULT NULL,
  `current_question_started_at` timestamp NULL DEFAULT NULL,
  `is_showing_results` tinyint(1) DEFAULT '0',
  `phase` enum('WAITING','QUESTION','RESULT','ENDED') DEFAULT 'WAITING',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`contest_id`),
  KEY `current_question_pool_id` (`current_question_pool_id`),
  CONSTRAINT `quiz_contest_state_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest` (`id`) ON DELETE CASCADE,
  CONSTRAINT `quiz_contest_state_ibfk_2` FOREIGN KEY (`current_question_pool_id`) REFERENCES `quiz_question_pool` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text,
  `badge_code` varchar(50) DEFAULT NULL,
  `total_sections` int DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_course_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_course_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quiz_course_id` bigint NOT NULL,
  `section_number` int NOT NULL,
  `question_number` int NOT NULL,
  `question_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_type` enum('MULTIPLE_CHOICE','SHORT_ANSWER','MULTIPLE_CHOICE_MULTIPLE') COLLATE utf8mb4_unicode_ci DEFAULT 'MULTIPLE_CHOICE',
  `options` json DEFAULT NULL,
  `correct_answer` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `explanation` text COLLATE utf8mb4_unicode_ci,
  `keywords` json DEFAULT NULL COMMENT '서술형 채점용 핵심 키워드 JSON 배열',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `quiz_course_id` (`quiz_course_id`,`section_number`),
  KEY `idx_quiz_course_question_type` (`question_type`),
  CONSTRAINT `quiz_course_question_ibfk_1` FOREIGN KEY (`quiz_course_id`, `section_number`) REFERENCES `quiz_course_section` (`quiz_course_id`, `section_number`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_course_section`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_course_section` (
  `quiz_course_id` bigint NOT NULL,
  `section_number` int NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `total_questions` int DEFAULT '0',
  `pass_score` int DEFAULT '70',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`quiz_course_id`,`section_number`),
  CONSTRAINT `quiz_course_section_ibfk_1` FOREIGN KEY (`quiz_course_id`) REFERENCES `quiz_course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contest_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `nickname` varchar(50) NOT NULL,
  `total_score` int DEFAULT '0',
  `correct_count` int DEFAULT '0',
  `rank` int DEFAULT NULL,
  `last_answer_time` timestamp NULL DEFAULT NULL,
  `joined_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_contest_score` (`contest_id`,`total_score`,`last_answer_time`),
  KEY `idx_participant_user` (`user_id`,`joined_at`),
  CONSTRAINT `quiz_participant_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest` (`id`) ON DELETE CASCADE,
  CONSTRAINT `quiz_participant_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_practice_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_practice_answer` (
  `practice_record_id` bigint NOT NULL,
  `question_pool_id` bigint NOT NULL,
  `user_answer` json DEFAULT NULL,
  `is_correct` tinyint(1) NOT NULL,
  `time_taken_seconds` int DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`practice_record_id`,`question_pool_id`),
  KEY `question_pool_id` (`question_pool_id`),
  CONSTRAINT `quiz_practice_answer_ibfk_1` FOREIGN KEY (`practice_record_id`) REFERENCES `quiz_practice_record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `quiz_practice_answer_ibfk_2` FOREIGN KEY (`question_pool_id`) REFERENCES `quiz_question_pool` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_practice_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_practice_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `total_questions` int NOT NULL,
  `correct_count` int NOT NULL,
  `score` int NOT NULL,
  `time_spent_seconds` int DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `category_id` (`category_id`),
  KEY `idx_user_created` (`user_id`,`created_at`),
  CONSTRAINT `quiz_practice_record_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `quiz_practice_record_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `quiz_category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_practice_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_practice_stats` (
  `user_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `total_attempted` int DEFAULT '0',
  `total_correct` int DEFAULT '0',
  `best_score` int DEFAULT '0',
  `last_attempted_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`category_id`),
  KEY `category_id` (`category_id`),
  CONSTRAINT `quiz_practice_stats_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `quiz_practice_stats_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `quiz_category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contest_id` bigint NOT NULL,
  `question_pool_id` bigint DEFAULT NULL,
  `question_number` int NOT NULL,
  `question_text` text NOT NULL,
  `question_type` enum('MULTIPLE_CHOICE','SHORT_ANSWER') DEFAULT 'MULTIPLE_CHOICE',
  `options` json DEFAULT NULL,
  `correct_answer` varchar(500) NOT NULL,
  `explanation` text,
  `points` int DEFAULT '10',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `contest_id` (`contest_id`),
  KEY `question_pool_id` (`question_pool_id`),
  CONSTRAINT `quiz_question_ibfk_1` FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest` (`id`) ON DELETE CASCADE,
  CONSTRAINT `quiz_question_ibfk_2` FOREIGN KEY (`question_pool_id`) REFERENCES `quiz_question_pool` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_question_pool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_question_pool` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category_id` bigint NOT NULL,
  `question_text` text NOT NULL,
  `question_type` enum('MULTIPLE_CHOICE','MULTIPLE_CHOICE_MULTIPLE','SHORT_ANSWER') DEFAULT 'MULTIPLE_CHOICE',
  `correct_answer` json NOT NULL,
  `explanation` text,
  `difficulty` enum('EASY','MEDIUM','HARD') DEFAULT 'MEDIUM',
  `tags` json DEFAULT NULL,
  `usage_count` int DEFAULT '0',
  `correct_rate` decimal(5,2) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `created_by` (`created_by`),
  KEY `idx_category_difficulty` (`category_id`,`difficulty`,`is_active`),
  CONSTRAINT `quiz_question_pool_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `quiz_category` (`id`) ON DELETE CASCADE,
  CONSTRAINT `quiz_question_pool_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `quiz_question_pool_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_question_pool_option` (
  `question_pool_id` bigint NOT NULL,
  `option_label` varchar(10) NOT NULL,
  `option_text` varchar(500) NOT NULL,
  `is_correct` tinyint(1) DEFAULT '0',
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`question_pool_id`,`option_label`),
  KEY `idx_question_option_order` (`question_pool_id`,`sort_order`),
  CONSTRAINT `quiz_question_pool_option_ibfk_1` FOREIGN KEY (`question_pool_id`) REFERENCES `quiz_question_pool` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `refresh_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token` varchar(500) NOT NULL,
  `expires_at` timestamp NOT NULL,
  `is_revoked` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token` (`token`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `refresh_token_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `region` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(100) NOT NULL,
  `name` varchar(50) NOT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `level` int NOT NULL DEFAULT '1',
  `parent_id` bigint DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_region_parent` (`parent_id`),
  KEY `idx_region_level` (`level`),
  CONSTRAINT `region_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `region` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `report_type` enum('APPLICATION','MONTHLY_RESULT') NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` json DEFAULT NULL,
  `report_month` date DEFAULT NULL,
  `status` enum('DRAFT','SUBMITTED') DEFAULT 'DRAFT',
  `created_by` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `study_id` (`study_id`),
  KEY `created_by` (`created_by`),
  CONSTRAINT `report_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE,
  CONSTRAINT `report_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `retrospective`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `retrospective` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `session_id` bigint DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `retrospective_type` enum('KPT','FREE') DEFAULT 'KPT',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `study_id` (`study_id`),
  KEY `session_id` (`session_id`),
  CONSTRAINT `retrospective_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE,
  CONSTRAINT `retrospective_ibfk_2` FOREIGN KEY (`session_id`) REFERENCES `study_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `retrospective_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `retrospective_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `retrospective_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `category` enum('KEEP','PROBLEM','TRY') NOT NULL,
  `content` text NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `retrospective_id` (`retrospective_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `retrospective_item_ibfk_1` FOREIGN KEY (`retrospective_id`) REFERENCES `retrospective` (`id`) ON DELETE CASCADE,
  CONSTRAINT `retrospective_item_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `reward_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reward_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action_type` enum('SIGNUP','DAILY_LOGIN','STUDY_CREATE','STUDY_JOIN','STUDY_COMPLETE','QUIZ_SOLVE','QUIZ_CREATE','REVIEW_WRITE','COMMENT_WRITE','ATTENDANCE') NOT NULL,
  `exp_amount` int NOT NULL,
  `point_amount` int NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `daily_limit` int DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `session_memo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `session_memo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(500) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_user_memo` (`session_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `session_memo_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `study_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `session_memo_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `leader_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text,
  `topic_id` bigint NOT NULL,
  `format_id` bigint DEFAULT NULL,
  `study_type` enum('PLANNED','LIGHTNING') NOT NULL,
  `meeting_type` enum('ONLINE','OFFLINE','HYBRID') DEFAULT 'ONLINE',
  `region_id` bigint DEFAULT NULL,
  `location_detail` varchar(200) DEFAULT NULL,
  `schedule_summary` varchar(100) DEFAULT NULL,
  `schedule_days` varchar(50) DEFAULT NULL,
  `schedule_time` time DEFAULT NULL,
  `max_members` int DEFAULT '10',
  `is_public` tinyint(1) DEFAULT '1',
  `status` enum('DRAFT','SCHEDULED','RECRUITING','RECRUIT_CLOSED','PENDING','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'DRAFT',
  `penalty_policy` enum('STRICT','NORMAL','LENIENT','RATIO','NONE') DEFAULT 'NORMAL',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `total_sessions` int DEFAULT NULL,
  `recruit_start_date` date DEFAULT NULL,
  `recruit_end_date` date DEFAULT NULL,
  `extension_count` int DEFAULT '0',
  `textbook` varchar(500) DEFAULT NULL,
  `goal` varchar(500) DEFAULT NULL,
  `difficulty` enum('BEGINNER','ELEMENTARY','INTERMEDIATE','ADVANCED') DEFAULT 'INTERMEDIATE',
  `prerequisites` text,
  `process_detail` text,
  `target_org_type` varchar(50) DEFAULT NULL,
  `target_org_criteria` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `intro` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `leader_id` (`leader_id`),
  KEY `region_id` (`region_id`),
  KEY `idx_study_topic` (`topic_id`),
  KEY `idx_study_format` (`format_id`),
  CONSTRAINT `study_ibfk_1` FOREIGN KEY (`leader_id`) REFERENCES `user` (`id`),
  CONSTRAINT `study_ibfk_2` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`),
  CONSTRAINT `study_ibfk_3` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `study_ibfk_4` FOREIGN KEY (`format_id`) REFERENCES `format` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_application` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `message` text,
  `matching_score` decimal(5,2) DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  `rejected_reason` varchar(500) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `processed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `study_id` (`study_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `study_application_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_application_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_bookmark`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `study_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_study` (`user_id`,`study_id`),
  KEY `study_id` (`study_id`),
  CONSTRAINT `study_bookmark_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_bookmark_ibfk_2` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `content` text NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `study_id` (`study_id`),
  KEY `user_id` (`user_id`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `study_comment_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `study_comment_ibfk_3` FOREIGN KEY (`parent_id`) REFERENCES `study_comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_leader_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_leader_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `leader_id` bigint NOT NULL,
  `rating` decimal(2,1) NOT NULL,
  `comment` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_study_reviewer` (`study_id`,`reviewer_id`),
  KEY `reviewer_id` (`reviewer_id`),
  KEY `leader_id` (`leader_id`),
  CONSTRAINT `study_leader_review_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_leader_review_ibfk_2` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `study_leader_review_ibfk_3` FOREIGN KEY (`leader_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` enum('LEADER','MEMBER') DEFAULT 'MEMBER',
  `status` enum('PENDING','APPROVED','REJECTED','LEFT','KICKED') DEFAULT 'PENDING',
  `joined_at` timestamp NULL DEFAULT NULL,
  `left_at` timestamp NULL DEFAULT NULL,
  `is_probation` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_study_user` (`study_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `study_member_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_member_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_quiz`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_quiz` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `session_id` bigint DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `source_type` enum('MEETING','MATERIAL','MANUAL') NOT NULL,
  `source_id` bigint DEFAULT NULL,
  `status` enum('ACTIVE','DISABLED') DEFAULT 'ACTIVE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `study_id` (`study_id`),
  KEY `session_id` (`session_id`),
  CONSTRAINT `study_quiz_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_quiz_ibfk_2` FOREIGN KEY (`session_id`) REFERENCES `study_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_quiz_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_quiz_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `question_index` int NOT NULL COMMENT '문제 순서 (0부터 시작)',
  `user_answer` json DEFAULT NULL COMMENT '사용자 답변',
  `is_correct` tinyint(1) DEFAULT '0',
  `response_time_ms` bigint DEFAULT '0' COMMENT '응답 시간(ms)',
  `answered_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attempt_question` (`attempt_id`,`question_id`),
  KEY `question_id` (`question_id`),
  KEY `idx_attempt_index` (`attempt_id`,`question_index`),
  CONSTRAINT `study_quiz_answer_ibfk_1` FOREIGN KEY (`attempt_id`) REFERENCES `study_quiz_attempt` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_quiz_answer_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `study_quiz_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_quiz_attempt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_quiz_attempt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quiz_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `score` int DEFAULT '0',
  `total_questions` int DEFAULT NULL,
  `correct_count` int DEFAULT NULL,
  `status` enum('IN_PROGRESS','COMPLETED','ABANDONED') DEFAULT 'IN_PROGRESS',
  `started_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `last_answered_at` timestamp NULL DEFAULT NULL,
  `current_question_index` int DEFAULT '0',
  `completed_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `idx_quiz_attempt_status` (`quiz_id`,`status`),
  CONSTRAINT `study_quiz_attempt_ibfk_1` FOREIGN KEY (`quiz_id`) REFERENCES `study_quiz` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_quiz_attempt_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_quiz_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_quiz_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quiz_id` bigint NOT NULL,
  `question_text` text NOT NULL,
  `question_type` enum('MULTIPLE_CHOICE','SHORT_ANSWER') DEFAULT 'MULTIPLE_CHOICE',
  `options` json DEFAULT NULL,
  `correct_answer` varchar(500) NOT NULL,
  `explanation` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `quiz_id` (`quiz_id`),
  CONSTRAINT `study_quiz_question_ibfk_1` FOREIGN KEY (`quiz_id`) REFERENCES `study_quiz` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_recommend_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_recommend_action` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `log_id` bigint NOT NULL,
  `item_id` bigint DEFAULT NULL,
  `study_id` bigint NOT NULL,
  `action_type` enum('CLICK','APPLY','BOOKMARK','DISMISS') NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `item_id` (`item_id`),
  KEY `study_id` (`study_id`),
  KEY `idx_recommend_action_log` (`log_id`),
  KEY `idx_recommend_action_type` (`action_type`),
  CONSTRAINT `study_recommend_action_ibfk_1` FOREIGN KEY (`log_id`) REFERENCES `study_recommend_log` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_recommend_action_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `study_recommend_item` (`id`) ON DELETE SET NULL,
  CONSTRAINT `study_recommend_action_ibfk_3` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_recommend_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_recommend_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `log_id` bigint NOT NULL,
  `study_id` bigint NOT NULL,
  `rank_position` int NOT NULL,
  `matching_score` decimal(7,2) DEFAULT NULL,
  `tech_match_count` int DEFAULT '0',
  `schedule_match_count` int DEFAULT '0',
  `topic_match_count` int DEFAULT '0',
  `match_reason` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `study_id` (`study_id`),
  KEY `idx_recommend_item_log` (`log_id`),
  CONSTRAINT `study_recommend_item_ibfk_1` FOREIGN KEY (`log_id`) REFERENCES `study_recommend_log` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_recommend_item_ibfk_2` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_recommend_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_recommend_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `recommend_type` enum('GENERAL','TOPIC') NOT NULL DEFAULT 'GENERAL',
  `topic_id` bigint DEFAULT NULL,
  `result_count` int NOT NULL DEFAULT '0',
  `user_tech_snapshot` json DEFAULT NULL,
  `user_schedule_snapshot` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `topic_id` (`topic_id`),
  KEY `idx_recommend_log_user` (`user_id`),
  KEY `idx_recommend_log_created` (`created_at`),
  CONSTRAINT `study_recommend_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_recommend_log_ibfk_2` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `session_number` int NOT NULL,
  `title` varchar(200) DEFAULT NULL,
  `description` text,
  `scheduled_at` timestamp NOT NULL,
  `duration_minutes` int DEFAULT '60',
  `location` varchar(200) DEFAULT NULL,
  `is_online` tinyint(1) DEFAULT '1',
  `status` enum('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'SCHEDULED',
  `completed_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_study_session` (`study_id`,`session_number`),
  CONSTRAINT `study_session_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_session_calendar_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_session_calendar_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `google_event_id` varchar(255) NOT NULL COMMENT 'Google Calendar Event ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_user_mapping` (`session_id`,`user_id`),
  UNIQUE KEY `UKcb5tsf9on2kuu8be1a3ceymeu` (`session_id`,`user_id`),
  KEY `idx_session_mapping_session` (`session_id`),
  KEY `idx_session_mapping_user` (`user_id`),
  CONSTRAINT `study_session_calendar_mapping_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `study_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_session_calendar_mapping_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `study_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `intro` varchar(200) DEFAULT NULL,
  `is_system` tinyint(1) DEFAULT '0',
  `template_type` varchar(50) DEFAULT NULL,
  `topic_id` bigint DEFAULT NULL,
  `format_id` bigint DEFAULT NULL,
  `meeting_type` enum('ONLINE','OFFLINE','HYBRID') DEFAULT NULL,
  `description` text,
  `textbook` varchar(500) DEFAULT NULL,
  `goal` varchar(500) DEFAULT NULL,
  `difficulty` enum('BEGINNER','ELEMENTARY','INTERMEDIATE','ADVANCED') DEFAULT NULL,
  `prerequisites` text,
  `process_detail` text,
  `penalty_policy` enum('STRICT','NORMAL','LENIENT','RATIO','NONE') DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `format` varchar(50) DEFAULT NULL,
  `topic` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `idx_study_template_topic` (`topic_id`),
  KEY `idx_study_template_format` (`format_id`),
  CONSTRAINT `study_template_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `study_template_ibfk_2` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE SET NULL,
  CONSTRAINT `study_template_ibfk_3` FOREIGN KEY (`format_id`) REFERENCES `format` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_application` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recruit_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `applied_role` varchar(50) DEFAULT NULL,
  `message` text,
  `portfolio_url` varchar(500) DEFAULT NULL,
  `status` enum('PENDING','ACCEPTED','REJECTED') DEFAULT 'PENDING',
  `rejected_reason` varchar(500) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `processed_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `recruit_id` (`recruit_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `team_application_ibfk_1` FOREIGN KEY (`recruit_id`) REFERENCES `team_recruit` (`id`) ON DELETE CASCADE,
  CONSTRAINT `team_application_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_recruit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_recruit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `category` enum('HACKATHON','PROJECT','COMPETITION') NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `required_roles` json DEFAULT NULL,
  `tech_stack` json DEFAULT NULL,
  `max_members` int DEFAULT '5',
  `current_members` int DEFAULT '1',
  `deadline` date DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `duration` varchar(100) DEFAULT NULL,
  `meeting_type` enum('ONLINE','OFFLINE','HYBRID') DEFAULT 'ONLINE',
  `region_id` bigint DEFAULT NULL,
  `status` enum('RECRUITING','CLOSED','COMPLETED') DEFAULT 'RECRUITING',
  `view_count` int DEFAULT '0',
  `is_deleted` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `region_id` (`region_id`),
  CONSTRAINT `team_recruit_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `team_recruit_ibfk_2` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `template_usage_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `template_usage_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `template_id` bigint NOT NULL,
  `study_id` bigint DEFAULT NULL,
  `used_as_is` tinyint(1) DEFAULT '0',
  `modifications` json DEFAULT NULL,
  `user_tech_stack` json DEFAULT NULL,
  `user_schedule` json DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `study_id` (`study_id`),
  KEY `idx_template_usage_user` (`user_id`),
  KEY `idx_template_usage_template` (`template_id`),
  CONSTRAINT `template_usage_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `template_usage_log_ibfk_2` FOREIGN KEY (`template_id`) REFERENCES `study_template` (`id`) ON DELETE CASCADE,
  CONSTRAINT `template_usage_log_ibfk_3` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `topic` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `icon` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_topic_parent` (`parent_id`),
  KEY `idx_topic_sort` (`sort_order`),
  CONSTRAINT `topic_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `topic` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `department` varchar(255) DEFAULT NULL,
  `position` varchar(255) DEFAULT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `role` enum('USER','ADMIN') DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `is_online` tinyint(1) NOT NULL DEFAULT '0',
  `last_seen_at` timestamp NULL DEFAULT NULL,
  `is_searchable` tinyint(1) NOT NULL DEFAULT '1',
  `bio` text,
  `interests` json DEFAULT NULL,
  `tech_stacks` json DEFAULT NULL,
  `available_days` json DEFAULT NULL COMMENT '가능한 요일 (JSON 배열)',
  `preferred_time_slots` json DEFAULT NULL COMMENT '선호 시간대 (JSON 배열)',
  `preferred_duration_weeks` int DEFAULT NULL COMMENT '선호 스터디 기간 (주)',
  `leader_rating` float DEFAULT '0',
  `leader_review_count` int DEFAULT '0',
  `last_login_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `current_level` int NOT NULL,
  `current_points` int NOT NULL,
  `level_name` varchar(50) DEFAULT NULL,
  `profile_image` varchar(500) DEFAULT NULL,
  `total_exp` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_badge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_badge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `badge_id` bigint NOT NULL,
  `badge_type` enum('ACTIVITY','STREAK','STUDY','QUIZ_KING','SPECIAL') DEFAULT NULL,
  `reference_type` enum('CONTEST','STUDY','GENERAL') DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `rank` int DEFAULT NULL,
  `earned_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_badge` (`user_id`,`badge_id`),
  KEY `badge_id` (`badge_id`),
  KEY `idx_user_badge_type` (`user_id`,`badge_type`,`reference_type`),
  CONSTRAINT `user_badge_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_badge_ibfk_2` FOREIGN KEY (`badge_id`) REFERENCES `badge` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_block`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_block` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `blocker_id` bigint NOT NULL,
  `blocked_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_block` (`blocker_id`,`blocked_id`),
  KEY `idx_user_block_blocker` (`blocker_id`),
  KEY `idx_user_block_blocked` (`blocked_id`),
  CONSTRAINT `user_block_ibfk_1` FOREIGN KEY (`blocker_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_block_ibfk_2` FOREIGN KEY (`blocked_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_course_progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_course_progress` (
  `user_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `current_section` int DEFAULT '1',
  `completed_sections` int DEFAULT '0',
  `is_completed` tinyint(1) DEFAULT '0',
  `completed_at` timestamp NULL DEFAULT NULL,
  `started_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`course_id`),
  KEY `course_id` (`course_id`),
  CONSTRAINT `user_course_progress_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_course_progress_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `quiz_course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_organization`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_organization` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `org_type` varchar(50) NOT NULL,
  `org_data` json NOT NULL,
  `verified_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `last_checked_at` timestamp NULL DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_organization_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_penalty`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_penalty` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `penalty_id` bigint NOT NULL,
  `study_id` bigint DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `removal_progress` int DEFAULT '0',
  `granted_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `removed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `penalty_id` (`penalty_id`),
  KEY `study_id` (`study_id`),
  CONSTRAINT `user_penalty_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_penalty_ibfk_2` FOREIGN KEY (`penalty_id`) REFERENCES `penalty` (`id`),
  CONSTRAINT `user_penalty_ibfk_3` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_review_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_review_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `content_type` enum('COURSE_QUESTION','STUDY_QUESTION') NOT NULL,
  `content_id` bigint NOT NULL,
  `stability` double NOT NULL DEFAULT '0',
  `difficulty` double NOT NULL DEFAULT '5',
  `elapsed_days` int DEFAULT '0',
  `scheduled_days` int DEFAULT '0',
  `reps` int DEFAULT '0',
  `lapses` int DEFAULT '0',
  `state` int DEFAULT '0',
  `last_elapsed_days` int DEFAULT '0',
  `last_response_time_ms` bigint DEFAULT '0',
  `retrievability` double DEFAULT '0',
  `last_reviewed_at` timestamp NULL DEFAULT NULL,
  `next_review_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_content` (`user_id`,`content_type`,`content_id`),
  KEY `idx_user_next_review` (`user_id`,`next_review_at`),
  CONSTRAINT `user_review_items_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_review_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_review_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_item_id` bigint NOT NULL,
  `is_correct` tinyint(1) NOT NULL,
  `response_time_ms` bigint NOT NULL,
  `stability` double NOT NULL,
  `difficulty` double NOT NULL,
  `reviewed_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_review_log_item` (`review_item_id`,`reviewed_at`),
  CONSTRAINT `user_review_log_ibfk_1` FOREIGN KEY (`review_item_id`) REFERENCES `user_review_items` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `day_of_week` enum('MON','TUE','WED','THU','FRI','SAT','SUN') NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `is_available` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_schedule_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_social_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_social_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `provider` enum('GOOGLE','KAKAO','NAVER') NOT NULL,
  `provider_user_id` varchar(100) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `access_token` varchar(2048) DEFAULT NULL COMMENT 'OAuth Access Token',
  `refresh_token` varchar(512) DEFAULT NULL COMMENT 'OAuth Refresh Token',
  `token_expires_at` timestamp NULL DEFAULT NULL COMMENT '토큰 만료 시간',
  `calendar_id` varchar(255) DEFAULT 'primary' COMMENT '연동된 Google Calendar ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_primary` bit(1) NOT NULL,
  `linked_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_user` (`provider`,`provider_user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_social_account_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `level` int DEFAULT '1',
  `level_name` varchar(50) DEFAULT '새싹',
  `total_activity_days` int DEFAULT '0',
  `current_streak` int DEFAULT '0',
  `max_streak` int DEFAULT '0',
  `last_activity_date` date DEFAULT NULL,
  `total_studies_joined` int DEFAULT '0',
  `total_studies_led` int DEFAULT '0',
  `total_chat_count` int DEFAULT '0',
  `total_quiz_count` int DEFAULT '0',
  `total_materials_uploaded` int DEFAULT '0',
  `total_retrospectives` int DEFAULT '0',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `total_experience` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `user_stats_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `workspace`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workspace` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `study_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `study_id` (`study_id`),
  CONSTRAINT `workspace_ibfk_1` FOREIGN KEY (`study_id`) REFERENCES `study` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- =============================================
-- ModuStudy Initial Data
-- Flyway Migration V2 - Topic & Format Reference Data
-- =============================================

-- =============================================
-- Topic (대분류 및 소분류)
-- =============================================

-- 1. 알고리즘/코딩테스트 (id = 1)
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (1, '알고리즘/코딩테스트', NULL, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (11, '백준', 1, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (12, '프로그래머스', 1, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (13, 'SWEA', 1, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (14, 'LeetCode', 1, 4);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (15, '코드포스', 1, 5);

-- 2. CS 기초 (id = 2)
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (2, 'CS 기초', NULL, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (21, '자료구조', 2, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (22, '운영체제', 2, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (23, '네트워크', 2, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (24, '데이터베이스', 2, 4);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (25, '컴퓨터구조', 2, 5);

-- 3. 프론트엔드 (id = 3)
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (3, '프론트엔드', NULL, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (31, 'HTML/CSS', 3, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (32, 'JavaScript', 3, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (33, 'TypeScript', 3, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (34, 'React', 3, 4);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (35, 'Vue', 3, 5);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (36, 'Angular', 3, 6);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (37, 'Next.js', 3, 7);

-- 4. 백엔드 (id = 4)
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (4, '백엔드', NULL, 4);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (41, 'Java/Spring', 4, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (42, 'Python/Django', 4, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (43, 'Node.js', 4, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (44, 'Go', 4, 4);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (45, 'Kotlin', 4, 5);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (46, 'C#/.NET', 4, 6);

-- 5. 인프라/DevOps (id = 5)
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (5, '인프라/DevOps', NULL, 5);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (51, 'Docker/Kubernetes', 5, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (52, 'AWS', 5, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (53, 'CI/CD', 5, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (54, 'Linux', 5, 4);

-- 6. 모바일 (id = 6)
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (6, '모바일', NULL, 6);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (61, 'Android', 6, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (62, 'iOS/Swift', 6, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (63, 'Flutter', 6, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (64, 'React Native', 6, 4);

-- 7. AI/머신러닝 (id = 7)
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (7, 'AI/머신러닝', NULL, 7);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (71, '머신러닝 기초', 7, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (72, '딥러닝', 7, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (73, '자연어처리', 7, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (74, '컴퓨터비전', 7, 4);

-- 8. 데이터 (id = 8)
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (8, '데이터', NULL, 8);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (81, '데이터 분석', 8, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (82, '데이터 엔지니어링', 8, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (83, 'SQL', 8, 3);

-- 9. 보안 (id = 9)
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (9, '보안', NULL, 9);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (91, '웹 보안', 9, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (92, '시스템 보안', 9, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (93, '암호학', 9, 3);

-- 10. 기타 (id = 10)
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (10, '기타', NULL, 10);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (101, '개발 문화', 10, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (102, '취업 준비', 10, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (103, '사이드 프로젝트', 10, 3);

-- =============================================
-- Format (스터디 형식)
-- =============================================
INSERT INTO format (id, name, description, sort_order) VALUES (1, '문제 풀이', '알고리즘, 코딩테스트 문제를 함께 풀고 리뷰합니다', 1);
INSERT INTO format (id, name, description, sort_order) VALUES (2, '독서/책 스터디', '기술 서적을 함께 읽고 토론합니다', 2);
INSERT INTO format (id, name, description, sort_order) VALUES (3, '강의 수강', '온라인 강의를 함께 수강하고 학습 내용을 공유합니다', 3);
INSERT INTO format (id, name, description, sort_order) VALUES (4, '프로젝트', '팀 프로젝트를 함께 진행합니다', 4);
INSERT INTO format (id, name, description, sort_order) VALUES (5, '모의 면접', '기술 면접을 준비하고 모의 면접을 진행합니다', 5);
INSERT INTO format (id, name, description, sort_order) VALUES (6, '발표/세미나', '주제별 발표와 세미나를 진행합니다', 6);
INSERT INTO format (id, name, description, sort_order) VALUES (7, '자격증 준비', '자격증 시험을 함께 준비합니다', 7);
INSERT INTO format (id, name, description, sort_order) VALUES (8, '자유 형식', '형식에 구애받지 않고 자유롭게 학습합니다', 8);
