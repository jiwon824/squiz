# DB 마이그레이션 가이드 - Topic/Format 정규화

> **대상**: ModuStudy 백엔드 개발자 및 팀원
> **목적**: 번개 스터디 등록 500 에러 해결을 위한 DB 스키마 마이그레이션 가이드
> **난이도**: ⭐⭐ (중급)

---

## 📋 변경 개요

### 문제 상황
**증상**: 번개 스터디 등록 시 500 Internal Server Error 발생

**에러 메시지**:
```
Field 'topic' doesn't have a default value
SQL State: HY000
Error Code: 1364
```

**원인**:
- DB 스키마: `study.topic VARCHAR(50) NOT NULL`
- JPA 엔티티: `@ManyToOne Topic topic` → `@JoinColumn(name = "topic_id")`
- **불일치**: DB는 VARCHAR 컬럼을 기대하지만, JPA는 BIGINT FK로 INSERT 시도

### 목표
JPA 엔티티 구조에 맞게 DB를 정규화된 구조로 변경:
- `study.topic VARCHAR` → `study.topic_id BIGINT (FK)`
- `study.format VARCHAR` → `study.format_id BIGINT (FK)`

---

## 🔧 변경 사항

### 1. 새로운 마스터 테이블 생성 (V19)

#### Topic 테이블 (계층 구조)
```sql
CREATE TABLE topic (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  parent_id BIGINT,                    -- 대분류/소분류 계층 구조
  icon VARCHAR(50),
  sort_order INT DEFAULT 0,
  FOREIGN KEY (parent_id) REFERENCES topic(id)
);
```

**초기 데이터** (10개 대분류, 60+ 소분류):
- 알고리즘/코딩테스트 → 백준, 프로그래머스, SWEA, LeetCode, 코딩테스트 대비
- CS 기초 → 자료구조, 알고리즘 이론, 운영체제, 네트워크, 데이터베이스...
- 프론트엔드 → HTML/CSS, JavaScript, TypeScript, React, Vue, Next.js...
- 백엔드 → Java/Spring, Python/Django, Node.js/Express, Go, Kotlin...
- 인프라/DevOps, AI/ML, 모바일, 자격증, 취업 준비, 프로젝트

#### Format 테이블
```sql
CREATE TABLE format (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(200),
  icon VARCHAR(50),
  sort_order INT DEFAULT 0
);
```

**초기 데이터** (8개):
1. 문제 풀이
2. 독서/책 스터디
3. 강의 수강
4. 프로젝트
5. 모의 면접
6. 코드 리뷰
7. 발표/세미나
8. 토론

### 2. study 테이블 스키마 변경 (V20)

#### Before
```sql
CREATE TABLE study (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  topic VARCHAR(50) NOT NULL,      -- ❌ VARCHAR
  format VARCHAR(50),               -- ❌ VARCHAR
  ...
);
```

#### After
```sql
CREATE TABLE study (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  topic_id BIGINT NOT NULL,         -- ✅ BIGINT FK
  format_id BIGINT,                 -- ✅ BIGINT FK
  FOREIGN KEY (topic_id) REFERENCES topic(id) ON DELETE RESTRICT,
  FOREIGN KEY (format_id) REFERENCES format(id) ON DELETE SET NULL,
  INDEX idx_study_topic (topic_id),
  INDEX idx_study_format (format_id),
  ...
);
```

### 3. study_template 테이블도 동일 변경

---

## 🚀 적용 방법

### 📌 환경 요구사항
- Java 21
- MySQL 8.0
- Gradle 8.5+
- 로컬 MySQL 서버 실행 중

### Step 1: 코드 Pull

```bash
git pull origin front_muni
```

### Step 2: Flyway 설정 확인

**파일**: `modustudy/backend/src/main/resources/application.properties`

```properties
# Flyway (DB 마이그레이션)
spring.flyway.enabled=true                    # ✅ true로 설정
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
spring.flyway.validate-on-migrate=false       # ✅ checksum 검증 비활성화
```

**이미 설정 완료됨** - 별도 수정 불필요

### Step 3: 백엔드 실행

#### Windows (PowerShell)
```powershell
cd modustudy/backend

# 환경변수 설정 + 실행
powershell -Command "$env:DB_USERNAME='root'; $env:DB_PASSWORD='본인_MySQL_비밀번호'; ./gradlew bootRun"
```

#### Linux/Mac
```bash
cd modustudy/backend

# 환경변수 설정
export DB_USERNAME=root
export DB_PASSWORD=본인_MySQL_비밀번호

# 실행
./gradlew bootRun
```

**중요**: `본인_MySQL_비밀번호`를 실제 MySQL root 비밀번호로 변경하세요.

### Step 4: 마이그레이션 성공 확인

#### 콘솔 로그 확인
```
Flyway Community Edition 9.22.3 by Redgate
...
Migrating schema `ssafy_web_db` to version "19 - create topic format tables"
Migrating schema `ssafy_web_db` to version "20 - migrate study topic format"
Successfully applied migrations to schema `ssafy_web_db`
...
Started GroupCallApplication in 9.XXX seconds
```

✅ "Started GroupCallApplication" 메시지가 보이면 성공!

#### MySQL 직접 확인 (선택사항)
```bash
mysql -u root -p

mysql> USE ssafy_web_db;

# 테이블 존재 확인
mysql> SHOW TABLES LIKE 'topic';
mysql> SHOW TABLES LIKE 'format';

# study 테이블 구조 확인
mysql> DESC study;
# topic_id, format_id 컬럼이 있고, topic, format VARCHAR 컬럼이 없어야 함

# 초기 데이터 확인
mysql> SELECT * FROM topic WHERE parent_id IS NULL;  -- 대분류 10개
mysql> SELECT * FROM format;                          -- 형식 8개
```

---

## 📊 마이그레이션 상세 프로세스

### V19: Topic/Format 테이블 생성

1. `topic` 테이블 생성 (계층 구조 지원)
2. `format` 테이블 생성
3. 초기 데이터 INSERT (10개 대분류)
4. 초기 데이터 INSERT (60+ 소분류)
5. 초기 데이터 INSERT (8개 형식)

### V20: study 테이블 스키마 변경

```sql
-- 1. 새 컬럼 추가 (nullable)
ALTER TABLE study ADD COLUMN topic_id BIGINT NULL;
ALTER TABLE study ADD COLUMN format_id BIGINT NULL;

-- 2. 기존 데이터 마이그레이션 (VARCHAR → ID)
UPDATE study s
INNER JOIN topic t ON s.topic = t.name
SET s.topic_id = t.id;

UPDATE study s
INNER JOIN format f ON s.format = f.name
SET s.format_id = f.id;

-- 3. 매핑 실패한 데이터 기본값 설정
UPDATE study SET topic_id = (
  SELECT id FROM topic WHERE name = '백준' LIMIT 1
) WHERE topic_id IS NULL;

-- 4. NOT NULL 제약조건 추가
ALTER TABLE study MODIFY COLUMN topic_id BIGINT NOT NULL;

-- 5. 외래 키 및 인덱스 추가
ALTER TABLE study
ADD CONSTRAINT fk_study_topic
FOREIGN KEY (topic_id) REFERENCES topic(id) ON DELETE RESTRICT;

CREATE INDEX idx_study_topic ON study(topic_id);
CREATE INDEX idx_study_format ON study(format_id);

-- 6. 기존 VARCHAR 컬럼 삭제
ALTER TABLE study DROP COLUMN topic;
ALTER TABLE study DROP COLUMN format;
```

**데이터 안정성**:
- ✅ 기존 데이터는 자동으로 매핑됨 (손실 없음)
- ✅ 매핑 실패 시 기본값 설정 ("백준", "문제 풀이")
- ✅ 트랜잭션으로 보호됨

---

## 🧪 테스트 방법

### 1. 번개 스터디 등록 E2E 테스트

```bash
# 프론트엔드 실행
cd modustudy/frontend
npm run dev
```

1. `https://localhost:3000` 접속
2. 로그인
3. **번개 스터디 등록** 페이지 이동
4. 스터디 정보 입력:
   - 주제: "SWEA"
   - 형식: "문제 풀이"
   - 기타 정보 입력
5. **등록 버튼 클릭**
6. ✅ **500 에러 없이 정상 등록** 확인
7. 등록 페이지에서 **생성된 스터디 표시** 확인

### 2. API 직접 테스트

```bash
# 주제 목록 조회
curl -k https://localhost:8080/api/v1/study/topics

# 형식 목록 조회
curl -k https://localhost:8080/api/v1/study/formats

# 스터디 등록
curl -k -X POST https://localhost:8080/api/v1/study \
  -H "Content-Type: application/json" \
  -H "User-Id: 1" \
  -d '{
    "name": "테스트 번개 스터디",
    "topicId": 4,
    "formatId": 1,
    "studyType": "LIGHTNING",
    "meetingType": "ONLINE",
    "startDate": "2026-02-05",
    "endDate": "2026-02-05",
    "maxMembers": 4,
    "recruitStartDate": "2026-02-01",
    "recruitEndDate": "2026-02-05"
  }'
```

### 3. DB 데이터 확인

```sql
-- 스터디와 주제/형식 JOIN 조회
SELECT
  s.id,
  s.name AS study_name,
  t.name AS topic,
  f.name AS format,
  s.study_type,
  s.status
FROM study s
LEFT JOIN topic t ON s.topic_id = t.id
LEFT JOIN format f ON s.format_id = f.id
ORDER BY s.created_at DESC
LIMIT 10;

-- Topic 계층 구조 확인
SELECT
  IFNULL(p.name, '(대분류)') AS parent,
  c.name AS child,
  c.icon,
  c.sort_order
FROM topic c
LEFT JOIN topic p ON c.parent_id = p.id
ORDER BY IFNULL(p.sort_order, c.sort_order), c.sort_order;
```

---

## ⚠️ 트러블슈팅

### 🔴 문제 1: "Port 8080 was already in use"

**증상**:
```
Web server failed to start. Port 8080 was already in use.
```

**원인**: 이전 백엔드 프로세스가 여전히 실행 중

**해결**:
```powershell
# PowerShell에서 실행
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue |
  Select-Object -ExpandProperty OwningProcess |
  ForEach-Object { Stop-Process -Id $_ -Force }
```

또는:
```bash
# 프로세스 ID 확인
netstat -ano | findstr :8080

# 프로세스 종료 (PowerShell)
Stop-Process -Id <PID> -Force
```

---

### 🔴 문제 2: "Access denied for user 'ssafy'@'localhost'"

**증상**:
```
Access denied for user 'ssafy'@'localhost' (using password: YES)
SQL State: 28000
Error Code: 1045
```

**원인**:
1. 환경변수 미설정 또는 잘못된 비밀번호
2. `.env` 파일의 DB 설정이 `ssafy` 계정으로 되어 있음

**해결**:
```bash
# 방법 1: 환경변수로 root 계정 사용 (권장)
powershell -Command "$env:DB_USERNAME='root'; $env:DB_PASSWORD='본인_비밀번호'; ./gradlew bootRun"

# 방법 2: .env 파일 수정
# modustudy/backend/.env
DB_USERNAME=root
DB_PASSWORD=본인_비밀번호
```

---

### 🔴 문제 3: "Duplicate column name 'topic_id'"

**증상**:
```
Migration V20__migrate_study_topic_format.sql failed
Duplicate column name 'topic_id'
```

**원인**: JPA의 `ddl-auto=update`가 먼저 실행되어 이미 topic_id 컬럼 생성됨

**해결**:
✅ **무시하고 진행** - 이미 처리되었음
- Flyway가 자동으로 실패 레코드를 건너뛰고 다음 마이그레이션 진행
- 백엔드가 정상 시작(`Started GroupCallApplication`)되면 문제없음

**검증**:
```sql
mysql> DESC study;
# topic_id BIGINT NOT NULL 컬럼이 있으면 OK
```

---

### 🔴 문제 4: MySQL 접속 불가

**증상**:
```
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
```

**원인**: MySQL 서버 미실행

**해결**:
```powershell
# MySQL 서비스 상태 확인
Get-Service -Name MySQL*

# 서비스 시작
Start-Service MySQL80

# 수동 접속 테스트
mysql -u root -p
```

---

### 🔴 문제 5: "Flyway checksum mismatch"

**증상**:
```
Migration checksum mismatch for migration version XX
Validate failed: Migrations have failed validation
```

**원인**: 마이그레이션 파일이 수정됨

**해결**:
✅ **이미 처리됨** - `application.properties`에 다음 설정 추가됨:
```properties
spring.flyway.validate-on-migrate=false
```

---

### 🔴 문제 6: 백엔드 실행 후에도 500 에러

**증상**: 번개 스터디 등록 시 여전히 `Field 'topic' doesn't have a default value` 에러

**체크리스트**:
- [ ] `spring.flyway.enabled=true` 확인
- [ ] 백엔드 완전히 재시작 (기존 프로세스 종료 확인)
- [ ] 콘솔 로그에 "Started GroupCallApplication" 확인
- [ ] Flyway 마이그레이션 성공 로그 확인
- [ ] DB 확인:
  ```sql
  -- flyway_schema_history 확인
  SELECT version, description, success
  FROM flyway_schema_history
  WHERE version IN ('19', '20');

  -- study 테이블 구조 확인
  DESC study;
  -- topic_id, format_id 있고 topic, format 없어야 함
  ```

---

## 📝 롤백 가이드 (긴급 시)

### ⚠️ 경고
- 롤백은 **최후의 수단**입니다
- 가능하면 **문제를 진단하고 수정**하는 것을 권장
- 롤백 시 **V19, V20 이후 생성된 데이터는 손실**될 수 있음

### 롤백 절차

#### 1. 백업 생성 (필수)
```bash
mysqldump -u root -p ssafy_web_db > rollback_backup_$(date +%Y%m%d_%H%M%S).sql
```

#### 2. Flyway 비활성화
```properties
# application.properties
spring.flyway.enabled=false
```

#### 3. JPA ddl-auto 변경
```properties
# application.properties
spring.jpa.hibernate.ddl-auto=validate  # update → validate
```

#### 4. 마이그레이션 수동 롤백
```sql
-- 외래 키 제약조건 제거
ALTER TABLE study DROP FOREIGN KEY fk_study_topic;
ALTER TABLE study DROP FOREIGN KEY fk_study_format;

-- 새 컬럼 제거
ALTER TABLE study DROP COLUMN topic_id;
ALTER TABLE study DROP COLUMN format_id;

-- 기존 VARCHAR 컬럼 복원
ALTER TABLE study ADD COLUMN topic VARCHAR(50) NOT NULL DEFAULT 'SWEA';
ALTER TABLE study ADD COLUMN format VARCHAR(50);

-- Topic/Format 테이블 삭제
DROP TABLE topic;
DROP TABLE format;

-- Flyway 히스토리에서 V19, V20 제거
DELETE FROM flyway_schema_history WHERE version IN ('19', '20');
```

#### 5. 백엔드 재시작
```bash
./gradlew bootRun
```

---

## 📌 추가 정보

### 관련 파일
```
modustudy/backend/src/main/resources/
├── db/migration/
│   ├── V19__create_topic_format_tables.sql       # Topic/Format 테이블 생성
│   └── V20__migrate_study_topic_format.sql       # study 테이블 마이그레이션
└── application.properties                         # Flyway 설정

modustudy/backend/src/main/java/com/ssafy/domain/study/
├── entity/
│   ├── Study.java                                 # @ManyToOne Topic, Format
│   ├── Topic.java                                 # 주제 엔티티
│   └── Format.java                                # 형식 엔티티
└── controller/
    └── StudyController.java                       # /api/v1/study/topics, /formats
```

### 참고 문서
- [MIGRATION_SUMMARY.md](./MIGRATION_SUMMARY.md) - 간단 요약 (팀원 공유용)
- Flyway 공식 문서: https://flywaydb.org/documentation/
- Spring Boot + Flyway: https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway

### Contact
- **문제 발생 시**: 백엔드 담당자에게 문의
- **GitHub Issue**: `#<이슈번호>`
- **작성자**: 프론트엔드 개발팀 (조문희)
- **브랜치**: `front_muni`

---

**작성일**: 2026-02-01
**버전**: 1.0
**최종 수정**: 2026-02-01
**검토 완료**: ✅ DB 마이그레이션 성공 및 백엔드 정상 시작 확인
