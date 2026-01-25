-- =============================================
-- V2: Add updated_at column to attendance table
-- =============================================

-- MySQL에서는 컬럼이 없으면 추가하는 조건문 사용
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'attendance'
    AND COLUMN_NAME = 'updated_at'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE `attendance` ADD COLUMN `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
