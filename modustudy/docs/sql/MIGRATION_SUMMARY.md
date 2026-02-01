# DB 마이그레이션 요약 - 번개 스터디 등록 에러 해결

## 🚨 문제
번개 스터디 등록 시 500 에러 발생:
```
Field 'topic' doesn't have a default value
```

**원인**: DB 스키마(VARCHAR)와 JPA 엔티티(BIGINT FK) 불일치

## ✅ 해결
Topic/Format을 정규화된 테이블로 분리하고, study 테이블을 외래 키 구조로 변경

## 📦 생성/수정된 파일

### 1. 마이그레이션 파일
```
modustudy/backend/src/main/resources/db/migration/
├── V19__create_topic_format_tables.sql     # Topic/Format 테이블 생성
└── V20__migrate_study_topic_format.sql     # study 테이블 스키마 변경
```

### 2. 설정 변경
```
modustudy/backend/src/main/resources/application.properties
+ spring.flyway.enabled=true                 # Flyway 활성화
+ spring.flyway.validate-on-migrate=false    # checksum 검증 비활성화
```

### 3. 가이드 문서
```
DB_MIGRATION_GUIDE.md                        # 팀원 전달용 상세 가이드
```

## 🚀 팀원 적용 방법

### Windows 환경 (로컬 개발)

#### 1단계: 코드 Pull
```bash
git pull origin front_muni
```

#### 2단계: 환경변수 설정 및 백엔드 실행
```bash
cd modustudy/backend

# PowerShell에서 실행
powershell -Command "$env:DB_USERNAME='root'; $env:DB_PASSWORD='본인_MySQL_비밀번호'; ./gradlew bootRun"
```

**중요**: `본인_MySQL_비밀번호`를 실제 MySQL root 비밀번호로 변경하세요.

#### 3단계: 마이그레이션 확인
콘솔 로그에서 다음 메시지 확인:
```
Started GroupCallApplication in X.XXX seconds
```

#### 4단계: 테스트
1. 프론트엔드 실행: `cd modustudy/frontend && npm run dev`
2. 번개 스터디 등록 페이지 접속
3. 스터디 등록 → ✅ 500 에러 없이 정상 등록 확인

### Linux/Mac 환경

```bash
cd modustudy/backend

# 환경변수 설정
export DB_USERNAME=root
export DB_PASSWORD=본인_MySQL_비밀번호

./gradlew bootRun
```

## 📊 변경 내용 상세

### Before (에러 발생)
```sql
CREATE TABLE study (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100),
  topic VARCHAR(50) NOT NULL,     -- ❌ JPA: topic_id BIGINT
  format VARCHAR(50),              -- ❌ JPA: format_id BIGINT
  ...
);
```

### After (정상 작동)
```sql
-- 1. 새로운 마스터 테이블
CREATE TABLE topic (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  parent_id BIGINT,               -- 계층 구조 (대분류 - 소분류)
  icon VARCHAR(50),
  sort_order INT DEFAULT 0
);

CREATE TABLE format (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(200),
  icon VARCHAR(50)
);

-- 2. study 테이블 변경
CREATE TABLE study (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100),
  topic_id BIGINT NOT NULL,        -- ✅ JPA와 일치
  format_id BIGINT,                -- ✅ JPA와 일치
  FOREIGN KEY (topic_id) REFERENCES topic(id),
  FOREIGN KEY (format_id) REFERENCES format(id),
  ...
);
```

## 🗂️ 초기 데이터

### Topic (10개 대분류, 60+ 소분류)
- 알고리즘/코딩테스트 (백준, 프로그래머스, SWEA, LeetCode...)
- CS 기초 (자료구조, 알고리즘 이론, 운영체제, 네트워크...)
- 프론트엔드 (React, Vue, Next.js...)
- 백엔드 (Java/Spring, Python/Django...)
- 인프라/DevOps, AI/ML, 모바일, 자격증, 취업 준비, 프로젝트

### Format (8개)
- 문제 풀이, 독서/책 스터디, 강의 수강, 프로젝트
- 모의 면접, 코드 리뷰, 발표/세미나, 토론

## ⚠️ 트러블슈팅

### Q1. "Port 8080 was already in use" 에러
**원인**: 이전 백엔드 프로세스가 여전히 실행 중

**해결**:
```powershell
# PowerShell에서 실행
Get-NetTCPConnection -LocalPort 8080 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

### Q2. "Access denied for user 'ssafy'@'localhost'" 에러
**원인**: 환경변수 설정 누락

**해결**: 2단계에서 환경변수를 올바르게 설정했는지 확인
```bash
$env:DB_USERNAME='root'
$env:DB_PASSWORD='본인_비밀번호'
```

### Q3. MySQL 접속 불가
**원인**: MySQL 서버 미실행

**해결**:
```powershell
# MySQL 서비스 상태 확인
Get-Service -Name MySQL*

# 서비스 시작 (필요 시)
Start-Service MySQL80
```

### Q4. 마이그레이션 실패 (이미 컬럼 존재 에러)
**원인**: JPA의 ddl-auto=update가 먼저 실행됨

**해결**: 이미 처리되었으므로 무시하고 정상 시작 확인

## 🔍 상세 정보
**전체 가이드**: [DB_MIGRATION_GUIDE.md](DB_MIGRATION_GUIDE.md) 참고

## ✅ 검증 방법

### 1. DB 직접 확인 (선택사항)
```bash
mysql -u root -p

mysql> USE ssafy_web_db;
mysql> SHOW TABLES LIKE 'topic';      -- topic 테이블 존재 확인
mysql> SHOW TABLES LIKE 'format';     -- format 테이블 존재 확인
mysql> DESC study;                     -- topic_id, format_id 컬럼 확인

mysql> SELECT * FROM topic WHERE parent_id IS NULL;  -- 대분류 10개 확인
mysql> SELECT * FROM format;                          -- 형식 8개 확인
```

### 2. API 테스트
```bash
# 주제 목록 조회
curl https://localhost:8080/api/v1/study/topics

# 형식 목록 조회
curl https://localhost:8080/api/v1/study/formats
```

## 📝 주의사항
- ✅ **기존 데이터는 자동으로 매핑됨** (손실 없음)
- ⚠️ **로컬 환경에서 먼저 테스트** 후 운영 배포
- 💡 **Flyway는 한 번만 실행됨** (이미 성공한 마이그레이션은 재실행 안 됨)
- 🔒 **절대 flyway_schema_history 테이블 수동 수정 금지**

## 🆘 문제 발생 시
1. 콘솔 로그 전체 캡처
2. `SELECT * FROM flyway_schema_history ORDER BY installed_rank;` 결과 캡처
3. 백엔드 담당자에게 문의

---

**작성일**: 2026-02-01
**작성자**: 프론트엔드 개발팀 (조문희)
**브랜치**: front_muni
**관련 이슈**: 번개 스터디 등록 500 에러 (`Field 'topic' doesn't have a default value`)
