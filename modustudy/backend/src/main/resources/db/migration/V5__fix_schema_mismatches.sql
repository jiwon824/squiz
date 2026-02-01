-- V5: ERD와 엔티티 정합성 수정

-- user_review_items.content_type을 ENUM으로 변경
ALTER TABLE user_review_items
MODIFY COLUMN content_type ENUM('COURSE_QUESTION', 'STUDY_QUESTION') NOT NULL;
