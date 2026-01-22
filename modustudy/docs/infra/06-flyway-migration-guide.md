# Flyway 데이터베이스 마이그레이션 가이드

## 1. 왜 Flyway를 도입했나?

### 1.1 기존 방식의 문제점

**기존: JPA `ddl-auto=update` + 수동 SQL 실행**

```
문제 1: 데이터 손실 위험
- init.sql 다시 실행하면 기존 데이터 삭제됨
- 운영 DB에서 실수로 DROP 실행 가능

문제 2: 버전 관리 불가
- "지금 DB가 어느 버전인지" 알 수 없음
- 백업 복원 시 호환성 확인 불가

문제 3: 팀 협업 어려움
- "ERD.sql 최신본 누가 들고 있어?"
- 스키마 변경이 누락되거나 충돌

문제 4: MyBatis 테이블 미생성
- JPA ddl-auto는 JPA 엔티티만 생성
- MyBatis 전용 테이블은 수동 생성 필요
```

### 1.2 Flyway 도입 후

```
✅ 버전 관리: V1, V2, V3... 순차적 적용
✅ 이력 추적: flyway_schema_history 테이블에 기록
✅ 안전한 배포: 이미 적용된 버전은 스킵
✅ 롤백 대비: 백업에 버전 포함
✅ 팀 협업: Git으로 마이그레이션 파일 관리
```

---

## 2. Flyway 동작 원리

### 2.1 마이그레이션 파일 구조

```
src/main/resources/db/migration/
├── V1__init.sql              # 초기 스키마 (전체 테이블)
├── V2__add_friend_tables.sql # 친구 기능 추가
├── V3__add_dm_feature.sql    # DM 기능 추가
└── V4__add_index.sql         # 인덱스 최적화
```

**파일명 규칙**: `V<버전>__<설명>.sql`
- `V`: 필수 접두사
- `<버전>`: 1, 2, 3... (정수 또는 1.1, 1.2)
- `__`: 언더스코어 2개 (필수)
- `<설명>`: 변경 내용 설명

### 2.2 실행 흐름

```
1. 앱 시작 또는 gradle flywayMigrate 실행
2. flyway_schema_history 테이블 확인
3. 미적용 버전만 순차 실행
4. 실행 결과 기록

예시:
- DB에 V1, V2 적용됨
- 새로 V3, V4 파일 추가
- Flyway 실행 → V3, V4만 적용
```

### 2.3 flyway_schema_history 테이블

```sql
SELECT version, description, success, installed_on
FROM flyway_schema_history;

+--------+------------------+---------+---------------------+
| version| description      | success | installed_on        |
+--------+------------------+---------+---------------------+
| 1      | init             | 1       | 2025-01-20 10:00:00 |
| 2      | add friend tables| 1       | 2025-01-21 14:30:00 |
| 3      | add dm feature   | 1       | 2025-01-22 09:15:00 |
+--------+------------------+---------+---------------------+
```

---

## 3. 기존에서 변경된 것들

### 3.1 JPA 설정 변경

**Before**:
```properties
spring.jpa.hibernate.ddl-auto=update
```

**After**:
```properties
spring.jpa.hibernate.ddl-auto=validate  # 스키마 검증만
```

**이유**: Flyway가 스키마를 관리하므로 JPA는 검증만

### 3.2 새로운 파일/폴더

```
backend/
├── src/main/resources/
│   └── db/migration/
│       └── V1__init.sql     # 전체 스키마
└── build.gradle             # Flyway 플러그인 추가
```

### 3.3 CI/CD 파이프라인 변경

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - migrate    # 새로 추가됨
  - deploy
```

**migrate 단계**:
1. 배포 전 DB 백업 (버전 포함)
2. `gradle flywayMigrate` 실행
3. 마이그레이션 성공 시 deploy 진행

### 3.4 백업 파일 형식 변경

**Before**: `squiz_daily_20250120_030000.sql.gz`

**After**: `squiz_daily_V3_20250120_030000.sql.gz`
- 버전 포함으로 복원 시 호환성 확인 가능

---

## 4. 새 마이그레이션 추가하기

### 4.1 새 기능 개발 시

```bash
# 1. 마이그레이션 파일 생성
touch src/main/resources/db/migration/V2__add_notification_table.sql

# 2. SQL 작성
```

```sql
-- V2__add_notification_table.sql
CREATE TABLE IF NOT EXISTS `push_notification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `body` TEXT,
    `sent_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

-- 기존 테이블 수정은 ALTER 사용
ALTER TABLE `notification` ADD COLUMN `push_sent` BOOLEAN DEFAULT FALSE;
```

### 4.2 주의사항

```
❌ 절대 하면 안 되는 것
- 이미 적용된 V1 파일 수정 (체크섬 오류 발생)
- DROP TABLE 무분별 사용
- 운영 중인 테이블에 NOT NULL 컬럼 바로 추가

✅ 안전한 방법
- 새 버전 파일로만 변경
- ALTER TABLE은 점진적으로
- NOT NULL은 DEFAULT 값과 함께 추가 후, 다음 버전에서 DEFAULT 제거
```

### 4.3 무중단 스키마 변경 패턴

**Expand → Migrate → Contract**

```sql
-- V2: Expand (확장) - 새 컬럼 추가 (NULL 허용)
ALTER TABLE `user` ADD COLUMN `phone` VARCHAR(20) NULL;

-- 앱에서 새 컬럼 사용 시작 (구버전 코드도 동작함)

-- V3: Contract (정리) - 필요 시 NOT NULL로 변경
-- (이 시점에 구버전 코드가 모두 내려간 후)
ALTER TABLE `user` MODIFY COLUMN `phone` VARCHAR(20) NOT NULL;
```

---

## 5. Flyway 명령어

### 5.1 Gradle 명령어

```bash
cd modustudy/backend

# 현재 상태 확인
./gradlew flywayInfo

# 마이그레이션 실행
./gradlew flywayMigrate

# 마이그레이션 검증
./gradlew flywayValidate

# (주의!) 전체 초기화 - 개발/테스트용
./gradlew flywayClean
```

### 5.2 운영에서 주의사항

```bash
# 운영에서는 반드시 백업 후 실행
./scripts/backup-db.sh manual
./gradlew flywayMigrate

# 또는 CI/CD의 migrate 단계 사용 (권장)
```

---

## 6. 트러블슈팅

### 6.1 체크섬 오류

```
Flyway: Validate failed. Migration checksum mismatch for V1
```

**원인**: 이미 적용된 파일이 수정됨

**해결**:
```bash
# 개발 환경: clean 후 재시작
./gradlew flywayClean flywayMigrate

# 운영 환경: 절대 clean 금지! 히스토리 직접 수정
UPDATE flyway_schema_history SET checksum = NULL WHERE version = '1';
```

### 6.2 마이그레이션 실패

```
Flyway: Migration V3 failed
```

**해결**:
1. 오류 원인 파악 (SQL 문법, 제약조건 등)
2. V3 파일 수정 (아직 적용 안 됨)
3. 재실행: `./gradlew flywayMigrate`

### 6.3 순서 문제

```
Flyway: Detected resolved migration not applied to database: V2
```

**원인**: V3가 먼저 적용되고 V2가 나중에 추가됨

**해결**:
```properties
# application.properties
spring.flyway.out-of-order=true  # 순서 무시 허용 (비권장)
```

---

## 7. 환경별 설정

### 7.1 개발 환경 (application.properties)

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
spring.jpa.hibernate.ddl-auto=validate
```

### 7.2 테스트 환경 (test/application.properties)

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.clean-disabled=false  # 테스트에서만 clean 허용
spring.flyway.baseline-on-migrate=true
spring.jpa.hibernate.ddl-auto=validate
```

### 7.3 운영 환경 (application-prod.properties)

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
spring.jpa.hibernate.ddl-auto=validate
# clean은 기본적으로 비활성화됨 (clean-disabled=true)
```

---

## 8. 백업과 복원

### 8.1 버전이 포함된 백업

```bash
./scripts/backup-db.sh manual

# 출력:
# 현재 Flyway 버전: V3
# 백업 완료: squiz_manual_V3_20250120_143052.sql.gz
```

### 8.2 복원 시 버전 체크

```bash
./scripts/restore-db.sh squiz_manual_V1_20250115_120000.sql.gz

# 출력:
# 백업 Flyway 버전: V1
# 현재 DB Flyway 버전: V3
# ⚠️  버전 불일치 감지!
#     백업이 현재 DB보다 구버전입니다!
```

---

## 9. 기존 DB에 Flyway 처음 적용하기

기존에 운영 중인 DB가 있다면:

```bash
# 1. 현재 스키마가 V1과 동일한지 확인

# 2. baseline 설정으로 V1 스킵
./gradlew flywayBaseline -Dflyway.baselineVersion=1

# 3. 이후 V2부터 적용됨
./gradlew flywayMigrate
```

**baseline의 의미**: "현재 DB 상태를 V1으로 간주하고 시작"

---

## 10. 체크리스트

### 배포 전 체크리스트

- [ ] 새 마이그레이션 파일이 올바른 버전 번호인가?
- [ ] 파일명 규칙을 지켰는가? (`V<N>__<설명>.sql`)
- [ ] 개발 환경에서 테스트했는가?
- [ ] 롤백 계획이 있는가? (또는 forward-fix 준비)
- [ ] 백업이 완료되었는가?

### 운영 안전 수칙

- [ ] `ddl-auto`는 `validate` 또는 `none`
- [ ] `flywayClean`은 운영에서 절대 금지
- [ ] 마이그레이션은 CI/CD에서 1회만 실행
- [ ] 큰 테이블 ALTER는 오프피크 시간에
