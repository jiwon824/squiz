-- =============================================
-- ModuStudy ERD (DDL)
-- ERDCloud import용
-- =============================================

-- =============================================
-- 1. 사용자/인증
-- =============================================

CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255),                      -- 비밀번호 (nullable, 소셜만 사용 시 NULL)
    `name` VARCHAR(50),
    `nickname` VARCHAR(50) NOT NULL UNIQUE,
    `profile_image` VARCHAR(500),
    `role` ENUM('USER', 'ADMIN') DEFAULT 'USER',
    `is_active` BOOLEAN DEFAULT TRUE,
    `is_online` BOOLEAN DEFAULT FALSE,            -- 접속 상태
    `last_seen_at` TIMESTAMP,                     -- 마지막 접속 시간
    `is_searchable` BOOLEAN DEFAULT TRUE,         -- 친구 검색 허용 여부
    `leader_rating` FLOAT DEFAULT 0.0,            -- 스터디장 평점 (캐싱)
    `leader_review_count` INT DEFAULT 0,          -- 스터디장 평가 수 (캐싱)
    `last_login_at` TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 사용자 소셜 계정 연동
CREATE TABLE `user_social_account` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `provider` ENUM('GOOGLE', 'KAKAO', 'NAVER') NOT NULL,
    `provider_user_id` VARCHAR(100) NOT NULL,     -- 소셜 서비스에서의 사용자 ID
    `email` VARCHAR(100),                         -- 소셜 계정 이메일
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_provider_user` (`provider`, `provider_user_id`)
);

-- 로그인 이력
CREATE TABLE `login_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `provider` ENUM('GOOGLE', 'KAKAO', 'NAVER', 'EMAIL') NOT NULL,  -- 로그인 방식
    `login_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `ip_address` VARCHAR(45),
    `device_info` VARCHAR(200),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE `user_schedule` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `day_of_week` ENUM('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN') NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `is_available` BOOLEAN DEFAULT TRUE,         -- TRUE: 가능, FALSE: 불가능
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

-- 사용자 소속 인증 (매핑 테이블)
CREATE TABLE `user_organization` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `org_type` VARCHAR(50) NOT NULL,             -- SSAFY, NBC, WTC, UNIVERSITY 등
    `org_data` JSON NOT NULL,                    -- 타입별 상세 정보 (기수, 캠퍼스, retireYn 등)
    `verified_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 최초 인증 시점
    `last_checked_at` TIMESTAMP,                 -- 마지막 상태 체크 시점 (스터디 개설/가입 시)
    `is_active` BOOLEAN DEFAULT TRUE,            -- 인증 유효 여부 (퇴소 시 FALSE)
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

-- 사용자 프로필
CREATE TABLE `profile` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `profile_image_url` VARCHAR(500),
    `profile_image_source` ENUM('KAKAO', 'GOOGLE', 'NAVER', 'UPLOAD') DEFAULT 'UPLOAD',
    `bio` TEXT,
    `social_links` JSON,
    `tech` JSON,
    `favorite` JSON,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

-- 리프레시 토큰
CREATE TABLE `refresh_token` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(500) NOT NULL,
    `expires_at` TIMESTAMP NOT NULL,
    `is_revoked` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_refresh_token` (`token`)
);

-- 비밀번호 재설정
CREATE TABLE password_reset_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id)
);

-- =============================================
-- 1-1. 친구/DM
-- =============================================

-- 친구 관계
CREATE TABLE `friendship` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `requester_id` BIGINT NOT NULL,              -- 친구 요청자
    `addressee_id` BIGINT NOT NULL,              -- 친구 요청 대상
    `status` ENUM('PENDING', 'ACCEPTED') DEFAULT 'PENDING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`requester_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`addressee_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_friendship` (`requester_id`, `addressee_id`),
    INDEX `idx_friendship_requester` (`requester_id`),
    INDEX `idx_friendship_addressee` (`addressee_id`),
    INDEX `idx_friendship_status` (`status`)
);

-- 사용자 차단
CREATE TABLE `user_block` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `blocker_id` BIGINT NOT NULL,                -- 차단한 사용자
    `blocked_id` BIGINT NOT NULL,                -- 차단당한 사용자
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`blocker_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`blocked_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_block` (`blocker_id`, `blocked_id`),
    INDEX `idx_user_block_blocker` (`blocker_id`),
    INDEX `idx_user_block_blocked` (`blocked_id`)
);

-- DM 대화방
CREATE TABLE `dm_conversation` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user1_id` BIGINT NOT NULL,                  -- 참여자 1 (ID가 작은 쪽)
    `user2_id` BIGINT NOT NULL,                  -- 참여자 2 (ID가 큰 쪽)
    `user1_last_read_message_id` BIGINT,         -- user1의 마지막 읽은 메시지 ID
    `user2_last_read_message_id` BIGINT,         -- user2의 마지막 읽은 메시지 ID
    `user1_deleted` BOOLEAN DEFAULT FALSE,       -- user1이 대화를 삭제했는지
    `user2_deleted` BOOLEAN DEFAULT FALSE,       -- user2가 대화를 삭제했는지
    `last_message_at` TIMESTAMP,                 -- 마지막 메시지 시간
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user1_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user2_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_dm_conversation_users` (`user1_id`, `user2_id`),
    INDEX `idx_dm_conversation_user1` (`user1_id`),
    INDEX `idx_dm_conversation_user2` (`user2_id`),
    INDEX `idx_dm_conversation_last_message` (`last_message_at` DESC)
);

-- DM 메시지
CREATE TABLE `direct_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` BIGINT NOT NULL,           -- 소속 대화방 ID
    `sender_id` BIGINT NOT NULL,                 -- 발신자 ID
    `content` VARCHAR(2000) NOT NULL,            -- 메시지 내용
    `is_deleted` BOOLEAN DEFAULT FALSE,          -- 삭제 여부
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`conversation_id`) REFERENCES `dm_conversation`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX `idx_direct_message_conversation_created` (`conversation_id`, `created_at` DESC)
);


-- =============================================
-- 2. 스터디
-- =============================================

-- 지역 테이블
CREATE TABLE `region` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `code` VARCHAR(20) NOT NULL UNIQUE,          -- SEOUL, BUSAN 등
    `name` VARCHAR(50) NOT NULL,                 -- 서울, 부산 등
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 지역 초기 데이터
-- INSERT INTO region (code, name, sort_order) VALUES
-- ('SEOUL', '서울', 1), ('BUSAN', '부산', 2), ('DAEGU', '대구', 3),
-- ('INCHEON', '인천', 4), ('GWANGJU', '광주', 5), ('DAEJEON', '대전', 6),
-- ('ULSAN', '울산', 7), ('SEJONG', '세종', 8), ('GYEONGGI', '경기', 9),
-- ('GANGWON', '강원', 10), ('CHUNGBUK', '충북', 11), ('CHUNGNAM', '충남', 12),
-- ('JEONBUK', '전북', 13), ('JEONNAM', '전남', 14), ('GYEONGBUK', '경북', 15),
-- ('GYEONGNAM', '경남', 16), ('JEJU', '제주', 17);

CREATE TABLE `study` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `leader_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `intro` VARCHAR(200),                        -- 한줄 소개
    `description` TEXT,
    `topic_id` BIGINT NOT NULL,               -- 알고리즘/CS/자격증/프로젝트 등
    `format_id` BIGINT,                       -- 문제풀이/독서/강의수강/프로젝트
    `study_type` ENUM('PLANNED', 'LIGHTNING') NOT NULL,  -- 계획/번개
    `meeting_type` ENUM('ONLINE', 'OFFLINE', 'HYBRID') DEFAULT 'ONLINE',  -- 진행 방식
    `region_id` BIGINT,                          -- 오프라인/혼합 시 지역
    `location_detail` VARCHAR(200),              -- 상세 장소 (ex: 강남역 근처)
    `schedule_summary` VARCHAR(100),             -- 일정 요약 (ex: 매주 월/수 19:00)
    `schedule_days` VARCHAR(50),                 -- 요일 (ex: MON,WED)
    `schedule_time` TIME,                        -- 시작 시간
    `max_members` INT DEFAULT 10,
    `is_public` BOOLEAN DEFAULT TRUE,            -- 공개/비공개
    `status` ENUM('DRAFT', 'SCHEDULED', 'RECRUITING', 'RECRUIT_CLOSED', 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'DRAFT',
    `penalty_policy` ENUM('STRICT', 'NORMAL', 'LENIENT', 'RATIO', 'NONE') DEFAULT 'NORMAL',
    `start_date` DATE,
    `end_date` DATE,
    `total_sessions` INT,                        -- 총 회차
    `recruit_start_date` DATE,
    `recruit_end_date` DATE,
    `extension_count` INT DEFAULT 0,             -- 모집 연장 횟수 (최대 1회)
    `textbook` VARCHAR(500),                     -- 사용 교재/자료
    `goal` VARCHAR(500),                         -- 스터디 목표
    `difficulty` ENUM('BEGINNER', 'ELEMENTARY', 'INTERMEDIATE', 'ADVANCED') DEFAULT 'INTERMEDIATE',  -- 난이도
    `prerequisites` TEXT,                        -- 사전 지식
    `process_detail` TEXT,                       -- 진행 방식 상세
    `target_org_type` VARCHAR(50),               -- 대상 소속 타입 (SSAFY, NBC, WTC 등, NULL이면 누구나)
    `target_org_criteria` JSON,                  -- 대상 조건 ({"generation": 14} 등, NULL이면 해당 소속 전체)
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`leader_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`region_id`) REFERENCES `region`(`id`),
    FOREIGN KEY (`topic_id`) REFERENCES `topic`(`id`),
    FOREIGN KEY (`format_id`) REFERENCES `format`(`id`)
);

-- 스터디 모집글 템플릿 (사용자 저장용)
CREATE TABLE `study_template` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT,                            -- NULL이면 시스템 템플릿
    `name` VARCHAR(100) NOT NULL,                -- 템플릿 이름
    `intro` VARCHAR(200),                        -- 한줄 소개
    `is_system` BOOLEAN DEFAULT FALSE,           -- 시스템 기본 템플릿 여부
    `template_type` VARCHAR(50),                 -- ALGORITHM, CS, INTERVIEW, PROJECT, CERTIFICATE, READING
    `topic_id` BIGINT,                           -- Topic 테이블 참조
    `format_id` BIGINT,                          -- Format 테이블 참조
    `meeting_type` ENUM('ONLINE', 'OFFLINE', 'HYBRID'),
    `description` TEXT,
    `textbook` VARCHAR(500),
    `goal` VARCHAR(500),
    `difficulty` ENUM('BEGINNER', 'ELEMENTARY', 'INTERMEDIATE', 'ADVANCED'),
    `prerequisites` TEXT,
    `process_detail` TEXT,
    `penalty_policy` ENUM('STRICT', 'NORMAL', 'LENIENT', 'RATIO', 'NONE'),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`topic_id`) REFERENCES `topic`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`format_id`) REFERENCES `format`(`id`) ON DELETE SET NULL
);

-- 스터디 모집글 댓글
CREATE TABLE `study_comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT,                          -- 대댓글인 경우 부모 댓글 ID
    `content` TEXT NOT NULL,
    `image_url` VARCHAR(500),                    -- 댓글 첨부 이미지 URL
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`parent_id`) REFERENCES `study_comment`(`id`) ON DELETE CASCADE
);

CREATE TABLE `study_member` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role` ENUM('LEADER', 'MEMBER') DEFAULT 'MEMBER',
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'LEFT', 'KICKED') DEFAULT 'PENDING',
    `joined_at` TIMESTAMP,
    `left_at` TIMESTAMP,
    `is_probation` BOOLEAN DEFAULT TRUE,         -- 수습기간 여부
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_study_user` (`study_id`, `user_id`)
);

CREATE TABLE `study_session` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `session_number` INT NOT NULL,               -- 회차 번호
    `title` VARCHAR(200),
    `description` TEXT,
    `scheduled_at` TIMESTAMP NOT NULL,           -- 예정 일시
    `duration_minutes` INT DEFAULT 60,
    `location` VARCHAR(200),                     -- 장소 (온라인/오프라인 주소)
    `is_online` BOOLEAN DEFAULT TRUE,
    `status` ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    `completed_at` TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_study_session` (`study_id`, `session_number`)
);

CREATE TABLE `study_bookmark` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `study_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_study` (`user_id`, `study_id`)
);

CREATE TABLE `study_application` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `message` TEXT,                              -- 지원 메시지
    `matching_score` DECIMAL(5,2),               -- AI 매칭 점수
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    `rejected_reason` VARCHAR(500),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `processed_at` TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE `study_leader_review` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `reviewer_id` BIGINT NOT NULL,               -- 평가자
    `leader_id` BIGINT NOT NULL,                 -- 평가 대상 (스터디장)
    `rating` DECIMAL(2,1) NOT NULL,              -- 1.0 ~ 5.0
    `comment` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`reviewer_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`leader_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_study_reviewer` (`study_id`, `reviewer_id`)
);

CREATE TABLE `format` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `description` VARCHAR(200),
    `icon` VARCHAR(50),
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `topic` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `parent_id` BIGINT NULL,
    `icon` VARCHAR(50),
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`parent_id`) REFERENCES `topic`(`id`) ON DELETE SET NULL
);

-- =============================================
-- 3. 워크스페이스/채팅
-- =============================================

CREATE TABLE `workspace` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE
);

CREATE TABLE `message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `workspace_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `message_type` ENUM('TEXT', 'IMAGE', 'FILE', 'SYSTEM') DEFAULT 'TEXT',
    `file_url` VARCHAR(500),
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `is_pinned` BOOLEAN DEFAULT FALSE,              -- 메시지 고정 여부
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`workspace_id`) REFERENCES `workspace`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX `idx_message_pinned` (`workspace_id`, `is_pinned`)
);

-- =============================================
-- 4. 미팅
-- =============================================

CREATE TABLE `meeting` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `session_id` BIGINT,                         -- 연결된 스터디 세션
    `workspace_id` BIGINT,                         -- 음성 채널
    `title` VARCHAR(200),
    `meeting_type` ENUM('DAILY', 'WEEKLY', 'FREE', 'OTHER') NOT NULL,
    `started_at` TIMESTAMP,
    `ended_at` TIMESTAMP,
    `duration_seconds` INT,
    `planned_duration_seconds` INT,
    `participant_count` INT DEFAULT 0,
    `status` ENUM('WAITING', 'IN_PROGRESS', 'ENDED') DEFAULT 'WAITING',
    `recording_status`	ENUM('WAITING', 'RECORDING', 'READY', 'FAILED'),  -- 녹화 상태
    `processing_status` ENUM('PENDING', 'PROCESSING', 'DONE', 'FAILED') DEFAULT 'PENDING', -- 녹화 처리 상태
	`stt_status`	ENUM('PENDING', 'PROCESSING', 'DONE', 'FAILED') DEFAULT 'PENDING',
	`summary_status`	ENUM('PENDING', 'PROCESSING', 'DONE', 'FAILED') DEFAULT 'PENDING',
	`auto_share_summary`	BOOLEAN DEFAULT FALSE,
	`share_workspace_id`	BIGINT	NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`),
    FOREIGN KEY (`workspace_id`) REFERENCES `workspace`(`id`)
);

CREATE TABLE `meeting_participant` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `joined_at` TIMESTAMP NOT NULL,
    `left_at` TIMESTAMP,
    `is_muted` BOOLEAN DEFAULT FALSE,
    `is_camera_on` BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE `meeting_participant_summary` (
	`id`	VARCHAR(255)	NOT NULL,
	`meeting_id`	BIGINT	NULL,
	`user_id`	BIGINT	NOT NULL,
	`summary`	TEXT	NULL,
	`created_at`	TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE `meeting_transcript` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,                     -- STT 결과
    `timestamp_seconds` INT NOT NULL,            -- 미팅 시작 기준 초
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE `meeting_summary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL UNIQUE,
    `summary` TEXT,                              -- AI 요약
    `action_items` JSON,                         -- 액션 아이템 목록
    `keywords` JSON,                             -- 키워드 목록
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE
);

CREATE TABLE `meeting_photo` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `image_url` VARCHAR(500) NOT NULL,
    `captured_at` TIMESTAMP NOT NULL,
    `is_selected` BOOLEAN DEFAULT FALSE,         -- 보고서용 선택 여부
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE
);

CREATE TABLE `meeting_recording` (
	`meeting_recording_id`	BIGINT	NOT NULL,
	`meeting_id`	BIGINT	NULL,
	`recording_url`	VARCHAR(500)	NULL,
	`format`	VARCHAR(20)	NULL,
	`duration_seconds`	INT	NULL,
	`started_at`	TIMESTAMP	NULL,
	`ended_at`	TIMESTAMP	NULL,
	`file_size`	BIGINT	NULL,
	`status`	ENUM('UPLOADING', 'READY', 'FAILED') DEFAULT 'UPLOADING'	NULL,
	`created_at`	TIMESTAMP DEFAULT CURRENT_TIMESTAMP	NULL,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE    
);

CREATE TABLE `meeting_action_item` (
	`id`	BIGINT	NOT NULL,
	`meeting_id`	BIGINT	NULL,
	`user_id`	BIGINT	NOT NULL,
	`content`	TEXT	NULL,
	`status`	ENUM('TODO', 'DONE') DEFAULT 'TODO'	NULL,
	`created_at`	TIMESTAMP	NULL,
    FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)    
);

CREATE TABLE `meeting_chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `meeting_id` BIGINT NOT NULL,
    `user_id` BIGINT NULL,
    `sender_name` VARCHAR(100) NOT NULL,
    `content` TEXT NOT NULL,
    `sent_at` DATETIME NOT NULL,
    `created_at` DATETIME DEFAULT NULL,
    `updated_at` DATETIME ON UPDATE CURRENT_TIMESTAMP,
      FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
      FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX idx_meeting_chat_message_meeting_id_sent_at (meeting_id, sent_at)
  );
  
  CREATE TABLE `meeting_audio_recording` (
      `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
      `meeting_id` BIGINT NOT NULL,
      `user_id` BIGINT NULL,
      `track_type` ENUM('MIXED', 'INDIVIDUAL') NOT NULL,
      `recording_url` VARCHAR(500) NOT NULL,
      `format` VARCHAR(20) NULL,
      `file_size` BIGINT NULL,
      `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
      FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
  );
  
  CREATE TABLE `meeting_stt_file` (
      `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
      `meeting_id` BIGINT NOT NULL,
      `user_id` BIGINT NULL,
      `track_type` ENUM('MIXED', 'INDIVIDUAL') NOT NULL,
      `file_url` VARCHAR(500) NOT NULL,
      `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
      FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
      UNIQUE KEY `uk_meeting_stt_file` (`meeting_id`, `track_type`, `user_id`)
  );

  CREATE TABLE `meeting_stt_summary` (
      `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
      `meeting_id` BIGINT NOT NULL,
      `user_id` BIGINT NULL,
      `track_type` ENUM('MIXED', 'INDIVIDUAL') NOT NULL,
      `file_url` VARCHAR(500) NOT NULL,
      `action_items` JSON NULL,
      `keywords` JSON NULL,
      `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      FOREIGN KEY (`meeting_id`) REFERENCES `meeting`(`id`) ON DELETE CASCADE,
      FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
      UNIQUE KEY `uk_meeting_stt_summary` (`meeting_id`, `track_type`, `user_id`)
  );
-- =============================================
-- 5. 출석
-- =============================================

CREATE TABLE `attendance` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `check_type` ENUM('BLE', 'SELF', 'AUTO') DEFAULT 'SELF',
    `status` ENUM('PRESENT', 'LATE', 'ABSENT', 'EXCUSED') DEFAULT 'ABSENT',
    `checked_at` TIMESTAMP,
    `checked_by` BIGINT,                         -- BLE 체크 시 스터디장 ID
    `excuse_reason` TEXT,
    `excuse_status` ENUM('PENDING', 'APPROVED', 'REJECTED'),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`checked_by`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_session_user` (`session_id`, `user_id`)
);

-- 세션별 개인 메모 (오프라인 스터디용)
CREATE TABLE `session_memo` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` VARCHAR(500) NOT NULL,             -- 한 줄 메모
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_session_user_memo` (`session_id`, `user_id`)
);

-- ============================================================================
-- 6. 퀴즈 및 FSRS 통합 시스템 (최종 통합본)
-- 설계 원칙: 
-- 1. 시간 단위의 ms 통일 (말해보카식 정밀 측정을 위함)
-- 2. FSRS 데이터의 수치 안정성 확보 (DOUBLE NOT NULL)
-- 3. 다형성 구조를 통한 다양한 퀴즈 소스 통합 (content_type)
-- ============================================================================

-- 6-1. 카테고리 및 공용 문제 풀
CREATE TABLE IF NOT EXISTS `quiz_category` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `parent_id` BIGINT,
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '카테고리 코드 (예: JAVA_01)',
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `depth` INT NOT NULL DEFAULT 0,
    `sort_order` INT DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`parent_id`) REFERENCES `quiz_category`(`id`) ON DELETE CASCADE,
    INDEX `idx_parent_depth` (`parent_id`, `depth`, `sort_order`)
);

CREATE TABLE IF NOT EXISTS `quiz_question_pool` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `category_id` BIGINT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('MULTIPLE_CHOICE', 'MULTIPLE_CHOICE_MULTIPLE', 'SHORT_ANSWER') DEFAULT 'MULTIPLE_CHOICE',
    `correct_answer` JSON NOT NULL COMMENT '정답 데이터 (배열 형태)',
    `explanation` TEXT,
    `difficulty` ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM',
    `tags` JSON,
    `usage_count` INT DEFAULT 0,
    `correct_rate` DECIMAL(5,2),
    `created_by` BIGINT NOT NULL,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`category_id`) REFERENCES `quiz_category`(`id`) ON DELETE CASCADE,
    INDEX `idx_category_difficulty` (`category_id`, `difficulty`, `is_active`)
);

CREATE TABLE IF NOT EXISTS `quiz_question_pool_option` (
    `question_pool_id` BIGINT NOT NULL,
    `option_label` VARCHAR(10) NOT NULL, -- A, B, C, D ...
    `option_text` VARCHAR(500) NOT NULL,
    `is_correct` BOOLEAN DEFAULT FALSE,
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`question_pool_id`, `option_label`),
    FOREIGN KEY (`question_pool_id`) REFERENCES `quiz_question_pool`(`id`) ON DELETE CASCADE,
    INDEX `idx_question_option_order` (`question_pool_id`, `sort_order`)
);

-- 6-2. 연습 모드 통계 및 기록
CREATE TABLE IF NOT EXISTS `quiz_practice_stats` (
    `user_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `total_attempted` INT DEFAULT 0,
    `total_correct` INT DEFAULT 0,
    `best_score` INT DEFAULT 0,
    `last_attempted_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `category_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`category_id`) REFERENCES `quiz_category`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `quiz_practice_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category_id` BIGINT NOT NULL,
    `total_questions` INT NOT NULL,
    `correct_count` INT NOT NULL,
    `score` INT NOT NULL,
    `time_spent_seconds` INT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`category_id`) REFERENCES `quiz_category`(`id`) ON DELETE CASCADE,
    INDEX `idx_user_created` (`user_id`, `created_at`)
);

CREATE TABLE IF NOT EXISTS `quiz_practice_answer` (
    `practice_record_id` BIGINT NOT NULL,
    `question_pool_id` BIGINT NOT NULL,
    `user_answer` JSON, -- 사용자 선택 보기 번호 배열
    `is_correct` BOOLEAN NOT NULL,
    `time_taken_seconds` INT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`practice_record_id`, `question_pool_id`),
    FOREIGN KEY (`practice_record_id`) REFERENCES `quiz_practice_record`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_pool_id`) REFERENCES `quiz_question_pool`(`id`)
);

-- 6-3. 퀴즈 대회 (실시간 진행)
CREATE TABLE IF NOT EXISTS `quiz_contest` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `category_id` BIGINT,
    `contest_type` ENUM('PUBLIC', 'STUDY') DEFAULT 'PUBLIC',
    `study_id` BIGINT,
    `created_by` BIGINT NOT NULL,
    `status` ENUM('DRAFT', 'SCHEDULED', 'IN_PROGRESS', 'ENDED') DEFAULT 'DRAFT',
    `scheduled_at` TIMESTAMP NULL,
    `started_at` TIMESTAMP NULL,
    `ended_at` TIMESTAMP NULL,
    `time_limit_seconds` INT DEFAULT 30,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`category_id`) REFERENCES `quiz_category`(`id`),
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`),
    FOREIGN KEY (`created_by`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `quiz_contest_state` (
    `contest_id` BIGINT NOT NULL PRIMARY KEY,
    `current_question_pool_id` BIGINT,
    `current_question_started_at` TIMESTAMP NULL,
    `is_showing_results` BOOLEAN DEFAULT FALSE,
    `phase` ENUM('WAITING', 'QUESTION', 'RESULT', 'ENDED') DEFAULT 'WAITING',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`current_question_pool_id`) REFERENCES `quiz_question_pool`(`id`)
);

CREATE TABLE IF NOT EXISTS `quiz_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `contest_id` BIGINT NOT NULL,
    `question_pool_id` BIGINT,
    `question_number` INT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('MULTIPLE_CHOICE', 'SHORT_ANSWER') DEFAULT 'MULTIPLE_CHOICE',
    `options` JSON,
    `correct_answer` VARCHAR(500) NOT NULL,
    `explanation` TEXT,
    `points` INT DEFAULT 10,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_pool_id`) REFERENCES `quiz_question_pool`(`id`)
);

CREATE TABLE IF NOT EXISTS `quiz_participant` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `contest_id` BIGINT NOT NULL,
    `user_id` BIGINT,
    `nickname` VARCHAR(50) NOT NULL,
    `total_score` INT DEFAULT 0,
    `correct_count` INT DEFAULT 0,
    `rank` INT,
    `last_answer_time` TIMESTAMP NULL,
    `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX `idx_contest_score` (`contest_id`, `total_score`, `last_answer_time`),
    INDEX `idx_participant_user` (`user_id`, `joined_at`)
);

CREATE TABLE IF NOT EXISTS `quiz_contest_chat` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `contest_id` BIGINT NOT NULL,
    `user_id` BIGINT,
    `participant_id` BIGINT NOT NULL,
    `message` VARCHAR(500) NOT NULL,
    `message_type` ENUM('TEXT', 'SYSTEM') NOT NULL DEFAULT 'TEXT',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`contest_id`) REFERENCES `quiz_contest`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`participant_id`) REFERENCES `quiz_participant`(`id`) ON DELETE CASCADE,
    INDEX `idx_contest_created` (`contest_id`, `created_at`)
);

CREATE TABLE IF NOT EXISTS `quiz_answer` (
    `participant_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `user_answer` JSON,
    `is_correct` BOOLEAN,
    `score` INT DEFAULT 0,
    `time_taken_seconds` INT,
    `answered_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`participant_id`, `question_id`),
    FOREIGN KEY (`participant_id`) REFERENCES `quiz_participant`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_id`) REFERENCES `quiz_question`(`id`) ON DELETE CASCADE
);

-- 6-4. 스터디 퀴즈 (이어풀기 및 상태 관리)
CREATE TABLE IF NOT EXISTS `study_quiz` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `session_id` BIGINT,
    `title` VARCHAR(200) NOT NULL,
    `source_type` ENUM('MEETING', 'MATERIAL', 'MANUAL') NOT NULL, -- 화상회의 기반(화상회의에서 오간 대화 내용, 요약본, 사용자의 발언 오류 등을 AI가 분석해 생성), 학습 자료 기반(사용자가 업로드한 PDF, PPT, 텍스트 문서 등 정적인 자료를 바탕으로 AI 생성), 수동 생성(스터디 장이나 사용자가 직접 문제를 출제)
    `source_id` BIGINT,
    `status` ENUM('ACTIVE', 'DISABLED') DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`)
);

CREATE TABLE IF NOT EXISTS `study_quiz_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `quiz_id` BIGINT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('MULTIPLE_CHOICE', 'SHORT_ANSWER') DEFAULT 'MULTIPLE_CHOICE',
    `options` JSON,
    `correct_answer` VARCHAR(500) NOT NULL,
    `explanation` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`quiz_id`) REFERENCES `study_quiz`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `study_quiz_attempt` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `quiz_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `score` INT DEFAULT 0,
    `total_questions` INT,
    `correct_count` INT,
    `status` ENUM('IN_PROGRESS', 'COMPLETED', 'ABANDONED') DEFAULT 'IN_PROGRESS',
    `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `last_answered_at` TIMESTAMP NULL,
    `current_question_index` INT DEFAULT 0,
    `completed_at` TIMESTAMP NULL,
    FOREIGN KEY (`quiz_id`) REFERENCES `study_quiz`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    INDEX `idx_quiz_attempt_status` (`quiz_id`, `status`)
);

CREATE TABLE IF NOT EXISTS `study_quiz_answer` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `attempt_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `question_index` INT NOT NULL COMMENT '문제 순서 (0부터 시작)',
    `user_answer` JSON COMMENT '사용자 답변 (객관식: ["A"], 단답형: "답")',
    `is_correct` BOOLEAN DEFAULT FALSE,
    `time_taken_seconds` INT DEFAULT 0 COMMENT '문제 풀이 소요 시간',
    `answered_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`attempt_id`) REFERENCES `study_quiz_attempt`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_id`) REFERENCES `study_quiz_question`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_attempt_question` (`attempt_id`, `question_id`),
    INDEX `idx_attempt_index` (`attempt_id`, `question_index`)
) COMMENT '스터디 퀴즈 개별 문제 답변 기록';

-- 6-5. FSRS 기반 통합 복습 시스템 (V14 반영)
-- 기존 오답노트(wrong_answer_note)를 대체함
CREATE TABLE IF NOT EXISTS `user_review_items` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    
    -- 다형성 구조 (코스 퀴즈, AI 생성 퀴즈 등 수용)
    `content_type` VARCHAR(20) NOT NULL, -- 'STATIC_COURSE' 또는 'AI_GENERATED'
    `content_id` BIGINT NOT NULL,        -- 해당 퀴즈 테이블의 PK
    
    -- FSRS 핵심 변수 (지수 함수 정밀 연산을 위해 DOUBLE 사용)
    `stability` DOUBLE NOT NULL DEFAULT 0.0,      -- 기억 안정성 (S)
    `difficulty` DOUBLE NOT NULL DEFAULT 5.0,     -- 난이도 (D, 1~10 사이 관리)
    `elapsed_days` INT DEFAULT 0,        -- 마지막 복습 후 경과일
    `scheduled_days` INT DEFAULT 0,      -- 다음 복습까지의 간격 (오차 보정용)
    `reps` INT DEFAULT 0,                -- 전체 복습 횟수
    `lapses` INT DEFAULT 0,              -- 잊어버린(틀린) 횟수
    `state` INT DEFAULT 0,               -- 상태 (0:New, 1:Learning, 2:Review, 3:Relearning)
    
    -- V15 추가: 상태 추적 고도화
    `last_elapsed_days` INT DEFAULT 0,
    `last_response_time_ms` BIGINT DEFAULT 0,
    `retrievability` DOUBLE DEFAULT 0.0,
    
    -- 시간 관련
    `last_reviewed_at` TIMESTAMP NULL,
    `next_review_at` TIMESTAMP NULL,     -- 복습 예정일
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- [제약 조건] 한 사용자가 동일 문제를 중복 관리하지 않도록 방지
    UNIQUE KEY `uk_user_content` (`user_id`, `content_type`, `content_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    -- 오늘 복습할 문제를 찾기 위한 성능 최적화 인덱스
    INDEX `idx_user_next_review` (`user_id`, `next_review_at`)
);

CREATE TABLE IF NOT EXISTS `user_review_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `review_item_id` BIGINT NOT NULL,
    `is_correct` BOOLEAN NOT NULL COMMENT '정답 여부',
    `response_time_ms` BIGINT NOT NULL COMMENT '응답 시간(ms)',
    `stability` DOUBLE NOT NULL,
    `difficulty` DOUBLE NOT NULL,
    `reviewed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`review_item_id`) REFERENCES `user_review_items`(`id`) ON DELETE CASCADE,
    INDEX `idx_review_log_item` (`review_item_id`, `reviewed_at`)
) COMMENT '사용자 개입 없는 순수 학습 이력 로그';

-- 6-6. 코스 퀴즈 (세션형 학습)
CREATE TABLE IF NOT EXISTS `quiz_course` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL UNIQUE, -- JAVA, CS_BASIC 등
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `badge_code` VARCHAR(50),
    `total_sections` INT DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `quiz_course_section` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `course_id` BIGINT NOT NULL,
    `section_number` INT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `total_questions` INT DEFAULT 0,
    `pass_score` INT DEFAULT 70,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`course_id`) REFERENCES `quiz_course`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_course_section` (`course_id`, `section_number`)
);

CREATE TABLE IF NOT EXISTS `quiz_course_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `section_id` BIGINT NOT NULL,
    `question_number` INT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('MULTIPLE_CHOICE', 'SHORT_ANSWER', 'MULTIPLE_CHOICE_MULTIPLE') DEFAULT 'MULTIPLE_CHOICE',
    `options` JSON,
    `correct_answer` VARCHAR(500) NOT NULL,
    `explanation` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`section_id`) REFERENCES `quiz_course_section`(`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `user_course_progress` (
    `user_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `current_section` INT DEFAULT 1,
    `completed_sections` INT DEFAULT 0,
    `is_completed` BOOLEAN DEFAULT FALSE,
    `completed_at` TIMESTAMP NULL,
    `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `course_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`course_id`) REFERENCES `quiz_course`(`id`) ON DELETE CASCADE
);

-- 코스 시도 및 문제 (낙관적 잠금 version 및 복합키 FK 적용)
CREATE TABLE IF NOT EXISTS `user_section_attempt` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `quiz_course_id` BIGINT NOT NULL,
    `section_number` INT NOT NULL,
    `score` INT DEFAULT 0,
    `correct_count` INT DEFAULT 0,
    `total_questions` INT DEFAULT 0,
    `is_passed` BOOLEAN DEFAULT FALSE,
    `status` ENUM('IN_PROGRESS', 'SUBMITTED', 'ABANDONED') DEFAULT 'IN_PROGRESS',
    `version` BIGINT NOT NULL DEFAULT 0, -- 낙관적 잠금용
    `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `completed_at` TIMESTAMP NULL,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    -- 복합키 참조 (V5 대응)
    FOREIGN KEY (`quiz_course_id`, `section_number`) REFERENCES `quiz_course_section`(`course_id`, `section_number`) ON DELETE CASCADE,
    INDEX `idx_attempt_user_section_status` (`user_id`, `quiz_course_id`, `section_number`, `status`)
);

CREATE TABLE IF NOT EXISTS `user_section_attempt_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `attempt_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `order_index` INT NOT NULL,
    `user_answer` VARCHAR(500),
    `is_correct` BOOLEAN,
    `response_time_ms` BIGINT DEFAULT 0 COMMENT '응답 시간(ms)',
    `version` BIGINT NOT NULL DEFAULT 0, -- 낙관적 잠금용
    `answered_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`attempt_id`) REFERENCES `user_section_attempt`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_id`) REFERENCES `quiz_course_question`(`id`),
    UNIQUE KEY `uk_attempt_question` (`attempt_id`, `question_id`),
    INDEX `idx_attempt_question_order` (`attempt_id`, `order_index`)
) COMMENT '코스 퀴즈 섹션별 문제 답변 기록';



-- =============================================
-- 7. 게이미피케이션
-- =============================================

CREATE TABLE `daily_contribution` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `contribution_date` DATE NOT NULL,
    `has_activity` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_date` (`user_id`, `contribution_date`)
);

CREATE TABLE `contribution_detail` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `contribution_date` DATE NOT NULL,
    `activity_type` ENUM('STUDY_ATTENDANCE', 'QUIZ_CONTEST') NOT NULL,
    `reference_id` BIGINT NOT NULL,
    `reference_name` VARCHAR(200) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE `user_stats` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `level` INT DEFAULT 1,
    `level_name` VARCHAR(50) DEFAULT '새싹',
    `total_activity_days` INT DEFAULT 0,
    `current_streak` INT DEFAULT 0,
    `max_streak` INT DEFAULT 0,
    `last_activity_date` DATE,
    `total_studies_joined` INT DEFAULT 0,
    `total_studies_led` INT DEFAULT 0,
    `total_chat_count` INT DEFAULT 0,
    `total_quiz_count` INT DEFAULT 0,
    `total_materials_uploaded` INT DEFAULT 0,
    `total_retrospectives` INT DEFAULT 0,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE `level_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `level` INT NOT NULL UNIQUE,
    `level_name` VARCHAR(50) NOT NULL,
    `required_exp` INT NOT NULL,
    `level_icon_url` VARCHAR(500),
    `level_color` VARCHAR(20),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `reward_policy` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `action_type` ENUM('SIGNUP', 'DAILY_LOGIN', 'STUDY_CREATE', 'STUDY_JOIN', 'STUDY_COMPLETE', 'QUIZ_SOLVE', 'QUIZ_CREATE', 'REVIEW_WRITE', 'COMMENT_WRITE', 'ATTENDANCE') NOT NULL,
    `exp_amount` INT NOT NULL,
    `point_amount` INT NOT NULL,
    `description` VARCHAR(200),
    `is_active` BOOLEAN DEFAULT TRUE,
    `daily_limit` INT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `exp_transaction` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `exp_amount` INT NOT NULL,
    `exp_type` ENUM('SIGNUP', 'DAILY_LOGIN', 'STUDY_CREATE', 'STUDY_JOIN', 'STUDY_COMPLETE', 'QUIZ_SOLVE', 'QUIZ_CREATE', 'REVIEW_WRITE', 'COMMENT_WRITE', 'ATTENDANCE', 'LEVEL_UP', 'EVENT', 'ADMIN_GRANT', 'PENALTY') NOT NULL,
    `reference_type` ENUM('STUDY', 'QUIZ', 'REVIEW', 'COMMENT', 'ATTENDANCE', 'NONE') DEFAULT 'NONE',
    `reference_id` BIGINT,
    `description` VARCHAR(200),
    `balance_after` INT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE `point_transaction` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `point_amount` INT NOT NULL,
    `transaction_type` ENUM('EARN', 'SPEND') NOT NULL,
    `point_type` ENUM('SIGNUP', 'DAILY_LOGIN', 'STUDY_CREATE', 'STUDY_JOIN', 'STUDY_COMPLETE', 'QUIZ_SOLVE', 'QUIZ_CREATE', 'REVIEW_WRITE', 'COMMENT_WRITE', 'ATTENDANCE', 'LEVEL_UP', 'EVENT', 'ADMIN_GRANT', 'HINT_USE', 'ITEM_PURCHASE') NOT NULL,
    `reference_type` ENUM('STUDY', 'QUIZ', 'REVIEW', 'COMMENT', 'ATTENDANCE', 'HINT', 'ITEM', 'NONE') DEFAULT 'NONE',
    `reference_id` BIGINT,
    `description` VARCHAR(200),
    `balance_after` INT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE `hint_cost` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `hint_type` VARCHAR(50) NOT NULL,
    `cost_points` INT NOT NULL,
    `description` VARCHAR(200),
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 뱃지 정의 (영구, 긍정적 - 한번 획득하면 유지)
CREATE TABLE `badge` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL UNIQUE,          -- FIRST_ACTIVITY, STREAK_7 등
    `name` VARCHAR(100) NOT NULL,                -- 첫 발걸음, 일주일 연속 등
    `description` VARCHAR(200) NOT NULL,
    `icon` VARCHAR(10),
    `category` ENUM('ACTIVITY', 'STREAK', 'STUDY', 'ATTENDANCE', 'PARTICIPATION', 'QUIZ', 'MASTER', 'SPECIAL') NOT NULL,
    `condition_type` VARCHAR(50) NOT NULL,
    `condition_value` INT NOT NULL,
    `sort_order` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 사용자 뱃지 (획득 기록)
CREATE TABLE `user_badge` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `badge_id` BIGINT NOT NULL,
    `badge_type` ENUM('ACTIVITY', 'STREAK', 'STUDY', 'QUIZ_KING', 'SPECIAL'),
    `reference_type` ENUM('CONTEST', 'STUDY', 'GENERAL'),
    `reference_id` BIGINT,
    `rank` INT,                                  -- 퀴즈왕 순위 (1, 2, 3)
    `earned_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`badge_id`) REFERENCES `badge`(`id`),
    UNIQUE KEY `uk_user_badge` (`user_id`, `badge_id`),
    INDEX `idx_user_badge_type` (`user_id`, `badge_type`, `reference_type`)
);

-- 패널티 정의 (일시적, 부정적 - 해소 가능)
CREATE TABLE `penalty` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL UNIQUE,          -- THREE_DAY_QUIT, GHOST 등
    `name` VARCHAR(100) NOT NULL,                -- 작심삼일, 유령회원 등
    `description` VARCHAR(200) NOT NULL,
    `icon` VARCHAR(10),
    `grant_condition` VARCHAR(200),              -- 부여 조건 설명
    `removal_condition` VARCHAR(200),            -- 해소 조건 설명
    `removal_required` INT,                      -- 해소에 필요한 횟수
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 사용자 패널티 (일시적, 해소 가능)
CREATE TABLE `user_penalty` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `penalty_id` BIGINT NOT NULL,
    `study_id` BIGINT,                           -- 스터디별 패널티인 경우
    `is_active` BOOLEAN DEFAULT TRUE,
    `removal_progress` INT DEFAULT 0,            -- 해소 진행률 (출석 횟수 등)
    `granted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `removed_at` TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`penalty_id`) REFERENCES `penalty`(`id`),
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE SET NULL
);

-- =============================================
-- 8. 자료실
-- =============================================

CREATE TABLE `material` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `uploader_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `material_type` ENUM('LINK', 'FILE', 'IMAGE', 'VIDEO') NOT NULL,
    `url` VARCHAR(500),
    `file_path` VARCHAR(500),
    `file_size` BIGINT,
    `week_number` INT,                           -- 커리큘럼 주차
    `view_count` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`uploader_id`) REFERENCES `user`(`id`)
);

CREATE TABLE `progress` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `curriculum_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `is_completed` BOOLEAN DEFAULT FALSE,
    `completed_at` TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`curriculum_id`) REFERENCES `curriculum`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    UNIQUE KEY `uk_curriculum_user` (`curriculum_id`, `user_id`)
);

-- =============================================
-- 9. 회고
-- =============================================

CREATE TABLE `retrospective` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `session_id` BIGINT,
    `created_by` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `retrospective_type` ENUM('KPT', 'FREE') DEFAULT 'KPT',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`session_id`) REFERENCES `study_session`(`id`),
    FOREIGN KEY (`created_by`) REFERENCES `user`(`id`)
);

CREATE TABLE `retrospective_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `retrospective_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `category` ENUM('KEEP', 'PROBLEM', 'TRY') NOT NULL,  -- KPT
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`retrospective_id`) REFERENCES `retrospective`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- =============================================
-- 10. 알림
-- =============================================

CREATE TABLE `notification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` ENUM('CHAT', 'SCHEDULE', 'ATTENDANCE', 'STUDY_UPDATE', 'STUDY_APPLICATION', 'QUIZ', 'SYSTEM') NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `reference_type` VARCHAR(50),                -- study, meeting, quiz_contest 등
    `reference_id` BIGINT,
    `is_read` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE TABLE `notification_setting` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `notification_type` VARCHAR(50) NOT NULL,
    `is_enabled` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_type` (`user_id`, `notification_type`)
);

CREATE TABLE `fcm_token` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(500) NOT NULL,
    `device_type` ENUM('ANDROID', 'IOS', 'WEB') NOT NULL,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

-- =============================================
-- 11. SSAFY 보고서
-- =============================================

CREATE TABLE `report` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `report_type` ENUM('APPLICATION', 'MONTHLY_RESULT') NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` JSON,                              -- 보고서 내용 (자동 생성)
    `report_month` DATE,                         -- 월별 보고서인 경우
    `status` ENUM('DRAFT', 'SUBMITTED') DEFAULT 'DRAFT',
    `created_by` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by`) REFERENCES `user`(`id`)
);

-- =============================================
-- 12. AI 피드백
-- =============================================

CREATE TABLE `ai_feedback` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `feature_type` VARCHAR(50) NOT NULL,         -- summary, action_item, quiz, etc.
    `reference_id` BIGINT,
    `feedback` ENUM('POSITIVE', 'NEGATIVE') NOT NULL,
    `comment` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- =============================================
-- 13. 데일리
-- =============================================

CREATE TABLE `daily_report` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `report_date` DATE NOT NULL,
    `summary` TEXT,                              -- AI 생성 요약
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`study_id`) REFERENCES `study`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_study_date` (`study_id`, `report_date`)
);

CREATE TABLE `daily_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `daily_report_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `category` ENUM('YESTERDAY', 'TODAY', 'BLOCKER') NOT NULL,  -- 어제 한 일/오늘 할 일/블로커
    `content` TEXT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`daily_report_id`) REFERENCES `daily_report`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- =============================================
-- 14. 자유게시판
-- =============================================

CREATE TABLE `board_post` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `category` ENUM('PROJECT', 'COMPETITION', 'JOB', 'FREE') NOT NULL,  -- 프로젝트/공모전/채용/자유
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `view_count` INT DEFAULT 0,
    `like_count` INT DEFAULT 0,
    `comment_count` INT DEFAULT 0,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE `board_comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT,                          -- 대댓글인 경우 부모 댓글 ID
    `content` TEXT NOT NULL,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`post_id`) REFERENCES `board_post`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`parent_id`) REFERENCES `board_comment`(`id`) ON DELETE CASCADE
);

CREATE TABLE `board_like` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`post_id`) REFERENCES `board_post`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
);

-- =============================================
-- 15. 팀원모집
-- =============================================

CREATE TABLE `team_recruit` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,                   -- 작성자
    `category` ENUM('HACKATHON', 'PROJECT', 'COMPETITION') NOT NULL,  -- 해커톤/프로젝트/공모전
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `required_roles` JSON,                       -- 필요 역할 (["백엔드", "프론트엔드", "디자이너"])
    `tech_stack` JSON,                           -- 기술 스택 (["Java", "React", "Spring"])
    `max_members` INT DEFAULT 5,
    `current_members` INT DEFAULT 1,
    `deadline` DATE,                             -- 모집 마감일
    `start_date` DATE,                           -- 프로젝트 시작 예정일
    `duration` VARCHAR(100),                     -- 예상 기간 (ex: 2개월)
    `meeting_type` ENUM('ONLINE', 'OFFLINE', 'HYBRID') DEFAULT 'ONLINE',
    `region_id` BIGINT,                          -- 오프라인/혼합 시 지역
    `status` ENUM('RECRUITING', 'CLOSED', 'COMPLETED') DEFAULT 'RECRUITING',
    `view_count` INT DEFAULT 0,
    `is_deleted` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`region_id`) REFERENCES `region`(`id`)
);

CREATE TABLE `team_application` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `recruit_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `applied_role` VARCHAR(50),                  -- 지원 역할
    `message` TEXT,                              -- 지원 메시지
    `portfolio_url` VARCHAR(500),                -- 포트폴리오 URL
    `status` ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    `rejected_reason` VARCHAR(500),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `processed_at` TIMESTAMP,
    FOREIGN KEY (`recruit_id`) REFERENCES `team_recruit`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- =============================================
-- 16. 꼬멘틀 (IT 용어 추측 게임)
-- =============================================

CREATE TABLE `comendle_word` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `word` VARCHAR(100) NOT NULL UNIQUE,         -- IT 용어
    `hint` VARCHAR(500),                         -- 힌트
    `category` VARCHAR(50),                      -- 분류 (프로그래밍/네트워크/DB 등)
    `difficulty` ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM',
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `comendle_daily` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `word_id` BIGINT NOT NULL,
    `game_date` DATE NOT NULL UNIQUE,            -- 오늘의 단어 날짜
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`word_id`) REFERENCES `comendle_word`(`id`)
);

CREATE TABLE `comendle_attempt` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `daily_id` BIGINT NOT NULL,
    `user_id` BIGINT,                            -- NULL이면 비로그인
    `session_id` VARCHAR(100),                   -- 비로그인 사용자 세션
    `is_solved` BOOLEAN DEFAULT FALSE,
    `guess_count` INT DEFAULT 0,
    `solved_at` TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`daily_id`) REFERENCES `comendle_daily`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

CREATE TABLE `comendle_guess` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `attempt_id` BIGINT NOT NULL,
    `guess_number` INT NOT NULL,                 -- 시도 순번
    `guessed_word` VARCHAR(100) NOT NULL,
    `similarity` DECIMAL(5,2),                   -- 유사도 점수 (0~100)
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`attempt_id`) REFERENCES `comendle_attempt`(`id`) ON DELETE CASCADE
);

CREATE TABLE `comendle_streak` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `current_streak` INT DEFAULT 0,
    `max_streak` INT DEFAULT 0,
    `total_solved` INT DEFAULT 0,
    `total_played` INT DEFAULT 0,
    `last_played_date` DATE,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

-- =============================================
-- 17. IT 뉴스
-- =============================================

CREATE TABLE `it_news` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(300) NOT NULL,
    `summary` TEXT,
    `source_url` VARCHAR(500) NOT NULL,
    `source_name` VARCHAR(100),                  -- 출처 (velog, tistory, medium 등)
    `thumbnail_url` VARCHAR(500),
    `category` VARCHAR(50),                      -- 분류 (AI/백엔드/프론트엔드/DevOps 등)
    `published_at` TIMESTAMP,
    `view_count` INT DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `news_bookmark` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `news_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`news_id`) REFERENCES `it_news`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_news` (`user_id`, `news_id`)
);
-- ===== Quartz Scheduler 테이블 생성 =====

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

CREATE TABLE QRTZ_JOB_DETAILS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME VARCHAR(250) NOT NULL,
    IS_DURABLE VARCHAR(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    JOB_NAME VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT(13) NULL,
    PREV_FIRE_TIME BIGINT(13) NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT(13) NOT NULL,
    END_TIME BIGINT(13) NULL,
    CALENDAR_NAME VARCHAR(200) NULL,
    MISFIRE_INSTR SMALLINT(2) NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP) 
        REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    REPEAT_COUNT BIGINT(7) NOT NULL,
    REPEAT_INTERVAL BIGINT(12) NOT NULL,
    TIMES_TRIGGERED BIGINT(10) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) 
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) 
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) 
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_BLOB_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) 
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_CALENDARS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    FIRED_TIME BIGINT(13) NOT NULL,
    SCHED_TIME BIGINT(13) NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(200) NULL,
    JOB_GROUP VARCHAR(200) NULL,
    IS_NONCONCURRENT VARCHAR(1) NULL,
    REQUESTS_RECOVERY VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
    CHECKIN_INTERVAL BIGINT(13) NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS(SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS(SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS(SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS(SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS(SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS(SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, TRIGGER_GROUP);
