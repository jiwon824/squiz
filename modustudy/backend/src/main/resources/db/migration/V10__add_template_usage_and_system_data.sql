-- ============================================================
-- V10: 템플릿 사용 로그 테이블 + 시스템 템플릿 초기 데이터
-- ============================================================

-- 1. 템플릿 사용 로그 (파인튜닝 데이터 수집용)
CREATE TABLE `template_usage_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `template_id` BIGINT NOT NULL,
    `study_id` BIGINT,
    `used_as_is` BOOLEAN DEFAULT FALSE,
    `modifications` JSON,
    `user_tech_stack` JSON,
    `user_schedule` JSON,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`template_id`) REFERENCES `study_template`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE SET NULL
);

CREATE INDEX `idx_template_usage_user` ON `template_usage_log`(`user_id`);
CREATE INDEX `idx_template_usage_template` ON `template_usage_log`(`template_id`);

-- 2. 시스템 템플릿 초기 데이터
-- 알고리즘
INSERT INTO `study_template` (`is_system`, `template_type`, `name`, `topic`, `format`, `meeting_type`, `difficulty`, `goal`, `textbook`, `penalty_policy`, `description`, `process_detail`) VALUES
(TRUE, 'ALGORITHM', '코딩테스트 입문반', '코딩테스트', '문제풀이', 'ONLINE', 'BEGINNER', '프로그래머스 Lv.1 완주', '프로그래머스', 'NORMAL', '코딩테스트 입문자를 위한 기초 알고리즘 스터디', '매주 5문제 풀이 → 풀이 공유 → 코드 리뷰'),
(TRUE, 'ALGORITHM', '코딩테스트 중급반', '코딩테스트', '문제풀이', 'ONLINE', 'INTERMEDIATE', '백준 골드 달성', '백준 단계별', 'NORMAL', '백준 골드 레벨을 목표로 하는 알고리즘 스터디', '매주 3문제 풀이 → 시간복잡도 분석 → 최적화 토론'),
(TRUE, 'ALGORITHM', '코딩테스트 고급반', '코딩테스트', '문제풀이', 'ONLINE', 'ADVANCED', '삼성 SW역량테스트 A형', '삼성 기출', 'STRICT', '기업 코딩테스트 대비 고급 알고리즘 스터디', '매주 2문제 모의고사 → 풀이 발표 → 시간 내 풀이 훈련');

-- CS
INSERT INTO `study_template` (`is_system`, `template_type`, `name`, `topic`, `format`, `meeting_type`, `difficulty`, `goal`, `textbook`, `penalty_policy`, `description`, `process_detail`) VALUES
(TRUE, 'CS', 'CS 기초 스터디', 'CS기초', '발표', 'ONLINE', 'BEGINNER', '면접 CS 질문 100개 정복', 'Tech Interview Guide', 'NORMAL', '면접 대비 CS 기초 개념 학습 스터디', '주제별 발표 준비 → 발표 → 모의 면접 Q&A'),
(TRUE, 'CS', '운영체제 심화 스터디', '운영체제', '발표', 'ONLINE', 'INTERMEDIATE', 'OS 핵심 개념 마스터', '공룡책 (Operating System Concepts)', 'NORMAL', '운영체제 핵심 개념 심화 학습 스터디', '매주 1챕터 → 발표 → 실습 과제');

-- 프로젝트
INSERT INTO `study_template` (`is_system`, `template_type`, `name`, `topic`, `format`, `meeting_type`, `difficulty`, `goal`, `textbook`, `penalty_policy`, `description`, `process_detail`) VALUES
(TRUE, 'PROJECT', '사이드 프로젝트', '사이드프로젝트', '프로젝트', 'HYBRID', 'INTERMEDIATE', '포트폴리오 1개 완성', NULL, 'LENIENT', '팀 사이드 프로젝트로 포트폴리오 제작', '주 1회 스프린트 회의 → 코드 리뷰 → 데모'),
(TRUE, 'PROJECT', '오픈소스 기여', '오픈소스', '프로젝트', 'ONLINE', 'ADVANCED', 'OSS 기여 경험', NULL, 'LENIENT', '오픈소스 프로젝트 기여 경험을 쌓는 스터디', 'OSS 분석 → 이슈 탐색 → PR 작성 → 리뷰');

-- 면접
INSERT INTO `study_template` (`is_system`, `template_type`, `name`, `topic`, `format`, `meeting_type`, `difficulty`, `goal`, `textbook`, `penalty_policy`, `description`, `process_detail`) VALUES
(TRUE, 'INTERVIEW', '기술 면접 준비', '기술면접', '모의면접', 'ONLINE', 'INTERMEDIATE', '기술 면접 합격', NULL, 'NORMAL', '기술 면접 대비 모의 면접 스터디', '주제별 준비 → 1:1 모의면접 → 피드백');

-- 독서
INSERT INTO `study_template` (`is_system`, `template_type`, `name`, `topic`, `format`, `meeting_type`, `difficulty`, `goal`, `textbook`, `penalty_policy`, `description`, `process_detail`) VALUES
(TRUE, 'READING', '개발 서적 읽기', '개발서적', '독서토론', 'ONLINE', 'ELEMENTARY', '월 1권 완독', NULL, 'LENIENT', '개발 관련 서적을 함께 읽고 토론하는 스터디', '매주 분량 읽기 → 핵심 정리 → 토론');
