# 🚨 번개 스터디 등록 에러 해결 완료

> **작업 브랜치**: `front_muni`
> **작업일**: 2026-02-01
> **작업자**: 프론트엔드 개발팀 (조문희)

---

## ✅ 해결된 문제

**증상**: 번개 스터디 등록 시 500 에러 발생
```
Field 'topic' doesn't have a default value
```

**원인**: DB 스키마(VARCHAR)와 JPA 엔티티(BIGINT FK) 불일치

**해결**: Topic/Format 테이블 정규화 및 study 테이블 마이그레이션

---

## 📂 문서 목록

### 1️⃣ 빠른 시작 (권장)
**[MIGRATION_SUMMARY.md](./MIGRATION_SUMMARY.md)** - 3분 가이드
- 팀원 적용 방법 (간단 버전)
- Windows/Linux 실행 명령어
- 트러블슈팅 Q&A

### 2️⃣ 상세 가이드
**[DB_MIGRATION_GUIDE.md](./DB_MIGRATION_GUIDE.md)** - 완전판
- 변경 사항 상세 설명
- 마이그레이션 프로세스
- DB 구조 Before/After
- 롤백 가이드

---

## ⚡ 빠른 적용 (3단계)

### 1️⃣ 코드 Pull
```bash
git pull origin front_muni
```

### 2️⃣ 백엔드 실행 (Windows)
```powershell
cd modustudy/backend
powershell -Command "$env:DB_USERNAME='root'; $env:DB_PASSWORD='본인_MySQL_비밀번호'; ./gradlew bootRun"
```

**⚠️ 주의**: `본인_MySQL_비밀번호`를 실제 MySQL root 비밀번호로 변경하세요.

### 3️⃣ 성공 확인
콘솔에서 다음 메시지 확인:
```
Started GroupCallApplication in X.XXX seconds
```

✅ 완료! 이제 번개 스터디 등록이 정상 작동합니다.

---

## 📊 변경 내용 요약

### Before (에러 발생)
```sql
study.topic VARCHAR(50)    -- ❌ JPA는 topic_id BIGINT 기대
study.format VARCHAR(50)   -- ❌ JPA는 format_id BIGINT 기대
```

### After (정상 작동)
```sql
topic (id, name, parent_id, icon)        -- 새 테이블 (10개 대분류, 60+ 소분류)
format (id, name, description, icon)     -- 새 테이블 (8개 형식)

study.topic_id BIGINT FK → topic(id)     -- ✅ JPA와 일치
study.format_id BIGINT FK → format(id)   -- ✅ JPA와 일치
```

---

## 🗂️ 생성된 파일

```
modustudy/backend/src/main/resources/
├── db/migration/
│   ├── V19__create_topic_format_tables.sql      # Topic/Format 테이블 생성
│   └── V20__migrate_study_topic_format.sql      # study 테이블 마이그레이션
└── application.properties
    ├── spring.flyway.enabled=true               # Flyway 활성화
    └── spring.flyway.validate-on-migrate=false  # checksum 검증 비활성화
```

---

## 📋 초기 데이터

### Topic (10개 대분류)
- 알고리즘/코딩테스트
- CS 기초
- 프론트엔드
- 백엔드
- 인프라/DevOps
- AI/ML
- 모바일
- 자격증
- 취업 준비
- 프로젝트

각 대분류마다 5~8개 소분류 존재 (총 60+ 소분류)

### Format (8개)
1. 문제 풀이
2. 독서/책 스터디
3. 강의 수강
4. 프로젝트
5. 모의 면접
6. 코드 리뷰
7. 발표/세미나
8. 토론

---

## 🔍 검증 방법

### 1. 프론트엔드 테스트
1. 프론트엔드 실행: `cd modustudy/frontend && npm run dev`
2. 번개 스터디 등록 페이지 접속
3. 스터디 정보 입력 후 등록
4. ✅ 500 에러 없이 정상 등록 확인

### 2. DB 확인 (선택사항)
```bash
mysql -u root -p

mysql> USE ssafy_web_db;
mysql> SELECT * FROM topic WHERE parent_id IS NULL;  -- 대분류 10개
mysql> SELECT * FROM format;                          -- 형식 8개
mysql> DESC study;                                    -- topic_id, format_id 확인
```

---

## ⚠️ 자주 발생하는 문제

### 🔴 "Port 8080 was already in use"
**해결**: 기존 프로세스 종료
```powershell
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue |
  Select-Object -ExpandProperty OwningProcess |
  ForEach-Object { Stop-Process -Id $_ -Force }
```

### 🔴 "Access denied for user 'ssafy'@'localhost'"
**해결**: 환경변수 설정 확인
```powershell
# root 계정으로 실행
$env:DB_USERNAME='root'
$env:DB_PASSWORD='본인_비밀번호'
```

### 🔴 "Duplicate column name 'topic_id'"
**해결**: ✅ 무시하고 진행
- JPA가 이미 컬럼을 생성했음
- 백엔드가 정상 시작되면 문제없음

---

## 📞 문의

- **문제 발생 시**: 백엔드 담당자 또는 조문희에게 문의
- **상세 가이드**: [DB_MIGRATION_GUIDE.md](./DB_MIGRATION_GUIDE.md) 참고
- **GitHub Issue**: #`<이슈번호>`

---

## ✅ 체크리스트

팀원이 아래 항목을 순서대로 확인하세요:

- [ ] 코드 Pull 완료 (`git pull origin front_muni`)
- [ ] MySQL 서버 실행 중 확인
- [ ] 환경변수 설정 (`DB_USERNAME=root`, `DB_PASSWORD=본인_비밀번호`)
- [ ] 백엔드 실행 (`./gradlew bootRun`)
- [ ] 콘솔에 "Started GroupCallApplication" 메시지 확인
- [ ] 번개 스터디 등록 테스트 완료 (500 에러 없음)
- [ ] 등록된 스터디가 목록에 표시됨 확인

모든 항목이 ✅이면 성공!

---

**작성일**: 2026-02-01
**브랜치**: front_muni
**상태**: ✅ 마이그레이션 완료 및 테스트 성공
