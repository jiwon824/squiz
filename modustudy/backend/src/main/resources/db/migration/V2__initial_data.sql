-- =============================================
-- ModuStudy Initial Data
-- Flyway Migration V2 - Topic & Format Reference Data
-- =============================================

-- =============================================
-- Topic (대분류 및 소분류)
-- =============================================

-- 1. 알고리즘/코딩테스트 (id = 1)
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (1, '알고리즘/코딩테스트', NULL, 0, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (11, '백준', 1, 1, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (12, '프로그래머스', 1, 1, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (13, 'SWEA', 1, 1, 3);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (14, 'LeetCode', 1, 1, 4);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (15, '코드포스', 1, 1, 5);

-- 2. CS 기초 (id = 2)
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (2, 'CS 기초', NULL, 0, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (21, '자료구조', 2, 1, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (22, '운영체제', 2, 1, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (23, '네트워크', 2, 1, 3);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (24, '데이터베이스', 2, 1, 4);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (25, '컴퓨터구조', 2, 1, 5);

-- 3. 프론트엔드 (id = 3)
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (3, '프론트엔드', NULL, 0, 3);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (31, 'HTML/CSS', 3, 1, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (32, 'JavaScript', 3, 1, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (33, 'TypeScript', 3, 1, 3);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (34, 'React', 3, 1, 4);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (35, 'Vue', 3, 1, 5);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (36, 'Angular', 3, 1, 6);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (37, 'Next.js', 3, 1, 7);

-- 4. 백엔드 (id = 4)
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (4, '백엔드', NULL, 0, 4);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (41, 'Java/Spring', 4, 1, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (42, 'Python/Django', 4, 1, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (43, 'Node.js', 4, 1, 3);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (44, 'Go', 4, 1, 4);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (45, 'Kotlin', 4, 1, 5);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (46, 'C#/.NET', 4, 1, 6);

-- 5. 인프라/DevOps (id = 5)
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (5, '인프라/DevOps', NULL, 0, 5);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (51, 'Docker/Kubernetes', 5, 1, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (52, 'AWS', 5, 1, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (53, 'CI/CD', 5, 1, 3);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (54, 'Linux', 5, 1, 4);

-- 6. 모바일 (id = 6)
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (6, '모바일', NULL, 0, 6);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (61, 'Android', 6, 1, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (62, 'iOS/Swift', 6, 1, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (63, 'Flutter', 6, 1, 3);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (64, 'React Native', 6, 1, 4);

-- 7. AI/머신러닝 (id = 7)
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (7, 'AI/머신러닝', NULL, 0, 7);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (71, '머신러닝 기초', 7, 1, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (72, '딥러닝', 7, 1, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (73, '자연어처리', 7, 1, 3);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (74, '컴퓨터비전', 7, 1, 4);

-- 8. 데이터 (id = 8)
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (8, '데이터', NULL, 0, 8);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (81, '데이터 분석', 8, 1, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (82, '데이터 엔지니어링', 8, 1, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (83, 'SQL', 8, 1, 3);

-- 9. 보안 (id = 9)
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (9, '보안', NULL, 0, 9);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (91, '웹 보안', 9, 1, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (92, '시스템 보안', 9, 1, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (93, '암호학', 9, 1, 3);

-- 10. 기타 (id = 10)
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (10, '기타', NULL, 0, 10);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (101, '개발 문화', 10, 1, 1);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (102, '취업 준비', 10, 1, 2);
INSERT INTO topic (id, name, parent_id, level, ordering) VALUES (103, '사이드 프로젝트', 10, 1, 3);

-- =============================================
-- Format (스터디 형식)
-- =============================================
INSERT INTO format (id, name, description, ordering) VALUES (1, '문제 풀이', '알고리즘, 코딩테스트 문제를 함께 풀고 리뷰합니다', 1);
INSERT INTO format (id, name, description, ordering) VALUES (2, '독서/책 스터디', '기술 서적을 함께 읽고 토론합니다', 2);
INSERT INTO format (id, name, description, ordering) VALUES (3, '강의 수강', '온라인 강의를 함께 수강하고 학습 내용을 공유합니다', 3);
INSERT INTO format (id, name, description, ordering) VALUES (4, '프로젝트', '팀 프로젝트를 함께 진행합니다', 4);
INSERT INTO format (id, name, description, ordering) VALUES (5, '모의 면접', '기술 면접을 준비하고 모의 면접을 진행합니다', 5);
INSERT INTO format (id, name, description, ordering) VALUES (6, '발표/세미나', '주제별 발표와 세미나를 진행합니다', 6);
INSERT INTO format (id, name, description, ordering) VALUES (7, '자격증 준비', '자격증 시험을 함께 준비합니다', 7);
INSERT INTO format (id, name, description, ordering) VALUES (8, '자유 형식', '형식에 구애받지 않고 자유롭게 학습합니다', 8);
