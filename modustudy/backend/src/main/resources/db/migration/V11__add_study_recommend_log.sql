-- ============================================================
-- V11: 스터디 참여 추천 로그 테이블
-- 향후 LLM 파인튜닝 학습 데이터 수집용
-- ============================================================

-- 1. 추천 세션 로그 (한 번의 추천 요청 = 1 세션)
CREATE TABLE `study_recommend_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `recommend_type` ENUM('GENERAL', 'TOPIC') NOT NULL DEFAULT 'GENERAL',
    `topic_id` BIGINT,                              -- TOPIC 타입일 때 필터 토픽
    `result_count` INT NOT NULL DEFAULT 0,           -- 추천 결과 수
    `user_tech_snapshot` JSON,                       -- 요청 시점 사용자 기술스택
    `user_schedule_snapshot` JSON,                   -- 요청 시점 사용자 일정
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`topic_id`) REFERENCES `topic`(`id`) ON DELETE SET NULL
);

-- 2. 추천 결과 상세 (세션당 추천된 스터디 목록 + 점수)
CREATE TABLE `study_recommend_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `log_id` BIGINT NOT NULL,                        -- FK: 추천 세션
    `study_id` BIGINT NOT NULL,
    `rank_position` INT NOT NULL,                    -- 노출 순위 (1부터)
    `matching_score` DECIMAL(7,2),                   -- 매칭 점수
    `tech_match_count` INT DEFAULT 0,
    `schedule_match_count` INT DEFAULT 0,
    `topic_match_count` INT DEFAULT 0,
    `match_reason` VARCHAR(500),                     -- 매칭 이유 텍스트
    FOREIGN KEY (`log_id`) REFERENCES `study_recommend_log`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE
);

-- 3. 사용자 반응 로그 (추천 결과에 대한 행동)
CREATE TABLE `study_recommend_action` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `log_id` BIGINT NOT NULL,                        -- FK: 추천 세션
    `item_id` BIGINT,                                -- FK: 추천 항목 (클릭한 항목)
    `study_id` BIGINT NOT NULL,
    `action_type` ENUM(
        'CLICK',                                     -- 상세 조회 클릭
        'APPLY',                                     -- 지원
        'BOOKMARK',                                  -- 북마크
        'DISMISS'                                    -- 관심없음 표시
    ) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`log_id`) REFERENCES `study_recommend_log`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`item_id`) REFERENCES `study_recommend_item`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_recommend_log_user` ON `study_recommend_log`(`user_id`);
CREATE INDEX `idx_recommend_log_created` ON `study_recommend_log`(`created_at`);
CREATE INDEX `idx_recommend_item_log` ON `study_recommend_item`(`log_id`);
CREATE INDEX `idx_recommend_action_log` ON `study_recommend_action`(`log_id`);
CREATE INDEX `idx_recommend_action_type` ON `study_recommend_action`(`action_type`);
