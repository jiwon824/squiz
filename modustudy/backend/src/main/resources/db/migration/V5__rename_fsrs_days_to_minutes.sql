-- Rename elapsed_days to elapsed_minutes
ALTER TABLE `user_review_items` CHANGE COLUMN `elapsed_days` `elapsed_minutes` INT DEFAULT 0;

-- Rename scheduled_days to scheduled_minutes
ALTER TABLE `user_review_items` CHANGE COLUMN `scheduled_days` `scheduled_minutes` INT DEFAULT 0;

-- Rename last_elapsed_days to last_elapsed_minutes
ALTER TABLE `user_review_items` CHANGE COLUMN `last_elapsed_days` `last_elapsed_minutes` INT DEFAULT 0;
