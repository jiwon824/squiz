-- ============================================================
-- 테스트 데이터 INSERT SQL
-- 사용법: MySQL 클라이언트에서 직접 실행
--   mysql -u root -p modustudy < seed_test_data.sql
-- 또는 DBeaver, MySQL Workbench 등에서 실행
-- ============================================================

-- ==================== 1. 테스트 유저 (5명) ====================
-- 비밀번호는 BCrypt('test1234') - 실제 로그인 테스트 시 사용
-- $2a$10$N4zHFYdmonKBOLsR5E4/Medxpyemqe2T8/9ZuJuLpn1eddzgCTBhW

INSERT INTO `user` (user_id, email, password, name, nickname, role, is_active, current_level, current_points, total_exp, leader_rating, leader_review_count, bio, interests, tech_stacks, created_at)
VALUES
('testuser1', 'test1@ssafy.com', '$2a$10$N4zHFYdmonKBOLsR5E4/Medxpyemqe2T8/9ZuJuLpn1eddzgCTBhW', '김싸피', 'ssafy_kim', 'USER', 1, 5, 1200, 3500, 4.5, 10, '알고리즘을 좋아하는 SSAFY 교육생입니다.', '["알고리즘", "백엔드"]', '["Java", "Spring Boot", "MySQL"]', NOW()),

('testuser2', 'test2@ssafy.com', '$2a$10$N4zHFYdmonKBOLsR5E4/Medxpyemqe2T8/9ZuJuLpn1eddzgCTBhW', '이프론트', 'front_lee', 'USER', 1, 3, 800, 2000, 4.2, 5, 'React와 TypeScript 개발자입니다.', '["프론트엔드", "UI/UX"]', '["React", "TypeScript", "Tailwind CSS"]', NOW()),

('testuser3', 'test3@ssafy.com', '$2a$10$N4zHFYdmonKBOLsR5E4/Medxpyemqe2T8/9ZuJuLpn1eddzgCTBhW', '박백엔드', 'backend_park', 'USER', 1, 4, 1000, 2800, 3.8, 3, 'Spring Boot와 JPA 전문가를 목표로 하고 있습니다.', '["백엔드", "데이터베이스"]', '["Java", "Spring Boot", "JPA", "Redis"]', NOW()),

('testuser4', 'test4@ssafy.com', '$2a$10$N4zHFYdmonKBOLsR5E4/Medxpyemqe2T8/9ZuJuLpn1eddzgCTBhW', '최데이터', 'data_choi', 'USER', 1, 2, 400, 1000, 0, 0, '데이터 분석과 머신러닝에 관심이 많습니다.', '["AI/머신러닝", "데이터"]', '["Python", "TensorFlow", "SQL"]', NOW()),

('testuser5', 'test5@ssafy.com', '$2a$10$N4zHFYdmonKBOLsR5E4/Medxpyemqe2T8/9ZuJuLpn1eddzgCTBhW', '정모바일', 'mobile_jung', 'USER', 1, 3, 600, 1500, 4.0, 2, 'Android 개발자입니다.', '["모바일", "인프라"]', '["Kotlin", "Jetpack Compose", "Docker"]', NOW());

-- 삽입된 유저 ID 확인용 (변수 저장)
SET @user1 = (SELECT id FROM `user` WHERE user_id = 'testuser1');
SET @user2 = (SELECT id FROM `user` WHERE user_id = 'testuser2');
SET @user3 = (SELECT id FROM `user` WHERE user_id = 'testuser3');
SET @user4 = (SELECT id FROM `user` WHERE user_id = 'testuser4');
SET @user5 = (SELECT id FROM `user` WHERE user_id = 'testuser5');


-- ==================== 2. 테스트 스터디 (8개) ====================
-- topic_id, format_id는 V1__init.sql seed 데이터 기준

-- 스터디 1: 모집중 / 알고리즘 / 온라인
INSERT INTO `study` (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user1, '백준 골드 달성 스터디', '함께 백준 골드를 정복해봅시다!', '매주 월/수/금 저녁 7시에 백준 골드 난이도 문제를 2~3문제씩 풀고 코드 리뷰를 진행합니다.\n\n진행 방식:\n1. 사전에 문제 공지\n2. 각자 풀어온 후 화상 미팅\n3. 코드 리뷰 및 최적화 토론', 11, 1, 'PLANNED', 'ONLINE', 'RECRUITING', 1, 6, 'INTERMEDIATE', '["MON","WED","FRI"]', '19:00:00', '2026-01-20', '2026-02-15', '2026-02-17', '2026-05-17', 12, '백준 골드 티어 달성 및 코딩테스트 역량 강화', '백준 온라인 저지', 'NORMAL', NOW(), NOW());

SET @study1 = LAST_INSERT_ID();

-- 스터디 2: 모집중 / React / 온라인
INSERT INTO `study` (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user2, 'React 심화 스터디', 'React 고급 패턴과 성능 최적화를 학습합니다', 'React의 고급 패턴(Render Props, HOC, Compound Components)과 성능 최적화 기법을 함께 학습합니다.\n\n매주 화/목 저녁 8시에 발표 + 실습 형태로 진행합니다.', 34, 6, 'PLANNED', 'ONLINE', 'RECRUITING', 1, 5, 'ADVANCED', '["TUE","THU"]', '20:00:00', '2026-01-25', '2026-02-20', '2026-02-24', '2026-04-24', 8, 'React 고급 패턴 마스터 및 포트폴리오 프로젝트 완성', 'React 공식 문서 + Epic React (Kent C. Dodds)', 'NORMAL', NOW(), NOW());

SET @study2 = LAST_INSERT_ID();

-- 스터디 3: 진행중 / Spring Boot / 오프라인
INSERT INTO `study` (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user3, 'Spring Boot 프로젝트 스터디', '실전 프로젝트로 Spring Boot 마스터하기', 'Spring Boot + JPA + QueryDSL을 활용한 실전 프로젝트를 함께 진행합니다.\n\n매주 토요일 오후 2시에 강남역 스터디카페에서 모여 코딩합니다.\n\n현재 REST API 설계 단계를 진행 중입니다.', 41, 4, 'PLANNED', 'OFFLINE', 'IN_PROGRESS', 1, 4, 'INTERMEDIATE', '["SAT"]', '14:00:00', '2026-01-01', '2026-01-15', '2026-01-20', '2026-04-20', 12, 'Spring Boot 기반 풀스택 프로젝트 완성', '스프링 부트와 AWS로 혼자 구현하는 웹 서비스', 'STRICT', NOW(), NOW());

SET @study3 = LAST_INSERT_ID();

-- 스터디 4: 모집중 / CS 기초 / 온라인
INSERT INTO `study` (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user1, 'CS 면접 대비 스터디', '취업 준비를 위한 CS 기초 완전 정복', '운영체제, 네트워크, 데이터베이스, 자료구조 핵심 개념을 매주 정리하고 모의 면접을 진행합니다.\n\n매주 일요일 오전 10시에 온라인으로 진행합니다.', 2, 5, 'PLANNED', 'ONLINE', 'RECRUITING', 1, 8, 'ELEMENTARY', '["SUN"]', '10:00:00', '2026-01-28', '2026-02-28', '2026-03-02', '2026-06-02', 12, 'CS 핵심 개념 정리 및 기술 면접 자신감 향상', '면접을 위한 CS 전공지식 노트', 'LENIENT', NOW(), NOW());

SET @study4 = LAST_INSERT_ID();

-- 스터디 5: 번개 스터디 / 모집중 / AI
INSERT INTO `study` (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, penalty_policy, created_at, updated_at)
VALUES (@user4, 'ChatGPT API 활용 해커톤', '주말 동안 ChatGPT API로 미니 프로젝트를!', '이번 주말(토~일) 동안 ChatGPT API를 활용한 미니 프로젝트를 함께 만들어봅시다.\n\n각자 아이디어를 가져와서 팀을 나누고, 2일 동안 집중 개발 후 발표합니다.', 7, 4, 'LIGHTNING', 'ONLINE', 'RECRUITING', 1, 10, 'BEGINNER', '["SAT","SUN"]', '09:00:00', '2026-01-30', '2026-02-07', '2026-02-08', '2026-02-09', 2, 'ChatGPT API 활용 미니 프로젝트 완성', 'NONE', NOW(), NOW());

SET @study5 = LAST_INSERT_ID();

-- 스터디 6: 완료 / 프로그래머스 / 온라인
INSERT INTO `study` (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user5, '프로그래머스 Lv2 정복', '프로그래머스 레벨2 문제를 모두 풀어봅시다', '프로그래머스 레벨2 문제를 매일 1문제씩 풀고, 주 2회 코드 리뷰를 진행한 스터디입니다.\n\n총 8주간 진행하여 성공적으로 완료했습니다!', 12, 1, 'PLANNED', 'ONLINE', 'COMPLETED', 1, 5, 'ELEMENTARY', '["MON","THU"]', '21:00:00', '2025-09-01', '2025-09-15', '2025-09-20', '2025-11-20', 8, '프로그래머스 Lv2 전 문제 풀이 완료', '프로그래머스', 'NORMAL', '2025-09-01 10:00:00', '2025-11-20 18:00:00');

SET @study6 = LAST_INSERT_ID();

-- 스터디 7: 모집중 / Docker/K8s / 하이브리드
INSERT INTO `study` (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user5, 'Docker & Kubernetes 입문', '컨테이너 기술의 기초부터 실전까지', 'Docker와 Kubernetes의 기초 개념부터 실전 배포까지 함께 학습합니다.\n\n격주 토요일 오프라인 모임 + 매주 온라인 진도 체크로 진행합니다.', 51, 3, 'PLANNED', 'HYBRID', 'RECRUITING', 1, 6, 'BEGINNER', '["WED","SAT"]', '15:00:00', '2026-01-25', '2026-02-25', '2026-03-01', '2026-05-01', 10, 'Docker Compose & K8s 기본 배포 가능 수준 달성', '쿠버네티스 인 액션', 'NORMAL', NOW(), NOW());

SET @study7 = LAST_INSERT_ID();

-- 스터디 8: 모집예정 / Flutter / 온라인
INSERT INTO `study` (leader_id, name, intro, description, topic_id, format_id, study_type, meeting_type, status, is_public, max_members, difficulty, schedule_days, schedule_time, recruit_start_date, recruit_end_date, start_date, end_date, total_sessions, goal, textbook, penalty_policy, created_at, updated_at)
VALUES (@user2, 'Flutter 크로스플랫폼 앱 개발', 'Flutter로 iOS/Android 앱을 동시에!', 'Flutter와 Dart를 활용한 크로스플랫폼 앱 개발을 함께 학습합니다.\n\n기초 위젯부터 상태 관리, Firebase 연동까지 단계적으로 진행합니다.', 63, 3, 'PLANNED', 'ONLINE', 'SCHEDULED', 1, 5, 'BEGINNER', '["TUE","FRI"]', '19:30:00', '2026-02-15', '2026-03-01', '2026-03-03', '2026-05-30', 12, 'Flutter 앱 1개 배포 완료', 'Flutter in Action', 'LENIENT', NOW(), NOW());

SET @study8 = LAST_INSERT_ID();


-- ==================== 3. 스터디 멤버 ====================

-- 스터디 1 (백준 골드) - 리더: user1, 멤버: user2, user3
INSERT INTO `study_member` (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study1, @user1, 'LEADER', 'APPROVED', NOW(), 0),
(@study1, @user2, 'MEMBER', 'APPROVED', NOW(), 0),
(@study1, @user3, 'MEMBER', 'APPROVED', NOW(), 1);

-- 스터디 2 (React 심화) - 리더: user2, 멤버: user1
INSERT INTO `study_member` (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study2, @user2, 'LEADER', 'APPROVED', NOW(), 0),
(@study2, @user1, 'MEMBER', 'APPROVED', NOW(), 0);

-- 스터디 3 (Spring Boot) - 리더: user3, 멤버: user1, user5
INSERT INTO `study_member` (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study3, @user3, 'LEADER', 'APPROVED', NOW(), 0),
(@study3, @user1, 'MEMBER', 'APPROVED', NOW(), 0),
(@study3, @user5, 'MEMBER', 'APPROVED', NOW(), 1);

-- 스터디 4 (CS 면접) - 리더: user1만
INSERT INTO `study_member` (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study4, @user1, 'LEADER', 'APPROVED', NOW(), 0);

-- 스터디 5 (ChatGPT 해커톤) - 리더: user4, 멤버: user2, user3
INSERT INTO `study_member` (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study5, @user4, 'LEADER', 'APPROVED', NOW(), 0),
(@study5, @user2, 'MEMBER', 'APPROVED', NOW(), 0),
(@study5, @user3, 'MEMBER', 'PENDING', NOW(), 1);

-- 스터디 6 (프로그래머스 - 완료) - 리더: user5, 멤버: user1, user4
INSERT INTO `study_member` (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study6, @user5, 'LEADER', 'APPROVED', '2025-09-15 10:00:00', 0),
(@study6, @user1, 'MEMBER', 'APPROVED', '2025-09-16 14:00:00', 0),
(@study6, @user4, 'MEMBER', 'APPROVED', '2025-09-17 09:00:00', 0);

-- 스터디 7 (Docker/K8s) - 리더: user5, 멤버: user3
INSERT INTO `study_member` (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study7, @user5, 'LEADER', 'APPROVED', NOW(), 0),
(@study7, @user3, 'MEMBER', 'APPROVED', NOW(), 1);

-- 스터디 8 (Flutter - 모집예정) - 리더: user2만
INSERT INTO `study_member` (study_id, user_id, role, status, joined_at, is_probation) VALUES
(@study8, @user2, 'LEADER', 'APPROVED', NOW(), 0);


-- ==================== 4. 테스트 미팅 (스터디3 - Spring Boot, IN_PROGRESS) ====================
-- 스터디3(Spring Boot 프로젝트 스터디)에 진행중 미팅 1개 생성
INSERT INTO `meeting` (study_id, title, started_at, planned_duration_seconds, participant_count, status, meeting_type, created_at, updated_at)
VALUES (@study3, '테스트 미팅 - UI 확인용', NOW(), 3600, 2, 'IN_PROGRESS', 'FREE', NOW(), NOW());

SET @meeting1 = LAST_INSERT_ID();

-- 미팅 참가자: user3(리더), user1(멤버)
INSERT INTO `meeting_participant` (meeting_id, user_id, joined_at, is_muted, is_camera_on, created_at) VALUES
(@meeting1, @user3, NOW(), 0, 1, NOW()),
(@meeting1, @user1, NOW(), 0, 0, NOW());

-- 테스트 채팅 메시지 몇 개 추가
INSERT INTO `meeting_chat_message` (meeting_id, user_id, sender_name, content, sent_at) VALUES
(@meeting1, @user3, '박백엔드', '안녕하세요! 미팅 시작합니다.', NOW()),
(@meeting1, @user1, '김싸피', '네, 준비됐습니다!', DATE_ADD(NOW(), INTERVAL 10 SECOND)),
(@meeting1, @user3, '박백엔드', '오늘은 REST API 설계를 리뷰하겠습니다.', DATE_ADD(NOW(), INTERVAL 20 SECOND)),
(@meeting1, @user1, '김싸피', '좋습니다. 화면 공유해주세요.', DATE_ADD(NOW(), INTERVAL 30 SECOND));


-- ==================== 확인 쿼리 ====================
SELECT '=== 테스트 유저 ===' AS info;
SELECT id, user_id, nickname, email, current_level, leader_rating FROM `user` WHERE user_id LIKE 'testuser%';

SELECT '=== 테스트 스터디 ===' AS info;
SELECT s.id, s.name, s.status, s.study_type, s.meeting_type, u.nickname AS leader
FROM study s JOIN `user` u ON s.leader_id = u.id
WHERE s.leader_id IN (@user1, @user2, @user3, @user4, @user5)
ORDER BY s.id;

SELECT '=== 스터디 멤버 ===' AS info;
SELECT sm.study_id, s.name AS study_name, u.nickname, sm.role, sm.status
FROM study_member sm
JOIN study s ON sm.study_id = s.id
JOIN `user` u ON sm.user_id = u.id
WHERE sm.study_id IN (@study1, @study2, @study3, @study4, @study5, @study6, @study7, @study8)
ORDER BY sm.study_id, sm.role DESC;

SELECT '=== 테스트 미팅 ===' AS info;
SELECT m.id AS meeting_id, m.study_id, m.title, m.status, m.meeting_type,
       CONCAT('https://localhost:3000/study/', m.study_id, '/meetings/', m.id, '/room') AS room_url
FROM meeting m WHERE m.id = @meeting1;
