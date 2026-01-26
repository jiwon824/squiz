-- =============================================
-- V5: 스터디 퀴즈 및 코스 퀴즈 진행 상태 추적 기능 추가
-- - 스터디 퀴즈: 이어풀기 / 처음부터 다시풀기 지원
-- - 코스 퀴즈: 복합키 적용 및 상태 추적 (UserSectionAttempt Entity 대응)
-- =============================================

-- 0. 외래키 체크 임시 비활성화 (안전한 스키마 변경을 위해)
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 1. Study Quiz Changes (Existing V5 Logic)
-- =============================================

-- 1. study_quiz_attempt 테이블에 진행 상태 컬럼 추가 (컬럼이 없을 때만)
SET @has_status := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_quiz_attempt'
      AND COLUMN_NAME = 'status'
);
SET @sql := IF(
    @has_status = 0,
    'ALTER TABLE `study_quiz_attempt` ADD COLUMN `status` ENUM(''IN_PROGRESS'', ''COMPLETED'', ''ABANDONED'') DEFAULT ''IN_PROGRESS''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_started_at := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_quiz_attempt'
      AND COLUMN_NAME = 'started_at'
);
SET @sql := IF(
    @has_started_at = 0,
    'ALTER TABLE `study_quiz_attempt` ADD COLUMN `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_last_answered_at := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_quiz_attempt'
      AND COLUMN_NAME = 'last_answered_at'
);
SET @sql := IF(
    @has_last_answered_at = 0,
    'ALTER TABLE `study_quiz_attempt` ADD COLUMN `last_answered_at` TIMESTAMP NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_current_question_index := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_quiz_attempt'
      AND COLUMN_NAME = 'current_question_index'
);
SET @sql := IF(
    @has_current_question_index = 0,
    'ALTER TABLE `study_quiz_attempt` ADD COLUMN `current_question_index` INT DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- 1.1 study_quiz_attempt 테이블에 진행 상태 컬럼 추가
-- (컬럼이 이미 존재하는지 체크하는 로직은 생략하고, Flyway 관례따라 변경)
ALTER TABLE `study_quiz_attempt`
ADD COLUMN `status` ENUM('IN_PROGRESS', 'COMPLETED', 'ABANDONED') DEFAULT 'IN_PROGRESS',
ADD COLUMN `started_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN `last_answered_at` TIMESTAMP NULL,
ADD COLUMN `current_question_index` INT DEFAULT 0;

-- 기존 데이터는 완료된 것으로 처리
SET @has_completed_at := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'study_quiz_attempt'
      AND COLUMN_NAME = 'completed_at'
);
SET @sql := IF(
    @has_completed_at > 0,
    'UPDATE `study_quiz_attempt` SET `status` = ''COMPLETED'' WHERE `completed_at` IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.2 스터디 퀴즈 개별 답변 기록 테이블 생성
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

-- 1.3 인덱스 추가 (이어풀기 조회 최적화)
CREATE INDEX `idx_quiz_attempt_status` ON `study_quiz_attempt` (`quiz_id`, `status`);


-- =============================================
-- 2. Course Quiz Changes (UserSectionAttempt Fixes)
-- =============================================

-- 2.1 user_section_attempt 테이블 스키마 변경
-- status, quiz_course_id, section_number, completed_at 추가
ALTER TABLE `user_section_attempt`
ADD COLUMN `status` ENUM('IN_PROGRESS', 'SUBMITTED', 'ABANDONED') DEFAULT 'IN_PROGRESS',
ADD COLUMN `quiz_course_id` BIGINT NULL,
ADD COLUMN `section_number` INT NULL,
ADD COLUMN `completed_at` TIMESTAMP NULL;

-- 2.2 데이터 마이그레이션 (section_id -> quiz_course_id, section_number)
-- A. quiz_course_section 테이블(id, course_id, section_number) 참조하여 데이터 이관
UPDATE `user_section_attempt` u
JOIN `quiz_course_section` s ON u.section_id = s.id
SET u.quiz_course_id = s.course_id,
    u.section_number = s.section_number;

-- 2.3 컬럼 속성 변경 (NOT NULL)
ALTER TABLE `user_section_attempt`
MODIFY COLUMN `quiz_course_id` BIGINT NOT NULL,
MODIFY COLUMN `section_number` INT NOT NULL;

-- 2.4 기존 FK 및 컬럼 삭제 (section_id)
-- FK 이름을 알 수 없으므로 information_schema 조회 후 삭제
SET @fk_name := (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                 WHERE TABLE_NAME = 'user_section_attempt'
                 AND COLUMN_NAME = 'section_id'
                 AND REFERENCED_TABLE_NAME = 'quiz_course_section'
                 AND TABLE_SCHEMA = DATABASE()
                 LIMIT 1);

SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE `user_section_attempt` DROP FOREIGN KEY `', @fk_name, '`'),
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 컬럼 삭제
ALTER TABLE `user_section_attempt` DROP COLUMN `section_id`;

-- 2.4.1 ERROR FIX: course_id 컬럼이 존재한다면 삭제 (GenericJDBCException 방지)
-- 만약 course_id 컬럼이 있다면 삭제
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'user_section_attempt'
                   AND COLUMN_NAME = 'course_id');

SET @sql = IF(@col_exists > 0,
              'ALTER TABLE `user_section_attempt` DROP COLUMN `course_id`',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.5 새로운 복합키 FK 추가
-- quiz_course_section 테이블의 (course_id, section_number) 복합키 참조
-- V1 기준 quiz_course_section의 course_id가 Entity의 quiz_course_id에 대응
ALTER TABLE `user_section_attempt`
ADD CONSTRAINT `fk_user_section_attempt_composite`
FOREIGN KEY (`quiz_course_id`, `section_number`) REFERENCES `quiz_course_section`(`course_id`, `section_number`)
ON DELETE CASCADE;

-- 2.6 인덱스 추가 (Entity @Index 대응)
CREATE INDEX `idx_attempt_user_section_status` ON `user_section_attempt` (`user_id`, `quiz_course_id`, `section_number`, `status`);


-- =============================================
-- 3. UserSectionAttemptQuestion Table (Entity 대응)
-- =============================================

CREATE TABLE IF NOT EXISTS `user_section_attempt_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `attempt_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `order_index` INT NOT NULL,
    `user_answer` VARCHAR(500),
    `is_correct` BOOLEAN,
    `answered_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`attempt_id`) REFERENCES `user_section_attempt`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`question_id`) REFERENCES `quiz_course_question`(`id`),
    UNIQUE KEY `uk_attempt_question` (`attempt_id`, `question_id`),
    INDEX `idx_attempt_question_order` (`attempt_id`, `order_index`)
) COMMENT '코스 퀴즈 섹션별 문제 답변 기록';

-- 11. 외래키 체크 재활성화
SET FOREIGN_KEY_CHECKS = 1;
