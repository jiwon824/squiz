-- =============================================================================
-- ModuStudy Quiz Course Dummy Data
-- 퀴즈 코스 테스트용 더미 데이터
-- =============================================================================
-- 실행 순서를 지켜서 실행해주세요 (외래 키 제약조건)
-- =============================================================================

-- =============================================================================
-- 1. 테스트 사용자 (Test User)
-- =============================================================================
-- 실제 user 테이블 스키마에 맞게 모든 필수 컬럼 포함
INSERT INTO `user` (
    `id`, `email`, `password`, `name`, `nickname`, `role`, `is_active`,
    `current_level`, `level_name`, `current_points`, `total_exp`,
    `is_online`, `is_searchable`, `leader_rating`, `leader_review_count`
)
VALUES 
    (1, 'test@modustudy.com', '$2a$10$dummyhashedpassword', 'Test User', 'testuser', 'USER', TRUE,
     1, '새싹', 0, 0, FALSE, TRUE, 0.0, 0),
    (2, 'admin@modustudy.com', '$2a$10$dummyhashedpassword', 'Admin User', 'adminuser', 'ADMIN', TRUE,
     1, '새싹', 0, 0, FALSE, TRUE, 0.0, 0) AS new_values
ON DUPLICATE KEY UPDATE `email` = new_values.`email`;

-- =============================================================================
-- 2. 퀴즈 코스 (Quiz Courses)
-- =============================================================================
INSERT INTO `quiz_course` (`id`, `code`, `name`, `description`, `badge_code`, `total_sections`, `is_active`, `sort_order`)
VALUES
    (1, 'OS', '운영체제 기초', '프로세스, 메모리, 파일 시스템 등 운영체제의 핵심 개념을 학습합니다.', 'OS_MASTER', 3, TRUE, 1),
    (2, 'NETWORK', '네트워크 기초', 'TCP/IP, HTTP, 소켓 통신 등 네트워크의 핵심 개념을 학습합니다.', 'NETWORK_MASTER', 3, TRUE, 2) AS new_values
ON DUPLICATE KEY UPDATE `name` = new_values.`name`;

-- =============================================================================
-- 3. 퀴즈 코스 섹션 (Quiz Course Sections)
-- =============================================================================
-- 실제 스키마: quiz_course_id(PK), section_number(PK), name, description, total_questions, pass_score
INSERT INTO `quiz_course_section` (`quiz_course_id`, `section_number`, `name`, `description`, `total_questions`, `pass_score`)
VALUES
    -- OS 코스 섹션 (quiz_course_id = 1)
    (1, 1, '프로세스 관리', '프로세스 생명주기, 스케줄링, 동기화를 학습합니다.', 10, 70),
    (1, 2, '메모리 관리', '가상 메모리, 페이징, 세그멘테이션을 학습합니다.', 10, 70),
    (1, 3, '파일 시스템', '파일 시스템 구조, 디렉토리, I/O를 학습합니다.', 10, 70),
    
    -- Network 코스 섹션 (quiz_course_id = 2)
    (2, 1, 'OSI 7계층과 TCP/IP', 'OSI 모델과 TCP/IP 프로토콜 스택을 학습합니다.', 10, 70),
    (2, 2, 'TCP와 UDP', 'TCP와 UDP의 특징과 차이점을 학습합니다.', 10, 70),
    (2, 3, 'HTTP와 웹', 'HTTP 프로토콜과 웹 통신을 학습합니다.', 10, 70) AS new_values
ON DUPLICATE KEY UPDATE `name` = new_values.`name`;

-- =============================================================================
-- 4. 퀴즈 문제 (Quiz Course Questions)
-- =============================================================================
-- 실제 스키마: id(AI PK), quiz_course_id, section_number, question_number, question_text, question_type, options, correct_answer, explanation

-- -----------------------------------------------------------------------------
-- 섹션 1: 프로세스 관리 (10문제) - section_id=1
-- -----------------------------------------------------------------------------
INSERT INTO `quiz_course_question` (`id`, `quiz_course_id`, `section_number`, `question_number`, `question_text`, `question_type`, `options`, `correct_answer`, `explanation`)
VALUES
    (1, 1, 1, 1, '프로세스의 5가지 상태 중 CPU를 할당받아 실행 중인 상태는?', 'MULTIPLE_CHOICE', 
     '[{"id":"A","text":"New"},{"id":"B","text":"Ready"},{"id":"C","text":"Running"},{"id":"D","text":"Waiting"}]',
     'C', 'Running 상태는 프로세스가 CPU를 할당받아 명령어를 실행하고 있는 상태입니다.'),
    
    (2, 1, 1, 2, '다음 중 선점형(Preemptive) 스케줄링 알고리즘은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"FCFS"},{"id":"B","text":"SJF"},{"id":"C","text":"Round Robin"},{"id":"D","text":"Priority (비선점)"}]',
     'C', 'Round Robin은 시간 할당량(Time Quantum)마다 CPU를 강제로 빼앗는 선점형 알고리즘입니다.'),
    
    (3, 1, 1, 3, '교착상태(Deadlock)가 발생하기 위한 4가지 필요조건을 모두 고르세요.', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"상호 배제"},{"id":"B","text":"점유와 대기"},{"id":"C","text":"비선점"},{"id":"D","text":"순환 대기"},{"id":"E","text":"무한 대기"}]',
     '["A","B","C","D"]', '교착상태의 4가지 필요조건: 상호 배제, 점유와 대기, 비선점, 순환 대기'),
    
    (4, 1, 1, 4, '프로세스와 스레드의 차이점으로 올바른 것은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"프로세스는 메모리를 공유하고 스레드는 독립적이다"},{"id":"B","text":"스레드는 프로세스 내에서 스택만 별도로 가진다"},{"id":"C","text":"스레드는 프로세스보다 생성 비용이 높다"},{"id":"D","text":"프로세스 간 통신이 스레드 간 통신보다 빠르다"}]',
     'B', '스레드는 프로세스 내에서 스택만 별도로 가지고, 코드/데이터/힙 영역은 공유합니다.'),
    
    (5, 1, 1, 5, '컨텍스트 스위칭(Context Switching)이란?', 'SHORT_ANSWER',
     NULL,
     'CPU가 한 프로세스에서 다른 프로세스로 전환될 때 현재 상태를 저장하고 새 프로세스의 상태를 불러오는 과정', 
     '컨텍스트 스위칭은 CPU가 프로세스 전환 시 PCB에 상태를 저장/복원하는 과정입니다.'),
    
    (6, 1, 1, 6, '세마포어(Semaphore)의 두 가지 원자적 연산은?', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"wait()"},{"id":"B","text":"signal()"},{"id":"C","text":"lock()"},{"id":"D","text":"unlock()"},{"id":"E","text":"acquire()"}]',
     '["A","B"]', '세마포어는 wait(P)와 signal(V) 두 가지 원자적 연산을 제공합니다.'),
    
    (7, 1, 1, 7, 'FCFS 스케줄링의 단점은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"기아 현상 발생"},{"id":"B","text":"호위 효과(Convoy Effect)"},{"id":"C","text":"오버헤드가 크다"},{"id":"D","text":"우선순위 역전"}]',
     'B', 'FCFS는 긴 프로세스 뒤에 짧은 프로세스들이 대기하는 호위 효과가 발생할 수 있습니다.'),
    
    (8, 1, 1, 8, '임계 구역(Critical Section) 문제 해결의 3가지 조건을 모두 고르세요.', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"상호 배제"},{"id":"B","text":"진행"},{"id":"C","text":"한정 대기"},{"id":"D","text":"선점"},{"id":"E","text":"공정성"}]',
     '["A","B","C"]', '임계 구역 해결 조건: 상호 배제(Mutual Exclusion), 진행(Progress), 한정 대기(Bounded Waiting)'),
    
    (9, 1, 1, 9, 'PCB(Process Control Block)에 저장되는 정보가 아닌 것은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"프로세스 상태"},{"id":"B","text":"프로그램 카운터"},{"id":"C","text":"CPU 레지스터"},{"id":"D","text":"사용자 비밀번호"}]',
     'D', 'PCB에는 프로세스 상태, PC, 레지스터, 메모리 정보 등이 저장되지만 사용자 비밀번호는 저장되지 않습니다.'),
    
    (10, 1, 1, 10, '뮤텍스(Mutex)와 세마포어의 차이점은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"뮤텍스는 이진값만 가질 수 있다"},{"id":"B","text":"세마포어는 단일 스레드만 사용 가능하다"},{"id":"C","text":"뮤텍스는 여러 프로세스가 공유할 수 있다"},{"id":"D","text":"세마포어는 잠금 해제가 불가능하다"}]',
     'A', '뮤텍스는 0 또는 1의 이진값만 가지며, 세마포어는 정수값을 가질 수 있습니다.') AS new_values
ON DUPLICATE KEY UPDATE `question_text` = new_values.`question_text`;

-- -----------------------------------------------------------------------------
-- 섹션 2: 메모리 관리 (10문제) - section_id=2
-- -----------------------------------------------------------------------------
INSERT INTO `quiz_course_question` (`id`, `quiz_course_id`, `section_number`, `question_number`, `question_text`, `question_type`, `options`, `correct_answer`, `explanation`)
VALUES
    (11, 1, 2, 1, '가상 메모리(Virtual Memory)의 주요 장점은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"CPU 속도 향상"},{"id":"B","text":"물리 메모리보다 큰 프로그램 실행 가능"},{"id":"C","text":"하드디스크 용량 증가"},{"id":"D","text":"네트워크 속도 향상"}]',
     'B', '가상 메모리를 통해 물리 메모리보다 큰 프로그램도 실행할 수 있습니다.'),
    
    (12, 1, 2, 2, '페이지 폴트(Page Fault)란?', 'SHORT_ANSWER',
     NULL,
     '접근하려는 페이지가 물리 메모리에 없어 디스크에서 불러와야 하는 상황',
     '페이지 폴트는 요청된 페이지가 메모리에 없을 때 발생하는 인터럽트입니다.'),
    
    (13, 1, 2, 3, '페이지 교체 알고리즘 중 가장 이상적이지만 구현 불가능한 것은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"FIFO"},{"id":"B","text":"LRU"},{"id":"C","text":"OPT (Optimal)"},{"id":"D","text":"Clock"}]',
     'C', 'OPT(Optimal)는 미래에 가장 오랫동안 사용되지 않을 페이지를 교체하므로 이상적이지만 구현 불가능합니다.'),
    
    (14, 1, 2, 4, '내부 단편화(Internal Fragmentation)가 발생하는 메모리 할당 방식은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"페이징"},{"id":"B","text":"세그멘테이션"},{"id":"C","text":"연속 할당"},{"id":"D","text":"버디 시스템"}]',
     'A', '페이징에서는 고정 크기 페이지를 사용하므로 마지막 페이지에서 내부 단편화가 발생할 수 있습니다.'),
    
    (15, 1, 2, 5, 'TLB(Translation Lookaside Buffer)의 역할은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"캐시 메모리 관리"},{"id":"B","text":"페이지 테이블 캐싱으로 주소 변환 속도 향상"},{"id":"C","text":"디스크 I/O 최적화"},{"id":"D","text":"프로세스 스케줄링"}]',
     'B', 'TLB는 자주 사용되는 페이지 테이블 항목을 캐싱하여 주소 변환 속도를 높입니다.'),
    
    (16, 1, 2, 6, '스래싱(Thrashing)이 발생하는 원인은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"CPU 과부하"},{"id":"B","text":"메모리 부족으로 페이지 폴트가 과도하게 발생"},{"id":"C","text":"디스크 용량 부족"},{"id":"D","text":"네트워크 지연"}]',
     'B', '스래싱은 메모리 부족으로 페이지 폴트가 과도하게 발생하여 CPU 이용률이 급감하는 현상입니다.'),
    
    (17, 1, 2, 7, '세그멘테이션의 장점을 모두 고르세요.', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"논리적 단위로 메모리 관리"},{"id":"B","text":"공유와 보호가 용이"},{"id":"C","text":"내부 단편화 없음"},{"id":"D","text":"외부 단편화 없음"}]',
     '["A","B","C"]', '세그멘테이션은 논리적 단위 관리, 공유/보호 용이, 내부 단편화 없음의 장점이 있습니다 (외부 단편화는 발생).'),
    
    (18, 1, 2, 8, '페이지 크기가 커지면 발생하는 현상은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"페이지 테이블 크기 증가"},{"id":"B","text":"내부 단편화 증가"},{"id":"C","text":"페이지 폴트 증가"},{"id":"D","text":"TLB 미스 증가"}]',
     'B', '페이지 크기가 커지면 마지막 페이지에서 낭비되는 공간(내부 단편화)이 증가합니다.'),
    
    (19, 1, 2, 9, '워킹 셋(Working Set)이란?', 'SHORT_ANSWER',
     NULL,
     '일정 시간 동안 프로세스가 참조하는 페이지들의 집합',
     '워킹 셋은 프로세스가 일정 시간 동안 자주 참조하는 페이지들의 집합으로, 스래싱 방지에 사용됩니다.'),
    
    (20, 1, 2, 10, 'LRU 알고리즘의 구현 방법은?', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"카운터 사용"},{"id":"B","text":"스택 사용"},{"id":"C","text":"큐 사용"},{"id":"D","text":"해시 테이블과 링크드 리스트 조합"}]',
     '["A","B","D"]', 'LRU는 카운터, 스택, 또는 해시 테이블+링크드 리스트 조합으로 구현할 수 있습니다.')
AS new_values ON DUPLICATE KEY UPDATE `question_text` = new_values.`question_text`;

-- -----------------------------------------------------------------------------
-- 섹션 3: 파일 시스템 (10문제) - section_id=3
-- -----------------------------------------------------------------------------
INSERT INTO `quiz_course_question` (`id`, `quiz_course_id`, `section_number`, `question_number`, `question_text`, `question_type`, `options`, `correct_answer`, `explanation`)
VALUES
    (21, 1, 3, 1, '파일 시스템에서 inode가 저장하는 정보가 아닌 것은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"파일 크기"},{"id":"B","text":"파일 이름"},{"id":"C","text":"소유자 정보"},{"id":"D","text":"데이터 블록 위치"}]',
     'B', 'inode에는 파일 이름을 제외한 메타데이터가 저장됩니다. 파일 이름은 디렉토리 엔트리에 저장됩니다.'),
    
    (22, 1, 3, 2, '파일 할당 방법 중 연속 할당의 단점은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"직접 접근 불가"},{"id":"B","text":"외부 단편화 발생"},{"id":"C","text":"포인터 오버헤드"},{"id":"D","text":"FAT 테이블 필요"}]',
     'B', '연속 할당은 파일들 사이에 외부 단편화가 발생할 수 있습니다.'),
    
    (23, 1, 3, 3, '저널링 파일 시스템의 주요 목적은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"파일 압축"},{"id":"B","text":"시스템 충돌 후 빠른 복구"},{"id":"C","text":"파일 암호화"},{"id":"D","text":"디스크 용량 증가"}]',
     'B', '저널링은 파일 시스템 변경 사항을 로그에 기록하여 충돌 후 빠른 복구를 가능하게 합니다.'),
    
    (24, 1, 3, 4, '하드 링크와 심볼릭 링크의 차이점은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"하드 링크는 다른 파일 시스템을 가리킬 수 있다"},{"id":"B","text":"심볼릭 링크는 같은 inode를 공유한다"},{"id":"C","text":"하드 링크는 원본 삭제 시에도 데이터에 접근 가능"},{"id":"D","text":"심볼릭 링크는 디렉토리에 생성할 수 없다"}]',
     'C', '하드 링크는 같은 inode를 가리키므로 원본 삭제 시에도 데이터에 접근 가능합니다.'),
    
    (25, 1, 3, 5, '디스크 스케줄링 알고리즘 중 SCAN의 특징은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"가장 가까운 요청부터 처리"},{"id":"B","text":"한 방향으로 이동하며 처리 후 반대 방향으로"},{"id":"C","text":"요청 순서대로 처리"},{"id":"D","text":"가장 짧은 탐색 시간 우선"}]',
     'B', 'SCAN은 엘리베이터 알고리즘이라고도 하며, 한 방향으로 끝까지 이동 후 반대로 이동합니다.'),
    
    (26, 1, 3, 6, 'RAID 1의 특징은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"스트라이핑"},{"id":"B","text":"미러링"},{"id":"C","text":"패리티"},{"id":"D","text":"분산 패리티"}]',
     'B', 'RAID 1은 미러링 방식으로 동일한 데이터를 두 개의 디스크에 저장합니다.'),
    
    (27, 1, 3, 7, '파일 시스템의 블록 크기가 크면 발생하는 문제는?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"메타데이터 증가"},{"id":"B","text":"내부 단편화 증가"},{"id":"C","text":"외부 단편화 증가"},{"id":"D","text":"디스크 속도 저하"}]',
     'B', '블록 크기가 크면 작은 파일에서 낭비되는 공간(내부 단편화)이 증가합니다.'),
    
    (28, 1, 3, 8, 'VFS(Virtual File System)의 역할은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"가상 메모리 관리"},{"id":"B","text":"다양한 파일 시스템에 대한 통합 인터페이스 제공"},{"id":"C","text":"네트워크 파일 전송"},{"id":"D","text":"파일 암호화"}]',
     'B', 'VFS는 다양한 파일 시스템(ext4, NTFS 등)에 대해 일관된 인터페이스를 제공합니다.'),
    
    (29, 1, 3, 9, '버퍼 캐시의 역할을 모두 고르세요.', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"디스크 I/O 횟수 감소"},{"id":"B","text":"읽기 성능 향상"},{"id":"C","text":"쓰기 성능 향상"},{"id":"D","text":"CPU 성능 향상"}]',
     '["A","B","C"]', '버퍼 캐시는 디스크 I/O를 줄이고 읽기/쓰기 성능을 향상시킵니다.'),
    
    (30, 1, 3, 10, '파일 디스크립터(File Descriptor)란?', 'SHORT_ANSWER',
     NULL,
     '프로세스가 열린 파일을 참조하기 위해 사용하는 정수값',
     '파일 디스크립터는 프로세스별로 유지되며, 열린 파일을 식별하는 정수값입니다.')
AS new_values ON DUPLICATE KEY UPDATE `question_text` = new_values.`question_text`;

-- -----------------------------------------------------------------------------
-- 섹션 4: OSI 7계층과 TCP/IP (10문제) - section_id=4
-- -----------------------------------------------------------------------------
INSERT INTO `quiz_course_question` (`id`, `quiz_course_id`, `section_number`, `question_number`, `question_text`, `question_type`, `options`, `correct_answer`, `explanation`)
VALUES
    (31, 2, 1, 1, 'OSI 7계층 중 데이터 링크 계층의 역할은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"라우팅"},{"id":"B","text":"물리적 연결"},{"id":"C","text":"프레임 전송 및 오류 검출"},{"id":"D","text":"세션 관리"}]',
     'C', '데이터 링크 계층은 인접 노드 간 프레임 전송과 오류 검출/수정을 담당합니다.'),
    
    (32, 2, 1, 2, 'TCP/IP 4계층 모델의 계층을 올바른 순서로 나열하세요.', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"응용-전송-인터넷-네트워크 접근"},{"id":"B","text":"응용-세션-전송-네트워크"},{"id":"C","text":"물리-데이터링크-네트워크-전송"},{"id":"D","text":"네트워크-전송-세션-응용"}]',
     'A', 'TCP/IP 4계층: 응용(Application) - 전송(Transport) - 인터넷(Internet) - 네트워크 접근(Network Access)'),
    
    (33, 2, 1, 3, 'IP 주소의 역할은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"물리적 장치 식별"},{"id":"B","text":"논리적 네트워크 주소 지정"},{"id":"C","text":"포트 번호 관리"},{"id":"D","text":"도메인 이름 해석"}]',
     'B', 'IP 주소는 네트워크 상에서 장치를 논리적으로 식별하는 역할을 합니다.'),
    
    (34, 2, 1, 4, 'MAC 주소의 특징을 모두 고르세요.', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"48비트 길이"},{"id":"B","text":"네트워크 카드에 할당"},{"id":"C","text":"변경 가능"},{"id":"D","text":"라우터에서 사용"}]',
     '["A","B"]', 'MAC 주소는 48비트 길이이며 네트워크 카드(NIC)에 고유하게 할당됩니다.'),
    
    (35, 2, 1, 5, 'ARP(Address Resolution Protocol)의 역할은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"도메인을 IP로 변환"},{"id":"B","text":"IP 주소를 MAC 주소로 변환"},{"id":"C","text":"라우팅 테이블 관리"},{"id":"D","text":"포트 번호 할당"}]',
     'B', 'ARP는 IP 주소를 해당하는 MAC 주소로 변환하는 프로토콜입니다.'),
    
    (36, 2, 1, 6, '서브넷 마스크의 역할은?', 'SHORT_ANSWER',
     NULL,
     'IP 주소에서 네트워크 부분과 호스트 부분을 구분',
     '서브넷 마스크는 IP 주소를 네트워크 ID와 호스트 ID로 분리하는 데 사용됩니다.'),
    
    (37, 2, 1, 7, 'IPv4와 IPv6의 주소 길이는 각각?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"32비트, 128비트"},{"id":"B","text":"64비트, 256비트"},{"id":"C","text":"16비트, 64비트"},{"id":"D","text":"48비트, 96비트"}]',
     'A', 'IPv4는 32비트(4바이트), IPv6는 128비트(16바이트)입니다.'),
    
    (38, 2, 1, 8, 'DHCP의 역할은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"DNS 캐싱"},{"id":"B","text":"동적 IP 주소 할당"},{"id":"C","text":"라우팅 프로토콜"},{"id":"D","text":"패킷 필터링"}]',
     'B', 'DHCP는 네트워크에 연결된 장치에 동적으로 IP 주소를 할당합니다.'),
    
    (39, 2, 1, 9, '라우터의 주요 기능은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"같은 네트워크 내 프레임 전달"},{"id":"B","text":"서로 다른 네트워크 간 패킷 전달"},{"id":"C","text":"물리적 신호 증폭"},{"id":"D","text":"MAC 주소 학습"}]',
     'B', '라우터는 서로 다른 네트워크 간에 패킷을 전달(라우팅)합니다.'),
    
    (40, 2, 1, 10, 'NAT(Network Address Translation)의 장점을 모두 고르세요.', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"IP 주소 절약"},{"id":"B","text":"내부 네트워크 보안 향상"},{"id":"C","text":"통신 속도 향상"},{"id":"D","text":"외부에서 직접 접근 차단"}]',
     '["A","B","D"]', 'NAT는 IP 주소를 절약하고, 내부 주소를 숨겨 보안을 향상시킵니다.')
AS new_values ON DUPLICATE KEY UPDATE `question_text` = new_values.`question_text`;

-- -----------------------------------------------------------------------------
-- 섹션 5: TCP와 UDP (10문제) - section_id=5
-- -----------------------------------------------------------------------------
INSERT INTO `quiz_course_question` (`id`, `quiz_course_id`, `section_number`, `question_number`, `question_text`, `question_type`, `options`, `correct_answer`, `explanation`)
VALUES
    (41, 2, 2, 1, 'TCP의 특징이 아닌 것은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"연결 지향형"},{"id":"B","text":"신뢰성 보장"},{"id":"C","text":"순서 보장"},{"id":"D","text":"브로드캐스트 지원"}]',
     'D', 'TCP는 유니캐스트만 지원하며, 브로드캐스트는 UDP에서 가능합니다.'),
    
    (42, 2, 2, 2, 'TCP 3-way Handshake의 순서는?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"SYN → SYN-ACK → ACK"},{"id":"B","text":"ACK → SYN → SYN-ACK"},{"id":"C","text":"SYN → ACK → SYN-ACK"},{"id":"D","text":"ACK → SYN-ACK → SYN"}]',
     'A', 'TCP 연결 수립: 클라이언트 SYN → 서버 SYN-ACK → 클라이언트 ACK'),
    
    (43, 2, 2, 3, 'UDP를 사용하기 적합한 애플리케이션은?', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"실시간 스트리밍"},{"id":"B","text":"온라인 게임"},{"id":"C","text":"파일 전송"},{"id":"D","text":"DNS 조회"}]',
     '["A","B","D"]', 'UDP는 실시간성이 중요한 스트리밍, 게임, DNS 조회에 적합합니다.'),
    
    (44, 2, 2, 4, 'TCP의 흐름 제어 방식은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"Go-Back-N"},{"id":"B","text":"슬라이딩 윈도우"},{"id":"C","text":"Stop-and-Wait"},{"id":"D","text":"토큰 버킷"}]',
     'B', 'TCP는 슬라이딩 윈도우 방식으로 흐름 제어를 수행합니다.'),
    
    (45, 2, 2, 5, 'TCP의 혼잡 제어 알고리즘을 모두 고르세요.', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"Slow Start"},{"id":"B","text":"Congestion Avoidance"},{"id":"C","text":"Fast Retransmit"},{"id":"D","text":"Fast Recovery"}]',
     '["A","B","C","D"]', 'TCP 혼잡 제어: Slow Start, Congestion Avoidance, Fast Retransmit, Fast Recovery'),
    
    (46, 2, 2, 6, 'TCP 연결 종료 시 4-way Handshake를 사용하는 이유는?', 'SHORT_ANSWER',
     NULL,
     '양방향 연결을 각각 독립적으로 종료하기 위해',
     'TCP는 전이중(Full-duplex) 통신이므로 각 방향의 연결을 별도로 종료해야 합니다.'),
    
    (47, 2, 2, 7, 'TCP 헤더에 포함되지 않는 것은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"시퀀스 번호"},{"id":"B","text":"체크섬"},{"id":"C","text":"TTL"},{"id":"D","text":"윈도우 크기"}]',
     'C', 'TTL(Time To Live)은 IP 헤더에 포함되며, TCP 헤더에는 포함되지 않습니다.'),
    
    (48, 2, 2, 8, 'UDP 헤더의 크기는?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"8바이트"},{"id":"B","text":"20바이트"},{"id":"C","text":"32바이트"},{"id":"D","text":"64바이트"}]',
     'A', 'UDP 헤더는 8바이트로 고정되어 있습니다 (소스 포트, 목적지 포트, 길이, 체크섬).'),
    
    (49, 2, 2, 9, 'TCP에서 재전송이 발생하는 경우를 모두 고르세요.', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"타임아웃 발생"},{"id":"B","text":"중복 ACK 3개 수신"},{"id":"C","text":"윈도우 크기 0"},{"id":"D","text":"연결 성립 전"}]',
     '["A","B"]', 'TCP는 타임아웃 또는 중복 ACK 3개 수신 시 재전송을 수행합니다.'),
    
    (50, 2, 2, 10, 'Keep-Alive의 목적은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"연결 상태 확인"},{"id":"B","text":"데이터 압축"},{"id":"C","text":"포트 번호 관리"},{"id":"D","text":"암호화"}]',
     'A', 'Keep-Alive는 유휴 연결이 여전히 활성 상태인지 확인하는 데 사용됩니다.')
AS new_values ON DUPLICATE KEY UPDATE `question_text` = new_values.`question_text`;

-- -----------------------------------------------------------------------------
-- 섹션 6: HTTP와 웹 (10문제) - section_id=6
-- -----------------------------------------------------------------------------
INSERT INTO `quiz_course_question` (`id`, `quiz_course_id`, `section_number`, `question_number`, `question_text`, `question_type`, `options`, `correct_answer`, `explanation`)
VALUES
    (51, 2, 3, 1, 'HTTP 상태 코드 404의 의미는?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"서버 오류"},{"id":"B","text":"리소스를 찾을 수 없음"},{"id":"C","text":"권한 없음"},{"id":"D","text":"리다이렉션"}]',
     'B', '404 Not Found는 요청한 리소스를 서버에서 찾을 수 없음을 의미합니다.'),
    
    (52, 2, 3, 2, 'HTTP 메서드 중 멱등성(Idempotent)을 가진 것을 모두 고르세요.', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"GET"},{"id":"B","text":"POST"},{"id":"C","text":"PUT"},{"id":"D","text":"DELETE"}]',
     '["A","C","D"]', 'GET, PUT, DELETE는 멱등성을 가지며, POST는 멱등성이 없습니다.'),
    
    (53, 2, 3, 3, 'HTTPS와 HTTP의 차이점은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"속도"},{"id":"B","text":"SSL/TLS를 통한 암호화"},{"id":"C","text":"포트 번호만 다름"},{"id":"D","text":"캐싱 정책"}]',
     'B', 'HTTPS는 HTTP에 SSL/TLS 암호화를 추가하여 데이터를 보호합니다.'),
    
    (54, 2, 3, 4, 'REST API의 특징을 모두 고르세요.', 'MULTIPLE_CHOICE_MULTIPLE',
     '[{"id":"A","text":"무상태성"},{"id":"B","text":"리소스 기반"},{"id":"C","text":"세션 필수"},{"id":"D","text":"HTTP 메서드 활용"}]',
     '["A","B","D"]', 'REST API는 무상태성, 리소스 기반 설계, HTTP 메서드 활용이 특징입니다.'),
    
    (55, 2, 3, 5, 'HTTP/2의 주요 개선점은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"텍스트 기반 프로토콜"},{"id":"B","text":"멀티플렉싱 지원"},{"id":"C","text":"암호화 필수 제거"},{"id":"D","text":"쿠키 제거"}]',
     'B', 'HTTP/2는 멀티플렉싱을 통해 하나의 연결에서 여러 요청/응답을 동시에 처리합니다.'),
    
    (56, 2, 3, 6, 'CORS(Cross-Origin Resource Sharing)란?', 'SHORT_ANSWER',
     NULL,
     '다른 출처(도메인)의 리소스에 접근할 수 있도록 허용하는 메커니즘',
     'CORS는 브라우저의 동일 출처 정책을 우회하여 다른 도메인의 리소스 접근을 허용합니다.'),
    
    (57, 2, 3, 7, 'HTTP 캐싱 관련 헤더가 아닌 것은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"Cache-Control"},{"id":"B","text":"ETag"},{"id":"C","text":"Content-Type"},{"id":"D","text":"Last-Modified"}]',
     'C', 'Content-Type은 응답 본문의 MIME 타입을 나타내며, 캐싱과는 관련이 없습니다.'),
    
    (58, 2, 3, 8, 'JWT(JSON Web Token)의 구성 요소는?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"Header, Body, Footer"},{"id":"B","text":"Header, Payload, Signature"},{"id":"C","text":"Key, Value, Hash"},{"id":"D","text":"Token, Secret, Expiry"}]',
     'B', 'JWT는 Header(알고리즘), Payload(클레임), Signature(서명)로 구성됩니다.'),
    
    (59, 2, 3, 9, '쿠키와 세션의 차이점은?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"쿠키는 서버에, 세션은 클라이언트에 저장"},{"id":"B","text":"쿠키는 클라이언트에, 세션은 서버에 저장"},{"id":"C","text":"둘 다 서버에 저장"},{"id":"D","text":"둘 다 클라이언트에 저장"}]',
     'B', '쿠키는 클라이언트(브라우저)에, 세션 데이터는 서버에 저장됩니다.'),
    
    (60, 2, 3, 10, 'HTTP 상태 코드 5xx의 의미는?', 'MULTIPLE_CHOICE',
     '[{"id":"A","text":"성공"},{"id":"B","text":"리다이렉션"},{"id":"C","text":"클라이언트 오류"},{"id":"D","text":"서버 오류"}]',
     'D', '5xx 상태 코드는 서버 측 오류를 나타냅니다.')
AS new_values ON DUPLICATE KEY UPDATE `question_text` = new_values.`question_text`;

-- =============================================================================
-- 5. 사용자 코스 진행 데이터 (Sample Progress Data)
-- =============================================================================
-- 아래 테이블들의 스키마도 실제 DB에 맞게 수정이 필요할 수 있습니다.
-- 문제 발생 시 DESCRIBE 명령으로 확인 후 알려주세요.

-- 사용자 코스 진행 상황
-- INSERT INTO `user_course_progress` (`user_id`, `course_id`, `current_section`, `completed_sections`, `is_completed`, `started_at`)
-- VALUES
--     (1, 1, 2, 1, FALSE, '2025-01-15 10:00:00'),  -- OS 코스: 섹션1 완료, 섹션2 진행 중
--     (1, 2, 1, 0, FALSE, '2025-01-20 14:00:00')   -- Network 코스: 섹션1 진행 중
-- ON DUPLICATE KEY UPDATE `current_section` = VALUES(`current_section`);

-- 섹션 시도 기록 (완료된 시도 예시)
-- INSERT INTO `user_section_attempt` (
--     `id`, `user_id`, `quiz_course_id`, `section_number`, 
--     `status`, `score`, `correct_count`, `total_questions`, 
--     `is_passed`, `completed_at`, `created_at`, `updated_at`, `version`
-- )
-- VALUES
--     (1, 1, 1, 1, 'SUBMITTED', 80, 8, 10, TRUE, '2026-01-15 11:30:00', NOW(), NOW(), 0)
-- ON DUPLICATE KEY UPDATE `score` = VALUES(`score`), `version` = VALUES(`version`);

-- =============================================================================
-- 끝 (END)
-- =============================================================================
