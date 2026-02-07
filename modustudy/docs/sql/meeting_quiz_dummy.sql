-- =============================================
-- 미팅 퀴즈 테스트용 더미 데이터
-- 대상 유저: ID=1 (박지원, love990890@naver.com)
-- =============================================
-- 실행 순서: 외래키 의존 관계를 고려하여 순서대로 삽입
--   user(기존) → topic/format(기존) → study → study_member
--   → study_session → meeting → study_quiz → study_quiz_question
--
-- ※ 충돌 방지: ID는 9900번대를 사용합니다.
--   기존 데이터와 충돌 시 ID를 조정하세요.
-- =============================================

-- =============================================
-- 0. 변수 설정 (가독성 및 유지보수용)
-- =============================================
SET @user_id        = 1;      -- 박지원
SET @study_id       = 9901;
SET @session_id     = 9901;
SET @meeting_id     = 9901;
SET @quiz_id        = 9901;

-- =============================================
-- 0-1. 필수 참조 데이터 (topic, format)
--      이미 존재하면 무시됩니다.
-- =============================================
INSERT IGNORE INTO topic (id, name, parent_id, sort_order) VALUES (3, '프론트엔드', NULL, 3);
INSERT IGNORE INTO topic (id, name, parent_id, sort_order) VALUES (34, 'React', 3, 4);
INSERT IGNORE INTO format (id, name, description, sort_order) VALUES (1, '문제 풀이', '알고리즘, 코딩테스트 문제를 함께 풀고 리뷰합니다', 1);

-- 실제 DB에 존재하는 ID를 동적으로 조회 (UNIQUE name 기준)
SET @topic_id  = (SELECT id FROM topic WHERE name = 'React' LIMIT 1);
SET @format_id = (SELECT id FROM format LIMIT 1);  -- 아무 format이나 사용 (nullable 컬럼)

-- =============================================
-- 1. 스터디 생성
-- =============================================
INSERT INTO study (
    id, leader_id, name, intro, description,
    topic_id, format_id, study_type, meeting_type,
    max_members, is_public, status,
    start_date, end_date, schedule_days,
    created_at, updated_at
) VALUES (
    @study_id, @user_id,
    'React 심화 스터디',
    'React 고급 패턴과 성능 최적화를 학습합니다',
    'React 18의 최신 기능과 디자인 패턴, 상태 관리, 테스트 전략 등을 깊이 있게 다루는 스터디입니다.',
    @topic_id, @format_id,
    'PLANNED', 'ONLINE',
    6, 1, 'IN_PROGRESS',
    '2026-01-01', '2026-03-31', '["토"]',
    NOW(), NOW()
);

-- =============================================
-- 2. 스터디 멤버 (유저를 LEADER로 등록)
-- =============================================
INSERT INTO study_member (
    study_id, user_id, role, status, joined_at, is_probation, created_at
) VALUES (
    @study_id, @user_id, 'LEADER', 'APPROVED', NOW(), 0, NOW()
);

-- =============================================
-- 3. 스터디 세션 (미팅의 상위 개념, 선택적)
-- =============================================
INSERT INTO study_session (
    id, study_id, session_number, title, description,
    scheduled_at, duration_minutes, is_online, status,
    completed_at, created_at
) VALUES (
    @session_id, @study_id, 1,
    '1회차: React 렌더링 최적화',
    'useMemo, useCallback, React.memo 등 렌더링 최적화 기법 학습',
    '2026-02-05 14:00:00', 90, 1, 'COMPLETED',
    '2026-02-05 15:30:00', NOW()
);

-- =============================================
-- 4. 미팅 (STT 완료 상태)
-- =============================================
INSERT INTO meeting (
    id, study_id, session_id, title,
    meeting_type, status,
    started_at, ended_at, duration_seconds,
    participant_count,
    recording_status, stt_status, summary_status,
    created_at, updated_at
) VALUES (
    @meeting_id, @study_id, @session_id,
    'React 렌더링 최적화 미팅',
    'WEEKLY', 'ENDED',
    '2026-02-05 14:00:00', '2026-02-05 15:30:00', 5400,
    3,
    'READY', 'DONE', 'DONE',
    NOW(), NOW()
);

-- =============================================
-- 5. 스터디 퀴즈 (미팅 기반 AI 생성 퀴즈)
-- =============================================
INSERT INTO study_quiz (
    id, study_id, session_id, title,
    source_type, source_id, status,
    created_at, updated_at
) VALUES (
    @quiz_id, @study_id, @session_id,
    'React 렌더링 최적화 퀴즈',
    'MEETING', @meeting_id, 'ACTIVE',
    NOW(), NOW()
);

-- =============================================
-- 6. 퀴즈 문제 (객관식 5문제 + 주관식 2문제)
-- =============================================
INSERT INTO study_quiz_question (
    id, quiz_id, question_text, question_type,
    options, correct_answer, answer_keywords, explanation,
    created_at, updated_at
) VALUES

-- [객관식 1] useMemo
(99001, @quiz_id,
 'React에서 useMemo 훅의 주요 목적은 무엇인가요?',
 'MULTIPLE_CHOICE',
 '[{"id":"A","text":"컴포넌트의 상태를 관리한다"},{"id":"B","text":"비용이 큰 계산의 결과를 메모이제이션한다"},{"id":"C","text":"DOM 요소에 직접 접근한다"},{"id":"D","text":"비동기 데이터를 가져온다"}]',
 'B', NULL,
 'useMemo는 의존성이 변경되지 않으면 이전 계산 결과를 재사용하여 불필요한 재계산을 방지합니다. 렌더링 성능 최적화에 핵심적인 Hook입니다.',
 NOW(), NOW()),

-- [객관식 2] React.memo
(99002, @quiz_id,
 'React.memo에 대한 설명으로 올바르지 않은 것은?',
 'MULTIPLE_CHOICE',
 '[{"id":"A","text":"고차 컴포넌트(HOC)이다"},{"id":"B","text":"props가 변경되지 않으면 리렌더링을 건너뛴다"},{"id":"C","text":"클래스 컴포넌트에서만 사용할 수 있다"},{"id":"D","text":"얕은 비교(shallow comparison)를 기본으로 사용한다"}]',
 'C', NULL,
 'React.memo는 함수형 컴포넌트를 위한 고차 컴포넌트입니다. 클래스 컴포넌트에서는 PureComponent나 shouldComponentUpdate를 사용합니다.',
 NOW(), NOW()),

-- [객관식 3] useCallback
(99003, @quiz_id,
 'useCallback 훅을 사용해야 하는 가장 적절한 상황은?',
 'MULTIPLE_CHOICE',
 '[{"id":"A","text":"모든 함수에 항상 적용해야 한다"},{"id":"B","text":"자식 컴포넌트에 콜백을 전달할 때 불필요한 리렌더링을 방지하기 위해"},{"id":"C","text":"API 호출을 캐싱하기 위해"},{"id":"D","text":"컴포넌트 마운트 시 한 번만 실행하기 위해"}]',
 'B', NULL,
 'useCallback은 함수 참조를 메모이제이션하여 자식 컴포넌트가 React.memo로 감싸져 있을 때 불필요한 리렌더링을 방지합니다. 모든 함수에 무분별하게 사용하면 오히려 메모리 오버헤드가 발생합니다.',
 NOW(), NOW()),

-- [객관식 4] Virtual DOM
(99004, @quiz_id,
 'React의 Virtual DOM 재조정(Reconciliation) 과정에서 사용하는 비교 알고리즘의 시간 복잡도는?',
 'MULTIPLE_CHOICE',
 '[{"id":"A","text":"O(n³)"},{"id":"B","text":"O(n²)"},{"id":"C","text":"O(n)"},{"id":"D","text":"O(log n)"}]',
 'C', NULL,
 'React는 두 가지 휴리스틱(같은 레벨의 노드만 비교, key를 통한 자식 요소 식별)을 적용하여 O(n) 시간 복잡도로 트리를 비교합니다. 일반적인 트리 diff 알고리즘은 O(n³)입니다.',
 NOW(), NOW()),

-- [객관식 5] key prop
(99005, @quiz_id,
 '리스트 렌더링에서 key prop을 올바르게 사용하는 방법은?',
 'MULTIPLE_CHOICE',
 '[{"id":"A","text":"배열의 index를 key로 사용한다"},{"id":"B","text":"Math.random()으로 생성한 값을 key로 사용한다"},{"id":"C","text":"데이터의 고유 ID를 key로 사용한다"},{"id":"D","text":"key를 생략하여 React가 자동 처리하게 한다"}]',
 'C', NULL,
 '고유하고 안정적인 ID를 key로 사용해야 React가 요소를 정확히 추적할 수 있습니다. index는 리스트 순서가 변경될 때 문제가 발생하고, random 값은 매 렌더링마다 새 key가 생겨 성능이 저하됩니다.',
 NOW(), NOW()),

-- [주관식 1] Profiler
(99006, @quiz_id,
 'React에서 컴포넌트의 렌더링 성능을 측정하기 위해 사용하는 내장 컴포넌트의 이름은?',
 'SHORT_ANSWER',
 NULL,
 'Profiler',
 '["Profiler", "React.Profiler", "프로파일러"]',
 'React.Profiler 컴포넌트를 사용하면 렌더링 빈도와 비용을 측정할 수 있습니다. onRender 콜백을 통해 각 렌더링의 소요 시간을 확인합니다.',
 NOW(), NOW()),

-- [주관식 2] lazy
(99007, @quiz_id,
 'React에서 코드 스플리팅을 위해 동적 import와 함께 사용하는 함수의 이름은? (React.___)',
 'SHORT_ANSWER',
 NULL,
 'lazy',
 '["lazy", "React.lazy"]',
 'React.lazy()는 동적 import()와 함께 사용하여 컴포넌트 레벨의 코드 스플리팅을 구현합니다. Suspense 컴포넌트와 함께 사용하여 로딩 상태를 처리합니다.',
 NOW(), NOW());

-- =============================================
-- 확인 쿼리
-- =============================================
-- SELECT * FROM study WHERE id = 9901;
-- SELECT * FROM study_member WHERE study_id = 9901;
-- SELECT * FROM meeting WHERE id = 9901;
-- SELECT * FROM study_quiz WHERE source_type = 'MEETING' AND source_id = 9901;
-- SELECT * FROM study_quiz_question WHERE quiz_id = 9901;

-- =============================================
-- 롤백 (데이터 삭제 시 역순으로 실행)
-- =============================================
-- DELETE FROM study_quiz_question WHERE quiz_id = 9901;
-- DELETE FROM study_quiz WHERE id = 9901;
-- DELETE FROM meeting WHERE id = 9901;
-- DELETE FROM study_session WHERE id = 9901;
-- DELETE FROM study_member WHERE study_id = 9901;
-- DELETE FROM study WHERE id = 9901;
