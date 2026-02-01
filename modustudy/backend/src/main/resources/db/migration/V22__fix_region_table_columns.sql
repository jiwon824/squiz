-- =============================================
-- V22: Region 테이블에 누락된 컬럼 추가
-- Entity와 DB 스키마 동기화
-- =============================================

-- full_name 컬럼 추가
ALTER TABLE `region` ADD COLUMN `full_name` VARCHAR(100) AFTER `name`;

-- level 컬럼 추가 (기본값 1)
ALTER TABLE `region` ADD COLUMN `level` INT NOT NULL DEFAULT 1 AFTER `full_name`;

-- parent_id 컬럼 추가 (자기 참조 FK)
ALTER TABLE `region` ADD COLUMN `parent_id` BIGINT AFTER `level`;

-- 외래 키 제약조건 추가
ALTER TABLE `region` ADD CONSTRAINT `fk_region_parent`
FOREIGN KEY (`parent_id`) REFERENCES `region`(`id`) ON DELETE SET NULL;

-- 인덱스 추가
CREATE INDEX `idx_region_parent` ON `region`(`parent_id`);
CREATE INDEX `idx_region_level` ON `region`(`level`);
