-- =============================================
-- ModuStudy Gamification Enhancement
-- Flyway Migration V7 - Update activity_type enum in contribution_detail
-- =============================================

-- activity_type ENUM에 새로운 활동 유형 추가
-- 기존: STUDY_ATTENDANCE, QUIZ_CONTEST
-- 추가: QUIZ_SOLVED, STUDY_JOIN, STUDY_CREATE, MATERIAL_UPLOAD, RETROSPECTIVE, CHAT_MESSAGE, FIRST_FRIEND_CHAT
ALTER TABLE `contribution_detail`
MODIFY COLUMN `activity_type` ENUM(
    'STUDY_ATTENDANCE',
    'QUIZ_CONTEST',
    'QUIZ_SOLVED',
    'STUDY_JOIN',
    'STUDY_CREATE',
    'MATERIAL_UPLOAD',
    'RETROSPECTIVE',
    'CHAT_MESSAGE',
    'FIRST_FRIEND_CHAT'
) NOT NULL;
