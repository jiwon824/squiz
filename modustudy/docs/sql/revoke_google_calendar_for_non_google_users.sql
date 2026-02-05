-- =====================================================
-- 카카오/네이버 가입 사용자의 구글 캘린더 연동 해제
-- 구글 로그인 사용자만 구글 캘린더 연동 가능하도록 정책 적용
-- =====================================================

SET SQL_SAFE_UPDATES = 0;

-- 1. 먼저 대상 확인 (실행 전 미리보기)
-- 주 계정이 KAKAO/NAVER인 사용자 중 GOOGLE 연동이 있고 캘린더 토큰이 있는 경우
SELECT
    u.id AS user_id,
    u.email,
    u.nickname,
    primary_acc.provider AS primary_provider,
    google_acc.id AS google_account_id,
    google_acc.email AS google_email,
    CASE WHEN google_acc.refresh_token IS NOT NULL THEN 'YES' ELSE 'NO' END AS has_calendar_token
FROM user u
JOIN user_social_account primary_acc ON u.id = primary_acc.user_id AND primary_acc.is_primary = 1
LEFT JOIN user_social_account google_acc ON u.id = google_acc.user_id AND google_acc.provider = 'GOOGLE'
WHERE primary_acc.provider IN ('KAKAO', 'NAVER')
AND google_acc.refresh_token IS NOT NULL;

-- 2. calendar_watch 테이블에서 해당 사용자 데이터 삭제
DELETE FROM calendar_watch
WHERE user_id IN (
    SELECT DISTINCT u.id
    FROM user u
    JOIN user_social_account primary_acc ON u.id = primary_acc.user_id AND primary_acc.is_primary = 1
    WHERE primary_acc.provider IN ('KAKAO', 'NAVER')
);

-- 3. study_session_calendar_mapping 테이블에서 해당 사용자 데이터 삭제
DELETE FROM study_session_calendar_mapping
WHERE user_id IN (
    SELECT DISTINCT u.id
    FROM user u
    JOIN user_social_account primary_acc ON u.id = primary_acc.user_id AND primary_acc.is_primary = 1
    WHERE primary_acc.provider IN ('KAKAO', 'NAVER')
);

-- 4. GOOGLE 계정의 캘린더 토큰 해제 (주 계정이 KAKAO/NAVER인 경우)
UPDATE user_social_account
SET
    access_token = NULL,
    refresh_token = NULL,
    token_expires_at = NULL,
    calendar_id = 'primary'
WHERE provider = 'GOOGLE'
AND user_id IN (
    SELECT user_id
    FROM (
        SELECT DISTINCT user_id
        FROM user_social_account
        WHERE is_primary = 1 AND provider IN ('KAKAO', 'NAVER')
    ) AS subquery
);

-- 5. 결과 확인
SELECT
    COUNT(*) AS affected_users,
    'Calendar tokens revoked for non-Google primary users' AS message
FROM user_social_account
WHERE provider = 'GOOGLE'
AND refresh_token IS NULL
AND user_id IN (
    SELECT DISTINCT user_id
    FROM user_social_account
    WHERE is_primary = 1 AND provider IN ('KAKAO', 'NAVER')
);

SET SQL_SAFE_UPDATES = 1;
