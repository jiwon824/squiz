-- =============================================
-- V6: 리뷰 테스트용 완료된 스터디 더미 데이터
-- =============================================

-- 1. 테스트용 스터디장 (리뷰를 받을 사람)
INSERT INTO `user` (
    `id`, `user_id`, `email`, `name`, `nickname`, `role`, `is_active`,
    `is_online`, `is_searchable`, `total_exp`, `current_points`, `current_level`, `level_name`,
    `leader_rating`, `leader_review_count`
)
VALUES (
    9901, 'test_leader', 'test_leader@test.com', '테스트리더', '테스트리더', 'USER', TRUE,
    FALSE, TRUE, 0, 0, 1, 'Bronze',
    0.0, 0
)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 2. 테스트용 멤버 (리뷰를 작성할 사람) - 실제 로그인한 유저가 이 역할
-- 기존 유저가 있으면 그 유저를 사용하면 됨

-- 3. 완료된 스터디 생성
INSERT INTO `study` (
    `id`, `leader_id`, `name`, `description`, `topic_id`, `format_id`,
    `study_type`, `meeting_type`, `max_members`, `is_public`, `status`,
    `start_date`, `end_date`, `total_sessions`, `difficulty`, `goal`
) VALUES (
    9901, 9901, '[테스트] 완료된 알고리즘 스터디',
    '리뷰 테스트를 위한 완료된 스터디입니다.',
    1, 1,  -- 알고리즘/코딩테스트, 문제 풀이
    'PLANNED', 'ONLINE', 5, TRUE, 'COMPLETED',
    DATE_SUB(CURDATE(), INTERVAL 8 WEEK),
    DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    8, 'INTERMEDIATE', '알고리즘 실력 향상'
)
ON DUPLICATE KEY UPDATE `status` = 'COMPLETED';

-- 4. 스터디장을 멤버로 추가 (LEADER 역할)
INSERT INTO `study_member` (`study_id`, `user_id`, `role`, `status`, `joined_at`, `is_probation`)
VALUES (9901, 9901, 'LEADER', 'APPROVED', DATE_SUB(CURDATE(), INTERVAL 8 WEEK), FALSE)
ON DUPLICATE KEY UPDATE `role` = 'LEADER';

-- 5. 기존 유저들을 멤버로 추가
-- 무냐2 (id: 3) - 리뷰 작성 가능
INSERT INTO `study_member` (`study_id`, `user_id`, `role`, `status`, `joined_at`, `is_probation`)
VALUES (9901, 3, 'MEMBER', 'APPROVED', DATE_SUB(CURDATE(), INTERVAL 7 WEEK), FALSE)
ON DUPLICATE KEY UPDATE `status` = 'APPROVED';

-- 무냐 (id: 2) - 리뷰 작성 가능
INSERT INTO `study_member` (`study_id`, `user_id`, `role`, `status`, `joined_at`, `is_probation`)
VALUES (9901, 2, 'MEMBER', 'APPROVED', DATE_SUB(CURDATE(), INTERVAL 7 WEEK), FALSE)
ON DUPLICATE KEY UPDATE `status` = 'APPROVED';

-- 추가 유저 (id: 4) - 리뷰 작성 가능
INSERT INTO `study_member` (`study_id`, `user_id`, `role`, `status`, `joined_at`, `is_probation`)
VALUES (9901, 4, 'MEMBER', 'APPROVED', DATE_SUB(CURDATE(), INTERVAL 7 WEEK), FALSE)
ON DUPLICATE KEY UPDATE `status` = 'APPROVED';

-- 6. 워크스페이스 생성 (스터디에 필요)
INSERT INTO `workspace` (`study_id`)
VALUES (9901)
ON DUPLICATE KEY UPDATE `study_id` = `study_id`;
