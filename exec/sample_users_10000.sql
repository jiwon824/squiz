-- =============================================
-- 샘플 사용자 데이터 (ID: 10000 ~ 10009)
-- 비밀번호 : password123
-- =============================================

INSERT INTO user (
    id, user_id, email, password, name, nickname, role,
    is_active, is_online, is_searchable, bio,
    interests, tech_stacks, available_days, preferred_time_slots,
    preferred_duration_weeks, leader_rating, leader_review_count,
    current_level, current_points, level_name, total_exp,
    created_at, updated_at
) VALUES
(10000, 'user10000', 'kim.dev@example.com', '$2a$10$S1nd731hsaLaS3RtFf1NV.Tqw6nTdthq8aA20uD2sNHIbqM0bKPMy', '김개발', '코딩마스터', 'USER',
 1, 0, 1, '풀스택 개발자를 꿈꾸는 백엔드 개발자입니다.',
 '["알고리즘", "백엔드", "클라우드"]', '["Java", "Spring Boot", "MySQL", "Docker"]', '["SAT", "SUN"]', '["14:00-18:00", "20:00-22:00"]',
 8, 4.5, 12, 15, 2500, '시니어 개발자', 15000,
 '2025-01-15 10:00:00', '2025-11-30 18:00:00'),

(10001, 'user10001', 'lee.algo@example.com', '$2a$10$S1nd731hsaLaS3RtFf1NV.Tqw6nTdthq8aA20uD2sNHIbqM0bKPMy', '이알고', '알고리즘왕', 'USER',
 1, 0, 1, '알고리즘 문제 풀이를 좋아하는 취준생입니다.',
 '["알고리즘", "코딩테스트", "자료구조"]', '["Python", "C++", "Java"]', '["MON", "WED", "FRI"]', '["19:00-22:00"]',
 4, 4.8, 8, 12, 1800, '중급 개발자', 12000,
 '2025-02-10 14:00:00', '2025-11-28 20:00:00'),

(10002, 'user10002', 'park.front@example.com', '$2a$10$S1nd731hsaLaS3RtFf1NV.Tqw6nTdthq8aA20uD2sNHIbqM0bKPMy', '박프론트', '리액트러버', 'USER',
 1, 0, 1, 'React와 TypeScript를 사랑하는 프론트엔드 개발자',
 '["프론트엔드", "UI/UX", "웹개발"]', '["React", "TypeScript", "Next.js", "TailwindCSS"]', '["TUE", "THU", "SAT"]', '["20:00-23:00"]',
 6, 4.2, 5, 10, 1500, '중급 개발자', 10000,
 '2025-03-05 09:00:00', '2025-11-25 21:00:00'),

(10003, 'user10003', 'choi.devops@example.com', '$2a$10$S1nd731hsaLaS3RtFf1NV.Tqw6nTdthq8aA20uD2sNHIbqM0bKPMy', '최데옵스', 'K8s마스터', 'USER',
 1, 0, 1, 'DevOps 엔지니어, 인프라 자동화에 관심이 많습니다.',
 '["DevOps", "클라우드", "인프라"]', '["Docker", "Kubernetes", "AWS", "Terraform"]', '["WED", "SAT", "SUN"]', '["10:00-12:00", "20:00-22:00"]',
 8, 4.6, 10, 18, 3200, '시니어 개발자', 18000,
 '2025-01-20 11:00:00', '2025-11-20 15:00:00'),

(10004, 'user10004', 'jung.mobile@example.com', '$2a$10$S1nd731hsaLaS3RtFf1NV.Tqw6nTdthq8aA20uD2sNHIbqM0bKPMy', '정모바일', '앱개발자', 'USER',
 1, 0, 1, 'Android/iOS 네이티브 앱 개발자입니다.',
 '["모바일", "Android", "iOS"]', '["Kotlin", "Swift", "Flutter", "Jetpack Compose"]', '["MON", "WED", "FRI"]', '["21:00-23:00"]',
 4, 4.3, 7, 13, 2100, '중급 개발자', 13000,
 '2025-02-28 10:00:00', '2025-11-22 22:00:00'),

(10005, 'user10005', 'kang.data@example.com', '$2a$10$S1nd731hsaLaS3RtFf1NV.Tqw6nTdthq8aA20uD2sNHIbqM0bKPMy', '강데이터', '데이터분석가', 'USER',
 1, 0, 1, '데이터로 인사이트를 찾는 데이터 분석가',
 '["데이터분석", "머신러닝", "통계"]', '["Python", "Pandas", "SQL", "Tableau"]', '["TUE", "THU"]', '["19:00-21:00"]',
 6, 4.7, 9, 14, 2300, '시니어 개발자', 14000,
 '2025-03-10 14:00:00', '2025-11-18 20:00:00'),

(10006, 'user10006', 'yoon.backend@example.com', '$2a$10$S1nd731hsaLaS3RtFf1NV.Tqw6nTdthq8aA20uD2sNHIbqM0bKPMy', '윤백엔드', 'API마스터', 'USER',
 1, 0, 1, 'Node.js 백엔드 개발자, API 설계를 좋아합니다.',
 '["백엔드", "API", "데이터베이스"]', '["Node.js", "Express", "MongoDB", "PostgreSQL"]', '["SAT", "SUN"]', '["13:00-17:00"]',
 8, 4.1, 6, 11, 1600, '중급 개발자', 11000,
 '2025-04-01 09:00:00', '2025-11-15 16:00:00'),

(10007, 'user10007', 'han.security@example.com', '$2a$10$S1nd731hsaLaS3RtFf1NV.Tqw6nTdthq8aA20uD2sNHIbqM0bKPMy', '한보안', '시큐리티', 'USER',
 1, 0, 1, '웹 보안 전문가를 목표로 공부 중입니다.',
 '["보안", "웹해킹", "네트워크"]', '["Python", "Burp Suite", "Wireshark", "Linux"]', '["FRI", "SAT"]', '["20:00-23:00"]',
 4, 4.4, 4, 9, 1200, '주니어 개발자', 9000,
 '2025-04-15 15:00:00', '2025-11-10 21:00:00'),

(10008, 'user10008', 'shin.ai@example.com', '$2a$10$S1nd731hsaLaS3RtFf1NV.Tqw6nTdthq8aA20uD2sNHIbqM0bKPMy', '신에이아이', 'ML엔지니어', 'USER',
 1, 0, 1, '머신러닝/딥러닝 연구에 관심이 많은 대학원생',
 '["AI", "딥러닝", "논문리뷰"]', '["Python", "PyTorch", "TensorFlow", "NumPy"]', '["MON", "THU", "SAT"]', '["16:00-19:00"]',
 8, 4.9, 11, 16, 2800, '시니어 개발자', 16000,
 '2025-02-05 10:00:00', '2025-11-08 18:00:00'),

(10009, 'user10009', 'oh.fullstack@example.com', '$2a$10$S1nd731hsaLaS3RtFf1NV.Tqw6nTdthq8aA20uD2sNHIbqM0bKPMy', '오풀스택', '만능개발자', 'USER',
 1, 0, 1, '프론트부터 백엔드, 인프라까지 다루는 풀스택 개발자',
 '["풀스택", "웹개발", "클라우드"]', '["React", "Node.js", "AWS", "Docker", "PostgreSQL"]', '["WED", "SAT", "SUN"]', '["10:00-12:00", "14:00-18:00"]',
 12, 4.5, 15, 20, 4000, '리드 개발자', 20000,
 '2025-01-05 08:00:00', '2025-11-05 17:00:00');


-- =============================================
-- user_stats 테이블 (필수 연관 데이터)
-- =============================================
INSERT INTO user_stats (
    user_id, level, level_name, total_activity_days, current_streak, max_streak,
    last_activity_date, total_studies_joined, total_studies_led, total_chat_count,
    total_quiz_count, total_materials_uploaded, total_retrospectives, total_experience
) VALUES
(10000, 15, '시니어 개발자', 120, 5, 30, '2025-11-30', 8, 3, 450, 200, 15, 12, 15000),
(10001, 12, '중급 개발자', 95, 3, 21, '2025-11-28', 6, 2, 380, 350, 8, 8, 12000),
(10002, 10, '중급 개발자', 80, 7, 14, '2025-11-25', 5, 1, 290, 120, 12, 6, 10000),
(10003, 18, '시니어 개발자', 150, 10, 45, '2025-11-20', 10, 4, 520, 180, 20, 15, 18000),
(10004, 13, '중급 개발자', 100, 4, 18, '2025-11-22', 7, 2, 340, 150, 10, 9, 13000),
(10005, 14, '시니어 개발자', 110, 6, 25, '2025-11-18', 9, 3, 410, 280, 18, 11, 14000),
(10006, 11, '중급 개발자', 85, 2, 12, '2025-11-15', 6, 1, 260, 100, 8, 7, 11000),
(10007, 9, '주니어 개발자', 60, 1, 10, '2025-11-10', 4, 1, 180, 80, 5, 4, 9000),
(10008, 16, '시니어 개발자', 130, 8, 35, '2025-11-08', 11, 3, 480, 220, 22, 13, 16000),
(10009, 20, '리드 개발자', 180, 15, 60, '2025-11-05', 15, 5, 650, 300, 30, 20, 20000);
