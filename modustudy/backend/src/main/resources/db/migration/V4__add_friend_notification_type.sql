-- =============================================
-- V4: notification 테이블에 FRIEND 타입 추가
-- =============================================

ALTER TABLE notification
MODIFY COLUMN type ENUM('CHAT', 'SCHEDULE', 'ATTENDANCE', 'STUDY_UPDATE', 'STUDY_APPLICATION', 'QUIZ', 'SYSTEM', 'FRIEND') NOT NULL;
