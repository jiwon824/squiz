-- ============================================================
-- V19: Topic, Format 테이블 생성 및 초기 데이터 삽입
-- 목적: study 테이블의 VARCHAR topic/format을 정규화된 테이블로 변환 준비
-- ============================================================

-- 1. Topic 테이블 생성 (계층 구조: 대분류 - 소분류)
CREATE TABLE IF NOT EXISTS `topic` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `parent_id` BIGINT,
    `icon` VARCHAR(50),
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`parent_id`) REFERENCES `topic`(`id`) ON DELETE CASCADE,
    INDEX `idx_topic_parent` (`parent_id`),
    INDEX `idx_topic_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Format 테이블 생성
CREATE TABLE IF NOT EXISTS `format` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(200),
    `icon` VARCHAR(50),
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_format_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Topic 초기 데이터 삽입
-- 대분류 (parent_id = NULL)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`) VALUES
('알고리즘/코딩테스트', NULL, '💻', 1),
('CS 기초', NULL, '📚', 2),
('프론트엔드', NULL, '🎨', 3),
('백엔드', NULL, '⚙️', 4),
('인프라/DevOps', NULL, '🔧', 5),
('AI/ML', NULL, '🤖', 6),
('모바일', NULL, '📱', 7),
('자격증', NULL, '📜', 8),
('취업 준비', NULL, '💼', 9),
('프로젝트', NULL, '🚀', 10);

-- 소분류 (알고리즘/코딩테스트)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`)
SELECT '백준', id, NULL, 1 FROM `topic` WHERE `name` = '알고리즘/코딩테스트' AND `parent_id` IS NULL
UNION ALL
SELECT '프로그래머스', id, NULL, 2 FROM `topic` WHERE `name` = '알고리즘/코딩테스트' AND `parent_id` IS NULL
UNION ALL
SELECT 'SWEA', id, NULL, 3 FROM `topic` WHERE `name` = '알고리즘/코딩테스트' AND `parent_id` IS NULL
UNION ALL
SELECT 'LeetCode', id, NULL, 4 FROM `topic` WHERE `name` = '알고리즘/코딩테스트' AND `parent_id` IS NULL
UNION ALL
SELECT '코딩테스트 대비', id, NULL, 5 FROM `topic` WHERE `name` = '알고리즘/코딩테스트' AND `parent_id` IS NULL;

-- 소분류 (CS 기초)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`)
SELECT '자료구조', id, NULL, 1 FROM `topic` WHERE `name` = 'CS 기초' AND `parent_id` IS NULL
UNION ALL
SELECT '알고리즘 이론', id, NULL, 2 FROM `topic` WHERE `name` = 'CS 기초' AND `parent_id` IS NULL
UNION ALL
SELECT '운영체제', id, NULL, 3 FROM `topic` WHERE `name` = 'CS 기초' AND `parent_id` IS NULL
UNION ALL
SELECT '네트워크', id, NULL, 4 FROM `topic` WHERE `name` = 'CS 기초' AND `parent_id` IS NULL
UNION ALL
SELECT '데이터베이스', id, NULL, 5 FROM `topic` WHERE `name` = 'CS 기초' AND `parent_id` IS NULL
UNION ALL
SELECT '컴퓨터구조', id, NULL, 6 FROM `topic` WHERE `name` = 'CS 기초' AND `parent_id` IS NULL
UNION ALL
SELECT '디자인패턴', id, NULL, 7 FROM `topic` WHERE `name` = 'CS 기초' AND `parent_id` IS NULL
UNION ALL
SELECT '시스템 설계', id, NULL, 8 FROM `topic` WHERE `name` = 'CS 기초' AND `parent_id` IS NULL;

-- 소분류 (프론트엔드)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`)
SELECT 'HTML/CSS', id, NULL, 1 FROM `topic` WHERE `name` = '프론트엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'JavaScript', id, NULL, 2 FROM `topic` WHERE `name` = '프론트엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'TypeScript', id, NULL, 3 FROM `topic` WHERE `name` = '프론트엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'React', id, NULL, 4 FROM `topic` WHERE `name` = '프론트엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'Vue', id, NULL, 5 FROM `topic` WHERE `name` = '프론트엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'Next.js', id, NULL, 6 FROM `topic` WHERE `name` = '프론트엔드' AND `parent_id` IS NULL
UNION ALL
SELECT '웹 접근성/성능', id, NULL, 7 FROM `topic` WHERE `name` = '프론트엔드' AND `parent_id` IS NULL;

-- 소분류 (백엔드)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`)
SELECT 'Java/Spring', id, NULL, 1 FROM `topic` WHERE `name` = '백엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'Python/Django', id, NULL, 2 FROM `topic` WHERE `name` = '백엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'Python/FastAPI', id, NULL, 3 FROM `topic` WHERE `name` = '백엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'Node.js/Express', id, NULL, 4 FROM `topic` WHERE `name` = '백엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'Go', id, NULL, 5 FROM `topic` WHERE `name` = '백엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'Kotlin', id, NULL, 6 FROM `topic` WHERE `name` = '백엔드' AND `parent_id` IS NULL
UNION ALL
SELECT 'API 설계', id, NULL, 7 FROM `topic` WHERE `name` = '백엔드' AND `parent_id` IS NULL;

-- 소분류 (인프라/DevOps)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`)
SELECT 'Docker', id, NULL, 1 FROM `topic` WHERE `name` = '인프라/DevOps' AND `parent_id` IS NULL
UNION ALL
SELECT 'Kubernetes', id, NULL, 2 FROM `topic` WHERE `name` = '인프라/DevOps' AND `parent_id` IS NULL
UNION ALL
SELECT 'CI/CD', id, NULL, 3 FROM `topic` WHERE `name` = '인프라/DevOps' AND `parent_id` IS NULL
UNION ALL
SELECT 'AWS', id, NULL, 4 FROM `topic` WHERE `name` = '인프라/DevOps' AND `parent_id` IS NULL
UNION ALL
SELECT 'GCP', id, NULL, 5 FROM `topic` WHERE `name` = '인프라/DevOps' AND `parent_id` IS NULL
UNION ALL
SELECT 'Linux', id, NULL, 6 FROM `topic` WHERE `name` = '인프라/DevOps' AND `parent_id` IS NULL
UNION ALL
SELECT '모니터링', id, NULL, 7 FROM `topic` WHERE `name` = '인프라/DevOps' AND `parent_id` IS NULL;

-- 소분류 (AI/ML)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`)
SELECT '머신러닝 기초', id, NULL, 1 FROM `topic` WHERE `name` = 'AI/ML' AND `parent_id` IS NULL
UNION ALL
SELECT '딥러닝', id, NULL, 2 FROM `topic` WHERE `name` = 'AI/ML' AND `parent_id` IS NULL
UNION ALL
SELECT 'NLP', id, NULL, 3 FROM `topic` WHERE `name` = 'AI/ML' AND `parent_id` IS NULL
UNION ALL
SELECT '컴퓨터 비전', id, NULL, 4 FROM `topic` WHERE `name` = 'AI/ML' AND `parent_id` IS NULL
UNION ALL
SELECT 'MLOps', id, NULL, 5 FROM `topic` WHERE `name` = 'AI/ML' AND `parent_id` IS NULL
UNION ALL
SELECT '논문 리뷰', id, NULL, 6 FROM `topic` WHERE `name` = 'AI/ML' AND `parent_id` IS NULL;

-- 소분류 (모바일)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`)
SELECT 'Android (Kotlin)', id, NULL, 1 FROM `topic` WHERE `name` = '모바일' AND `parent_id` IS NULL
UNION ALL
SELECT 'Android (Java)', id, NULL, 2 FROM `topic` WHERE `name` = '모바일' AND `parent_id` IS NULL
UNION ALL
SELECT 'iOS (Swift)', id, NULL, 3 FROM `topic` WHERE `name` = '모바일' AND `parent_id` IS NULL
UNION ALL
SELECT 'Flutter', id, NULL, 4 FROM `topic` WHERE `name` = '모바일' AND `parent_id` IS NULL
UNION ALL
SELECT 'React Native', id, NULL, 5 FROM `topic` WHERE `name` = '모바일' AND `parent_id` IS NULL;

-- 소분류 (자격증)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`)
SELECT '정보처리기사', id, NULL, 1 FROM `topic` WHERE `name` = '자격증' AND `parent_id` IS NULL
UNION ALL
SELECT 'SQLD/SQLP', id, NULL, 2 FROM `topic` WHERE `name` = '자격증' AND `parent_id` IS NULL
UNION ALL
SELECT '리눅스마스터', id, NULL, 3 FROM `topic` WHERE `name` = '자격증' AND `parent_id` IS NULL
UNION ALL
SELECT '네트워크관리사', id, NULL, 4 FROM `topic` WHERE `name` = '자격증' AND `parent_id` IS NULL
UNION ALL
SELECT 'AWS 자격증', id, NULL, 5 FROM `topic` WHERE `name` = '자격증' AND `parent_id` IS NULL
UNION ALL
SELECT 'Azure 자격증', id, NULL, 6 FROM `topic` WHERE `name` = '자격증' AND `parent_id` IS NULL
UNION ALL
SELECT 'CKAD/CKA', id, NULL, 7 FROM `topic` WHERE `name` = '자격증' AND `parent_id` IS NULL;

-- 소분류 (취업 준비)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`)
SELECT '기술 면접', id, NULL, 1 FROM `topic` WHERE `name` = '취업 준비' AND `parent_id` IS NULL
UNION ALL
SELECT '코딩테스트 대비', id, NULL, 2 FROM `topic` WHERE `name` = '취업 준비' AND `parent_id` IS NULL
UNION ALL
SELECT '포트폴리오', id, NULL, 3 FROM `topic` WHERE `name` = '취업 준비' AND `parent_id` IS NULL
UNION ALL
SELECT '이력서/자소서', id, NULL, 4 FROM `topic` WHERE `name` = '취업 준비' AND `parent_id` IS NULL
UNION ALL
SELECT '모의 면접', id, NULL, 5 FROM `topic` WHERE `name` = '취업 준비' AND `parent_id` IS NULL;

-- 소분류 (프로젝트)
INSERT INTO `topic` (`name`, `parent_id`, `icon`, `sort_order`)
SELECT '사이드 프로젝트', id, NULL, 1 FROM `topic` WHERE `name` = '프로젝트' AND `parent_id` IS NULL
UNION ALL
SELECT '클론 코딩', id, NULL, 2 FROM `topic` WHERE `name` = '프로젝트' AND `parent_id` IS NULL
UNION ALL
SELECT '오픈소스 기여', id, NULL, 3 FROM `topic` WHERE `name` = '프로젝트' AND `parent_id` IS NULL
UNION ALL
SELECT '해커톤 준비', id, NULL, 4 FROM `topic` WHERE `name` = '프로젝트' AND `parent_id` IS NULL;

-- 4. Format 초기 데이터 삽입
INSERT INTO `format` (`name`, `description`, `icon`, `sort_order`) VALUES
('문제 풀이', '알고리즘 문제를 풀고 풀이를 공유합니다', '✏️', 1),
('독서/책 스터디', '개발 서적을 함께 읽고 토론합니다', '📖', 2),
('강의 수강', '온라인 강의를 함께 듣고 학습합니다', '🎓', 3),
('프로젝트', '팀 프로젝트를 진행합니다', '🚀', 4),
('모의 면접', '기술 면접을 연습합니다', '💼', 5),
('코드 리뷰', '서로의 코드를 리뷰합니다', '🔍', 6),
('발표/세미나', '주제를 정해 발표하고 토론합니다', '🎤', 7),
('토론', '특정 주제에 대해 토론합니다', '💬', 8);
