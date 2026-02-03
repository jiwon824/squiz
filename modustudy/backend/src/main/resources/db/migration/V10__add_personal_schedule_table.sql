-- 개인 일정 테이블 생성
CREATE TABLE IF NOT EXISTS personal_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    start_time TIME,
    end_date DATE,
    end_time TIME,
    location VARCHAR(500),
    is_online BOOLEAN DEFAULT FALSE,
    color VARCHAR(20),
    google_event_id VARCHAR(255),
    is_synced_with_google BOOLEAN DEFAULT FALSE,
    last_synced_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_personal_schedule_user_id (user_id),
    INDEX idx_personal_schedule_start_date (start_date),
    INDEX idx_personal_schedule_user_date (user_id, start_date),
    INDEX idx_personal_schedule_google_event (google_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
