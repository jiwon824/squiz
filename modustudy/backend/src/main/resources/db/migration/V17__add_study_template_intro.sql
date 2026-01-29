-- study_template 테이블에 intro 컬럼 추가
ALTER TABLE `study_template` ADD COLUMN `intro` VARCHAR(200) NULL AFTER `name`;
