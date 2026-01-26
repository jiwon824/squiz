-- =============================================
-- V9: Add content column to daily_item table
-- =============================================
-- Fix: production DB missing content column

-- Check if column exists before adding (MySQL doesn't support IF NOT EXISTS for columns)
-- This will fail silently if column already exists
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE()
               AND table_name = 'daily_item'
               AND column_name = 'content');

SET @query := IF(@exist = 0,
    'ALTER TABLE `daily_item` ADD COLUMN `content` TEXT NOT NULL DEFAULT ""',
    'SELECT "content column already exists"');

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
