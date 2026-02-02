-- 스터디 모집 관리 기능을 위한 알림 타입 추가
-- STUDY_RECRUITMENT_COMPLETE: 모집 완료 알림
-- STUDY_EXTENSION: 스터디 연장 알림
-- STUDY_START: 스터디 시작 알림

ALTER TABLE notification
MODIFY COLUMN type enum(
    'CHAT',
    'SCHEDULE',
    'ATTENDANCE',
    'STUDY_UPDATE',
    'STUDY_APPLICATION',
    'STUDY_RECRUITMENT_COMPLETE',
    'STUDY_EXTENSION',
    'STUDY_START',
    'QUIZ',
    'SYSTEM',
    'FRIEND'
) NOT NULL;
