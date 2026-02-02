-- =============================================
-- ModuStudy Gamification Enhancement
-- Flyway Migration V6 - Add experience columns to user_stats
-- =============================================

-- 현재 레벨 경험치 (레벨업 시 리셋)
ALTER TABLE `user_stats`
ADD COLUMN IF NOT EXISTS `current_experience` INT DEFAULT 0 AFTER `level_name`;

-- 총 출석 횟수
ALTER TABLE `user_stats`
ADD COLUMN IF NOT EXISTS `total_attendance` INT DEFAULT 0 AFTER `total_studies_led`;

-- 오늘 채팅으로 얻은 경험치 (일일 제한용)
ALTER TABLE `user_stats`
ADD COLUMN IF NOT EXISTS `today_chat_exp` INT DEFAULT 0 AFTER `total_chat_count`;

-- 마지막 채팅 날짜 (일일 제한 리셋용)
ALTER TABLE `user_stats`
ADD COLUMN IF NOT EXISTS `last_chat_date` DATE DEFAULT NULL AFTER `today_chat_exp`;

-- total_experience 기본값 설정 (NULL인 경우)
UPDATE `user_stats` SET `total_experience` = 0 WHERE `total_experience` IS NULL;

-- total_experience NOT NULL로 변경
ALTER TABLE `user_stats`
MODIFY COLUMN `total_experience` INT NOT NULL DEFAULT 0;
