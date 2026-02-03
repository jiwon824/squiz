-- =============================================
-- 미팅 ID 124 관련 스터디 퀴즈 데이터
-- =============================================

-- 1. study_quiz (퀴즈 기본 정보)
-- source_type: MEETING, source_id: 124 (미팅 ID)
INSERT INTO study_quiz (id, study_id, session_id, title, source_type, source_id, status, created_at)
VALUES
(1001, 1, NULL, '미팅 124 복습 퀴즈', 'MEETING', 124, 'ACTIVE', NOW());

-- 2. study_quiz_question (퀴즈 문제들)
INSERT INTO study_quiz_question (id, quiz_id, question_text, question_type, options, correct_answer, explanation, created_at)
VALUES
-- 객관식 문제 1
(10001, 1001,
 'Spring Boot에서 의존성 주입(DI)을 위해 사용하는 어노테이션은?',
 'MULTIPLE_CHOICE',
 '[{"id": "A", "text": "@Autowired"}, {"id": "B", "text": "@Component"}, {"id": "C", "text": "@Service"}, {"id": "D", "text": "@Repository"}]',
 'A',
 '@Autowired는 스프링에서 의존성을 자동으로 주입하기 위해 사용하는 어노테이션입니다. @Component, @Service, @Repository는 빈 등록용 어노테이션입니다.',
 NOW()),

-- 객관식 문제 2
(10002, 1001,
 'JPA에서 영속성 컨텍스트의 1차 캐시에 대한 설명으로 올바른 것은?',
 'MULTIPLE_CHOICE',
 '[{"id": "A", "text": "애플리케이션 전체에서 공유된다"}, {"id": "B", "text": "트랜잭션 범위 내에서만 유효하다"}, {"id": "C", "text": "데이터베이스에 직접 저장된다"}, {"id": "D", "text": "Redis와 연동된다"}]',
 'B',
 '1차 캐시는 영속성 컨텍스트 내부에 존재하며, 트랜잭션이 끝나면 함께 소멸됩니다. 애플리케이션 전체에서 공유되는 것은 2차 캐시입니다.',
 NOW()),

-- 객관식 문제 3
(10003, 1001,
 'REST API에서 리소스 생성에 사용하는 HTTP 메서드는?',
 'MULTIPLE_CHOICE',
 '[{"id": "A", "text": "GET"}, {"id": "B", "text": "POST"}, {"id": "C", "text": "PUT"}, {"id": "D", "text": "DELETE"}]',
 'B',
 'POST는 새로운 리소스를 생성할 때 사용합니다. GET은 조회, PUT은 수정, DELETE는 삭제에 사용됩니다.',
 NOW()),

-- 객관식 문제 4
(10004, 1001,
 'React에서 컴포넌트의 상태를 관리하기 위해 사용하는 Hook은?',
 'MULTIPLE_CHOICE',
 '[{"id": "A", "text": "useEffect"}, {"id": "B", "text": "useState"}, {"id": "C", "text": "useContext"}, {"id": "D", "text": "useRef"}]',
 'B',
 'useState는 함수형 컴포넌트에서 상태를 관리하기 위한 Hook입니다. useEffect는 사이드 이펙트 처리, useContext는 컨텍스트 사용, useRef는 DOM 참조나 값 보존에 사용됩니다.',
 NOW()),

-- 단답형 문제 5
(10005, 1001,
 'Git에서 원격 저장소의 변경사항을 로컬에 가져오면서 자동으로 병합하는 명령어는?',
 'SHORT_ANSWER',
 NULL,
 'pull',
 'git pull은 git fetch와 git merge를 합친 명령어로, 원격 저장소의 변경사항을 가져와 현재 브랜치에 병합합니다.',
 NOW());

-- 3. study_quiz_attempt (퀴즈 시도 기록 - 사용자 10000이 풀었다고 가정)
INSERT INTO study_quiz_attempt (id, quiz_id, user_id, score, total_questions, correct_count, status, started_at, completed_at, current_question_index)
VALUES
(5001, 1001, 10000, 80, 5, 4, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 30 MINUTE), 5);

-- 4. study_quiz_answer (퀴즈 답변 기록)
INSERT INTO study_quiz_answer (id, attempt_id, question_id, question_index, user_answer, is_correct, response_time_ms, answered_at)
VALUES
(50001, 5001, 10001, 0, '"A"', 1, 15000, DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
(50002, 5001, 10002, 1, '"B"', 1, 20000, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(50003, 5001, 10003, 2, '"B"', 1, 12000, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(50004, 5001, 10004, 3, '"A"', 0, 18000, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),  -- 오답
(50005, 5001, 10005, 4, '"pull"', 1, 25000, DATE_SUB(NOW(), INTERVAL 35 MINUTE));

-- =============================================
-- 확인 쿼리
-- =============================================
-- SELECT * FROM study_quiz WHERE source_id = 124;
-- SELECT * FROM study_quiz_question WHERE quiz_id = 1001;
-- SELECT * FROM study_quiz_attempt WHERE quiz_id = 1001;
-- SELECT * FROM study_quiz_answer WHERE attempt_id = 5001;
