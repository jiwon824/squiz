-- V3: 초기 데이터 삭제 (DataInitializer에서 관리하도록 변경)
-- V1에서 삽입된 topic, format 데이터를 삭제하여 DataInitializer가 새로 삽입하도록 함

SET FOREIGN_KEY_CHECKS = 0;

-- Format 데이터 삭제 (study 테이블에서 참조하지 않는 경우만)
DELETE FROM format WHERE id <= 8;

-- Topic 데이터 삭제 (자식 먼저, 부모 나중에)
DELETE FROM topic WHERE parent_id IS NOT NULL AND id <= 103;
DELETE FROM topic WHERE parent_id IS NULL AND id <= 10;

SET FOREIGN_KEY_CHECKS = 1;
