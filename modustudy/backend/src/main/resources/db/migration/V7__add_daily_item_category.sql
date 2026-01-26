-- =============================================
-- V7: Add category column to daily_item table
-- =============================================
-- JPA entity has category field but production DB is missing it

-- Add category column with default value for existing rows
ALTER TABLE `daily_item`
ADD COLUMN `category` ENUM('YESTERDAY', 'TODAY', 'BLOCKER') NOT NULL DEFAULT 'TODAY';
