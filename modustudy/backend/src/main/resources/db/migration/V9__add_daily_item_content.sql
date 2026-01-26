-- =============================================
-- V9: Add content column to daily_item table
-- =============================================
-- Fix: production DB missing content column

-- Add content column (TEXT cannot have default value in MySQL)
-- First add as nullable, then update existing rows, then make NOT NULL
ALTER TABLE `daily_item` ADD COLUMN `content` TEXT NULL;

-- Update any existing rows with empty content
UPDATE `daily_item` SET `content` = '' WHERE `content` IS NULL;

-- Make column NOT NULL
ALTER TABLE `daily_item` MODIFY COLUMN `content` TEXT NOT NULL;
