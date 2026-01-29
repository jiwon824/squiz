-- ============================================================================
-- V14: 기존 오답노트(wrong_answer_note)를 FSRS 기반 통합 복습 시스템으로 전환
-- ============================================================================
DROP TABLE wrong_answer_note;

CREATE TABLE IF NOT EXISTS `user_review_items` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    
    -- 다형성 구조 (두 종류의 퀴즈 수용)
    `content_type` VARCHAR(20) NOT NULL, -- 'STATIC_COURSE' 또는 'AI_GENERATED'
    `content_id` BIGINT NOT NULL,        -- 해당 퀴즈 테이블의 PK
    
    -- FSRS 핵심 변수
    `stability` DOUBLE DEFAULT 0.0,      -- 기억 안정성 (S)
    `difficulty` DOUBLE DEFAULT 5.0,     -- 난이도 (D)
    `elapsed_days` INT DEFAULT 0,        -- 마지막 복습 후 경과일
    `scheduled_days` INT DEFAULT 0,      -- 다음 복습까지의 간격
    `reps` INT DEFAULT 0,                -- 전체 복습 횟수 (기존 review_count 역할)
    `lapses` INT DEFAULT 0,              -- 잊어버린(틀린) 횟수
    `state` INT DEFAULT 0,               -- 상태 (0:New, 1:Learning, 2:Review, 3:Relearning)
    
    -- 시간 관련
    `last_reviewed_at` TIMESTAMP NULL,
    `next_review_at` TIMESTAMP NULL,     -- 복습 예정일 (Index 필요)
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
-- [제약 조건] 한 사용자가 동일 문제를 중복 관리하지 않도록 방지
    UNIQUE KEY `uk_user_content` (`user_id`, `content_type`, `content_id`),

    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
    -- content_id는 소스가 두 개이므로 물리적 FK 대신 애플리케이션 로직에서 관리
);

-- 오늘 복습할 문제를 찾기 위한 성능 최적화 인덱스
CREATE INDEX idx_user_next_review ON user_review_items(user_id, next_review_at);

-- ============================================================================
-- 컬럼 설명:
-- DOUBLE 타입으로 만든 이유: FSRS 공식은 지수 함수를 사용하므로 정밀한 연산이 필요
-- `difficulty`: FSRS에서 난이도는 보통 1~10 사이로 관리되며, 중간값인 5.0에서 시작하여 사용자가 "쉽다"고 하면 낮추고 "어렵다"고 하면 높이는 방식이 안정적
-- `scheduled_days`: 단순히 "며칠 뒤"라는 정보뿐만 아니라, 나중에 사용자가 "원래 10일 뒤에 풀기로 한 문제를 12일 만에 풀었을 때" 발생하는 오차를 보정하는 파라미터로 활용
--`state`: 부드러운 학습 흐름을 만들려면 "방금 틀려서 다시 배우는 중(Relearning)"인지 "완전히 익혀서 장기 복습 중(Review)"인지를 구분하는 이 컬럼이 매우 유용
-- 
-- [제약 조건]
-- 데이터베이스 이론상으로는 (user_id, content_type, content_id)를 PK로 잡아도 무방
-- 하지만 실무(특히 Spring Boot/JPA 환경)에서는 다음과 같은 이유로 인공 키(id)를 PK로 두고 나머지를 UNIQUE로 묶는 방식을 선호
-- 1. JPA 편의성: JPA에서 복합키를 PK로 쓰려면 별도의 `@IdClass`나 `@EmbeddedId` 클래스를 선언해야 해서 코드가 복잡해집니다. `id` 하나를 PK로 쓰면 훨씬 간결해집니다.
-- 2. 인덱스 효율: 외래 키로 이 테이블을 참조할 일이 생길 경우, 복합키보다는 단일 `BIGINT` 값을 참조하는 것이 성능상 유리합니다.
-- ============================================================================