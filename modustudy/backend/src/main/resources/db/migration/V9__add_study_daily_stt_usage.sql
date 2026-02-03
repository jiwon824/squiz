-- V9: 스터디별 일일 STT/미팅 사용량 추적 테이블
-- 온라인 미팅: 3시간(10800초) 한도, 오프라인 STT: 2시간(7200초) 한도

CREATE TABLE study_daily_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    study_id BIGINT NOT NULL,
    usage_date DATE NOT NULL,

    -- 온라인 미팅 사용량 (실시간 화상회의)
    online_meeting_seconds INT NOT NULL DEFAULT 0,
    online_meeting_count INT NOT NULL DEFAULT 0,

    -- 오프라인 STT 사용량 (녹음 파일 업로드)
    offline_stt_seconds INT NOT NULL DEFAULT 0,
    offline_stt_count INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_study_date (study_id, usage_date),
    INDEX idx_usage_date (usage_date),
    CONSTRAINT fk_daily_usage_study FOREIGN KEY (study_id) REFERENCES study(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 한도 설명:
-- online_meeting_seconds: 온라인 미팅 누적 시간 (한도: 10800초 = 3시간)
-- offline_stt_seconds: 오프라인 STT 누적 시간 (한도: 7200초 = 2시간)
