-- =============================================
-- 샘플 데이터 전체 삭제 (외래 키 역순)
-- user 10000~10009 관련 데이터만 삭제
-- =============================================

-- Safe update mode 비활성화
SET SQL_SAFE_UPDATES = 0;

-- 1. retrospective_item 삭제 (user 10000~10009가 작성한 것)
DELETE FROM retrospective_item WHERE user_id BETWEEN 10000 AND 10009;

-- 2. retrospective 삭제 (user 10000~10009가 작성한 것)
DELETE FROM retrospective WHERE created_by BETWEEN 10000 AND 10009;

-- 3. material 삭제 (user 10000~10009가 업로드한 것)
DELETE FROM material WHERE uploader_id BETWEEN 10000 AND 10009;

-- 4. curriculum 삭제 (user 10000~10009가 리더인 스터디)
DELETE FROM curriculum WHERE study_id IN (
    SELECT id FROM study WHERE leader_id BETWEEN 10000 AND 10009
);

-- 5. meeting 삭제 (user 10000~10009가 리더인 스터디)
DELETE FROM meeting WHERE study_id IN (
    SELECT id FROM study WHERE leader_id BETWEEN 10000 AND 10009
);

-- 6. attendance 삭제 (user 10000~10009의 출석 기록)
DELETE FROM attendance WHERE user_id BETWEEN 10000 AND 10009;

-- 7. study_session 삭제 (user 10000~10009가 리더인 스터디)
DELETE FROM study_session WHERE study_id IN (
    SELECT id FROM study WHERE leader_id BETWEEN 10000 AND 10009
);

-- 8. study_member 삭제 (user 10000~10009가 멤버인 것)
DELETE FROM study_member WHERE user_id BETWEEN 10000 AND 10009;

-- 9. study 삭제 (user 10000~10009가 리더인 것)
DELETE FROM study WHERE leader_id BETWEEN 10000 AND 10009;

-- 10. user_stats 삭제
DELETE FROM user_stats WHERE user_id BETWEEN 10000 AND 10009;

-- 11. user 삭제
DELETE FROM user WHERE id BETWEEN 10000 AND 10009;

-- Safe update mode 다시 활성화
SET SQL_SAFE_UPDATES = 1;

-- 확인
-- SELECT COUNT(*) FROM user WHERE id BETWEEN 10000 AND 10009;
