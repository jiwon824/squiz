-- ============================================================
-- V20: study 테이블의 topic/format VARCHAR → topic_id/format_id BIGINT 마이그레이션
-- 목적: 정규화된 테이블 구조로 변경 (JPA 엔티티와 일치)
-- ============================================================

-- 1. study 테이블에 topic_id, format_id 컬럼 추가 (임시로 nullable)
ALTER TABLE `study` ADD COLUMN `topic_id` BIGINT NULL AFTER `description`;
ALTER TABLE `study` ADD COLUMN `format_id` BIGINT NULL AFTER `topic_id`;

-- 2. 기존 데이터 마이그레이션 (VARCHAR → ID 매핑)
-- 주의: 기존 데이터가 없으면 이 단계는 스킵됨

-- topic 매핑 (정확히 일치하는 소분류 또는 대분류 찾기)
-- COLLATE 명시로 utf8mb4_0900_ai_ci와 utf8mb4_unicode_ci 충돌 해결
UPDATE `study` s
INNER JOIN `topic` t ON s.topic COLLATE utf8mb4_unicode_ci = t.name
SET s.topic_id = t.id
WHERE s.topic_id IS NULL;

-- format 매핑
UPDATE `study` s
INNER JOIN `format` f ON s.format COLLATE utf8mb4_unicode_ci = f.name
SET s.format_id = f.id
WHERE s.format_id IS NULL AND s.format IS NOT NULL;

-- 3. 매핑되지 않은 데이터 처리 (기본값 설정)
-- topic이 매핑되지 않은 경우 "알고리즘/코딩테스트" > "백준"으로 기본 설정
UPDATE `study` s
SET s.topic_id = (
    SELECT id FROM `topic`
    WHERE name COLLATE utf8mb4_unicode_ci = '백준'
    AND parent_id = (SELECT id FROM `topic` WHERE name COLLATE utf8mb4_unicode_ci = '알고리즘/코딩테스트' AND parent_id IS NULL LIMIT 1)
    LIMIT 1
)
WHERE s.topic_id IS NULL;

-- format이 매핑되지 않은 경우 "문제 풀이"로 기본 설정
UPDATE `study` s
SET s.format_id = (SELECT id FROM `format` WHERE name COLLATE utf8mb4_unicode_ci = '문제 풀이' LIMIT 1)
WHERE s.format_id IS NULL AND s.format IS NOT NULL;

-- 4. topic_id를 NOT NULL로 변경 (필수 컬럼)
ALTER TABLE `study` MODIFY COLUMN `topic_id` BIGINT NOT NULL;

-- 5. 외래 키 제약조건 추가
ALTER TABLE `study`
ADD CONSTRAINT `fk_study_topic`
FOREIGN KEY (`topic_id`) REFERENCES `topic`(`id`) ON DELETE RESTRICT;

ALTER TABLE `study`
ADD CONSTRAINT `fk_study_format`
FOREIGN KEY (`format_id`) REFERENCES `format`(`id`) ON DELETE SET NULL;

-- 6. 인덱스 추가 (성능 최적화)
CREATE INDEX `idx_study_topic` ON `study`(`topic_id`);
CREATE INDEX `idx_study_format` ON `study`(`format_id`);

-- 7. 기존 topic, format VARCHAR 컬럼 삭제
ALTER TABLE `study` DROP COLUMN `topic`;
ALTER TABLE `study` DROP COLUMN `format`;

-- 8. study_template 테이블도 동일하게 마이그레이션
-- topic_id, format_id 컬럼 추가
ALTER TABLE `study_template` ADD COLUMN `topic_id` BIGINT NULL AFTER `template_type`;
ALTER TABLE `study_template` ADD COLUMN `format_id` BIGINT NULL AFTER `topic_id`;

-- 기존 데이터 매핑 (COLLATE 명시)
UPDATE `study_template` st
INNER JOIN `topic` t ON st.topic COLLATE utf8mb4_unicode_ci = t.name
SET st.topic_id = t.id
WHERE st.topic_id IS NULL AND st.topic IS NOT NULL;

UPDATE `study_template` st
INNER JOIN `format` f ON st.format COLLATE utf8mb4_unicode_ci = f.name
SET st.format_id = f.id
WHERE st.format_id IS NULL AND st.format IS NOT NULL;

-- 외래 키 제약조건 추가 (NULL 허용)
ALTER TABLE `study_template`
ADD CONSTRAINT `fk_study_template_topic`
FOREIGN KEY (`topic_id`) REFERENCES `topic`(`id`) ON DELETE SET NULL;

ALTER TABLE `study_template`
ADD CONSTRAINT `fk_study_template_format`
FOREIGN KEY (`format_id`) REFERENCES `format`(`id`) ON DELETE SET NULL;

-- 인덱스 추가
CREATE INDEX `idx_study_template_topic` ON `study_template`(`topic_id`);
CREATE INDEX `idx_study_template_format` ON `study_template`(`format_id`);

-- 기존 topic, format VARCHAR 컬럼 삭제
ALTER TABLE `study_template` DROP COLUMN `topic`;
ALTER TABLE `study_template` DROP COLUMN `format`;
