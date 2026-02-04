-- 신고 알림 타입 추가
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
    'REPORT',
    'SYSTEM',
    'FRIEND'
) NOT NULL;
