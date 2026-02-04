-- board_post 테이블에 모집글 관련 컬럼 추가
ALTER TABLE board_post
    ADD COLUMN recruitment_field VARCHAR(50) NULL AFTER category,
    ADD COLUMN meeting_type ENUM('ONLINE', 'OFFLINE', 'HYBRID') NULL,
    ADD COLUMN target_members INT NULL,
    ADD COLUMN recruitment_status ENUM('RECRUITING', 'COMPLETED') NULL;
