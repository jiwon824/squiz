SET @user1 = (SELECT id FROM user WHERE user_id = 'testuser1');
SET @user2 = (SELECT id FROM user WHERE user_id = 'testuser2');
SET @user3 = (SELECT id FROM user WHERE user_id = 'testuser3');
SET @user4 = (SELECT id FROM user WHERE user_id = 'testuser4');
SET @user5 = (SELECT id FROM user WHERE user_id = 'testuser5');

INSERT INTO study (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user1, '백준 골드 달성 스터디', '함께 백준 골드를 정복해봅시다!', '매주 월수금 저녁 7시에 백준 골드 난이도 문제를 풀고 코드 리뷰를 진행합니다.', 105, 9, 'PLANNED', 'ONLINE', 'RECRUITING', 1, 6, 'INTERMEDIATE', '["MON","WED","FRI"]', '19:00:00', '2026-01-20', '2026-02-15', '2026-02-17', '2026-05-17', 12, '백준 골드 티어 달성', '백준 온라인 저지', 'NORMAL', NOW(), NOW());
SET @study1 = LAST_INSERT_ID();

INSERT INTO study (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user2, 'React 심화 스터디', 'React 고급 패턴과 성능 최적화를 학습합니다', 'React의 고급 패턴과 성능 최적화 기법을 함께 학습합니다.', 123, 11, 'PLANNED', 'ONLINE', 'RECRUITING', 1, 5, 'ADVANCED', '["TUE","THU"]', '20:00:00', '2026-01-25', '2026-02-20', '2026-02-24', '2026-04-24', 8, 'React 고급 패턴 마스터', 'Epic React', 'NORMAL', NOW(), NOW());
SET @study2 = LAST_INSERT_ID();

INSERT INTO study (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user3, 'Spring Boot 프로젝트 스터디', '실전 프로젝트로 Spring Boot 마스터하기', 'Spring Boot + JPA + QueryDSL을 활용한 실전 프로젝트를 함께 진행합니다.', 128, 12, 'PLANNED', 'OFFLINE', 'IN_PROGRESS', 1, 4, 'INTERMEDIATE', '["SAT"]', '14:00:00', '2026-01-01', '2026-01-15', '2026-01-20', '2026-04-20', 12, 'Spring Boot 풀스택 프로젝트 완성', '스프링 부트와 AWS로 혼자 구현하는 웹 서비스', 'STRICT', NOW(), NOW());
SET @study3 = LAST_INSERT_ID();

INSERT INTO study (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user1, 'CS 면접 대비 스터디', '취업 준비를 위한 CS 기초 완전 정복', 'OS, 네트워크, DB, 자료구조 핵심 개념 정리 및 모의 면접을 진행합니다.', 110, 13, 'PLANNED', 'ONLINE', 'RECRUITING', 1, 8, 'ELEMENTARY', '["SUN"]', '10:00:00', '2026-01-28', '2026-02-28', '2026-03-02', '2026-06-02', 12, 'CS 핵심 개념 정리 및 기술 면접 준비', '면접을 위한 CS 전공지식 노트', 'LENIENT', NOW(), NOW());
SET @study4 = LAST_INSERT_ID();

INSERT INTO study (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, penalty_policy, created_at, updated_at)
VALUES (@user4, 'ChatGPT API 활용 해커톤', '주말 동안 ChatGPT API로 미니 프로젝트를!', '주말 동안 ChatGPT API를 활용한 미니 프로젝트를 함께 만들어봅시다.', 143, 12, 'LIGHTNING', 'ONLINE', 'RECRUITING', 1, 10, 'BEGINNER', '["SAT","SUN"]', '09:00:00', '2026-01-30', '2026-02-07', '2026-02-08', '2026-02-09', 2, 'ChatGPT API 미니 프로젝트 완성', 'NONE', NOW(), NOW());
SET @study5 = LAST_INSERT_ID();

INSERT INTO study (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user5, '프로그래머스 Lv2 정복', '프로그래머스 레벨2 문제를 모두 풀어봅시다', '프로그래머스 레벨2 문제를 매일 1문제씩 풀고 주 2회 코드 리뷰를 진행합니다.', 106, 9, 'PLANNED', 'ONLINE', 'COMPLETED', 1, 5, 'ELEMENTARY', '["MON","THU"]', '21:00:00', '2025-09-01', '2025-09-15', '2025-09-20', '2025-11-20', 8, '프로그래머스 Lv2 전 문제 풀이 완료', '프로그래머스', 'NORMAL', '2025-09-01 10:00:00', '2025-11-20 18:00:00');
SET @study6 = LAST_INSERT_ID();

INSERT INTO study (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user5, 'Docker & Kubernetes 입문', '컨테이너 기술의 기초부터 실전까지', 'Docker와 Kubernetes의 기초부터 실전 배포까지 함께 학습합니다.', 136, 11, 'PLANNED', 'HYBRID', 'RECRUITING', 1, 6, 'BEGINNER', '["WED","SAT"]', '15:00:00', '2026-01-25', '2026-02-25', '2026-03-01', '2026-05-01', 10, 'Docker Compose & K8s 기본 배포', '쿠버네티스 인 액션', 'NORMAL', NOW(), NOW());
SET @study7 = LAST_INSERT_ID();

INSERT INTO study (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user2, 'Flutter 크로스플랫폼 앱 개발', 'Flutter로 iOS/Android 앱을 동시에!', 'Flutter와 Dart를 활용한 크로스플랫폼 앱 개발을 학습합니다.', 154, 11, 'PLANNED', 'ONLINE', 'SCHEDULED', 1, 5, 'BEGINNER', '["TUE","FRI"]', '19:30:00', '2026-02-15', '2026-03-01', '2026-03-03', '2026-05-30', 12, 'Flutter 앱 1개 배포 완료', 'Flutter in Action', 'LENIENT', NOW(), NOW());
SET @study8 = LAST_INSERT_ID();

INSERT INTO study_member (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study1, @user1, 'LEADER', 'APPROVED', NOW(), 0),
(@study1, @user2, 'MEMBER', 'APPROVED', NOW(), 0),
(@study1, @user3, 'MEMBER', 'APPROVED', NOW(), 1);

INSERT INTO study_member (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study2, @user2, 'LEADER', 'APPROVED', NOW(), 0),
(@study2, @user1, 'MEMBER', 'APPROVED', NOW(), 0);

INSERT INTO study_member (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study3, @user3, 'LEADER', 'APPROVED', NOW(), 0),
(@study3, @user1, 'MEMBER', 'APPROVED', NOW(), 0),
(@study3, @user5, 'MEMBER', 'APPROVED', NOW(), 1);

INSERT INTO study_member (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study4, @user1, 'LEADER', 'APPROVED', NOW(), 0);

INSERT INTO study_member (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study5, @user4, 'LEADER', 'APPROVED', NOW(), 0),
(@study5, @user2, 'MEMBER', 'APPROVED', NOW(), 0),
(@study5, @user3, 'MEMBER', 'PENDING', NOW(), 1);

INSERT INTO study_member (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study6, @user5, 'LEADER', 'APPROVED', '2025-09-15 10:00:00', 0),
(@study6, @user1, 'MEMBER', 'APPROVED', '2025-09-16 14:00:00', 0),
(@study6, @user4, 'MEMBER', 'APPROVED', '2025-09-17 09:00:00', 0);

INSERT INTO study_member (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study7, @user5, 'LEADER', 'APPROVED', NOW(), 0),
(@study7, @user3, 'MEMBER', 'APPROVED', NOW(), 1);

INSERT INTO study_member (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study8, @user2, 'LEADER', 'APPROVED', NOW(), 0);
