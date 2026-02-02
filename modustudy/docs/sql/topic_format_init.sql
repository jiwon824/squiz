SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

TRUNCATE TABLE topic;
TRUNCATE TABLE format;

-- Topic 데이터
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (1, '알고리즘/코딩테스트', NULL, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (11, '백준', 1, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (12, '프로그래머스', 1, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (13, 'SWEA', 1, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (14, 'LeetCode', 1, 4);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (15, '코드포스', 1, 5);

INSERT INTO topic (id, name, parent_id, sort_order) VALUES (2, 'CS 기초', NULL, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (21, '자료구조', 2, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (22, '운영체제', 2, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (23, '네트워크', 2, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (24, '데이터베이스', 2, 4);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (25, '컴퓨터구조', 2, 5);

INSERT INTO topic (id, name, parent_id, sort_order) VALUES (3, '프론트엔드', NULL, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (31, 'HTML/CSS', 3, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (32, 'JavaScript', 3, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (33, 'TypeScript', 3, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (34, 'React', 3, 4);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (35, 'Vue', 3, 5);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (36, 'Angular', 3, 6);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (37, 'Next.js', 3, 7);

INSERT INTO topic (id, name, parent_id, sort_order) VALUES (4, '백엔드', NULL, 4);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (41, 'Java/Spring', 4, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (42, 'Python/Django', 4, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (43, 'Node.js', 4, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (44, 'Go', 4, 4);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (45, 'Kotlin', 4, 5);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (46, 'C#/.NET', 4, 6);

INSERT INTO topic (id, name, parent_id, sort_order) VALUES (5, '인프라/DevOps', NULL, 5);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (51, 'Docker/Kubernetes', 5, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (52, 'AWS', 5, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (53, 'CI/CD', 5, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (54, 'Linux', 5, 4);

INSERT INTO topic (id, name, parent_id, sort_order) VALUES (6, '모바일', NULL, 6);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (61, 'Android', 6, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (62, 'iOS/Swift', 6, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (63, 'Flutter', 6, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (64, 'React Native', 6, 4);

INSERT INTO topic (id, name, parent_id, sort_order) VALUES (7, 'AI/머신러닝', NULL, 7);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (71, '머신러닝 기초', 7, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (72, '딥러닝', 7, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (73, '자연어처리', 7, 3);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (74, '컴퓨터비전', 7, 4);

INSERT INTO topic (id, name, parent_id, sort_order) VALUES (8, '데이터', NULL, 8);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (81, '데이터 분석', 8, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (82, '데이터 엔지니어링', 8, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (83, 'SQL', 8, 3);

INSERT INTO topic (id, name, parent_id, sort_order) VALUES (9, '보안', NULL, 9);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (91, '웹 보안', 9, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (92, '시스템 보안', 9, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (93, '암호학', 9, 3);

INSERT INTO topic (id, name, parent_id, sort_order) VALUES (10, '기타', NULL, 10);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (101, '개발 문화', 10, 1);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (102, '취업 준비', 10, 2);
INSERT INTO topic (id, name, parent_id, sort_order) VALUES (103, '사이드 프로젝트', 10, 3);

-- Format 데이터
INSERT INTO format (id, name, description, sort_order) VALUES (1, '문제 풀이', '알고리즘, 코딩테스트 문제를 함께 풀고 리뷰합니다', 1);
INSERT INTO format (id, name, description, sort_order) VALUES (2, '책/강의 스터디', '책이나 강의를 함께 학습하고 토론합니다', 2);
INSERT INTO format (id, name, description, sort_order) VALUES (3, '프로젝트', '실제 프로젝트를 함께 진행합니다', 3);
INSERT INTO format (id, name, description, sort_order) VALUES (4, '면접 준비', '기술 면접, 코딩 테스트를 함께 준비합니다', 4);
INSERT INTO format (id, name, description, sort_order) VALUES (5, '자격증', 'IT 자격증 취득을 함께 준비합니다', 5);
INSERT INTO format (id, name, description, sort_order) VALUES (6, '코드 리뷰', '서로의 코드를 리뷰하고 피드백합니다', 6);
INSERT INTO format (id, name, description, sort_order) VALUES (7, '모각코', '모여서 각자 코딩합니다', 7);
INSERT INTO format (id, name, description, sort_order) VALUES (8, '기타', '기타 형식의 스터디입니다', 8);

SET FOREIGN_KEY_CHECKS=1;
