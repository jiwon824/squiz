-- =============================================
-- V4: Fix profile table missing columns
-- =============================================

-- user_id 컬럼 추가 (없으면)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profile' AND COLUMN_NAME = 'user_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `profile` ADD COLUMN `user_id` BIGINT UNIQUE AFTER `id`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- profile_image_url 컬럼 추가 (없으면)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profile' AND COLUMN_NAME = 'profile_image_url');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `profile` ADD COLUMN `profile_image_url` VARCHAR(500)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- profile_image_source 컬럼 추가 (없으면)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profile' AND COLUMN_NAME = 'profile_image_source');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `profile` ADD COLUMN `profile_image_source` ENUM(''KAKAO'', ''GOOGLE'', ''NAVER'', ''UPLOAD'') DEFAULT ''UPLOAD''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- bio 컬럼 추가 (없으면)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profile' AND COLUMN_NAME = 'bio');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `profile` ADD COLUMN `bio` TEXT',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- social_links 컬럼 추가 (없으면)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profile' AND COLUMN_NAME = 'social_links');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `profile` ADD COLUMN `social_links` JSON',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tech 컬럼 추가 (없으면)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profile' AND COLUMN_NAME = 'tech');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `profile` ADD COLUMN `tech` JSON',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- favorite 컬럼 추가 (없으면)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profile' AND COLUMN_NAME = 'favorite');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `profile` ADD COLUMN `favorite` JSON',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
