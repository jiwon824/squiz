-- ============================================================================
-- V13: 낙관적 잠금(Optimistic Locking)을 위한 version 컬럼 추가
-- ============================================================================
-- 목적: 동시 수정 충돌 방지
-- 대상 테이블:
--   - user_section_attempt: 퀴즈 시도 엔티티
--   - user_section_attempt_question: 시도별 문제 엔티티 (답안 저장 시 충돌 방지)
-- ============================================================================

-- user_section_attempt 테이블에 version 컬럼 추가
ALTER TABLE user_section_attempt
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- user_section_attempt_question 테이블에 version 컬럼 추가
ALTER TABLE user_section_attempt_question
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- ============================================================================
-- 기술적 설명:
--
-- Hibernate의 @Version은 UPDATE 쿼리 실행 시 WHERE 절에 version을 포함합니다:
--   UPDATE user_section_attempt_question
--   SET user_answer = ?, version = version + 1
--   WHERE id = ? AND version = ?
--
-- 두 트랜잭션이 동시에 같은 row를 수정하려 할 때:
-- 1. Transaction A: SELECT ... WHERE id = 1 (version = 0)
-- 2. Transaction B: SELECT ... WHERE id = 1 (version = 0)
-- 3. Transaction A: UPDATE ... WHERE id = 1 AND version = 0 → 성공, version = 1
-- 4. Transaction B: UPDATE ... WHERE id = 1 AND version = 0 → 실패 (0 rows affected)
-- 5. Hibernate throws ObjectOptimisticLockingFailureException
--
-- 이를 통해 "Lost Update" 문제를 방지합니다.
-- ============================================================================
