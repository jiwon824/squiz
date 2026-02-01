-- Add planned_duration_seconds column to meeting table
ALTER TABLE `meeting`
  ADD COLUMN `planned_duration_seconds` INT NULL AFTER `duration_seconds`;
