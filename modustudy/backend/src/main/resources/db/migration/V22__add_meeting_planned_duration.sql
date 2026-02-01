-- =============================================
-- V10: Add planned duration for meetings
-- =============================================
ALTER TABLE `meeting`
ADD COLUMN `planned_duration_seconds` INT NULL;
