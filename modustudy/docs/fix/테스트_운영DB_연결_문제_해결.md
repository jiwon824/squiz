# CI/CD 테스트가 운영 DB에 연결되어 데이터 유실된 문제

## 문제 요약

| 항목 | 내용 |
|------|------|
| 발생일 | 2026-01-22 15:36 (파이프라인 실행 시점) |
| 영향 범위 | friendship 테이블 데이터 전체 유실 |
| 근본 원인 | CI/CD 테스트가 운영 DB에 연결되어 schema.sql의 DROP TABLE 실행 |
| 심각도 | Critical - 운영 데이터 유실 |

---

## 1. 문제 상황

### 1.1 증상
- 친구 요청 및 수락된 친구 관계 데이터가 모두 사라짐
- `friendship` 테이블이 비어있음 (0건)
- `user` 테이블 데이터는 정상 (7건)
- JPA로 관리되는 테이블은 정상, **MyBatis로 관리되는 테이블만 데이터 유실**

### 1.2 발견 경위
```sql
-- 현재 DB 상태 확인
SELECT COUNT(*) FROM friendship;  -- 결과: 0
SELECT COUNT(*) FROM user;        -- 결과: 7
```

### 1.3 테이블 파일 타임스탬프 분석
```bash
$ ls -la /var/lib/mysql/squiz/ | grep friendship
-rw-r----- 1 mysql mysql 163840 Jan 22 15:42 friendship.ibd  # 재생성됨
-rw-r----- 1 mysql mysql 163840 Jan 22 15:42 user.ibd        # 재생성됨
-rw-r----- 1 mysql mysql 114688 Jan 20 09:44 user_badge.ibd  # 원래 상태
```

**15:42에 friendship, user, user_block 테이블이 재생성됨** - CI/CD 파이프라인 실행 시간과 일치

---

## 2. 원인 분석

### 2.1 핵심 원인: Spring Boot 환경변수 우선순위

**Spring Boot 설정 우선순위:**
```
환경변수 > application-{profile}.properties > application.properties
```

### 2.2 문제의 흐름

#### Step 1: CI/CD test 단계 실행
```yaml
# .gitlab-ci.yml
test:
  script:
    - source /etc/environment  # 환경변수 로드
    - export SPRING_PROFILES_ACTIVE=test
    - ./gradlew test
```

#### Step 2: /etc/environment의 환경변수
```bash
# /etc/environment
SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/squiz?..."  # 운영 DB!
DB_USERNAME="squiz"
DB_PASSWORD="REMOVED_DB_PASSWORD"
```

#### Step 3: 테스트 설정 파일 (무시됨)
```properties
# test/resources/application.properties
spring.datasource.url=${TEST_DB_URL:jdbc:mysql://localhost:3306/ssafy_test_db?...}

# 의도: TEST_DB_URL 환경변수 사용, 없으면 ssafy_test_db 사용
# 현실: SPRING_DATASOURCE_URL 환경변수가 우선 적용되어 운영 DB에 연결!
```

#### Step 4: schema.sql 실행 (운영 DB에서!)
```sql
-- test/resources/schema.sql
DROP TABLE IF EXISTS `friendship`;  -- 운영 DB의 테이블 삭제!
CREATE TABLE `friendship` (...);    -- 빈 테이블 생성
```

### 2.3 왜 user 데이터는 살아있고 friendship만 유실?

| 테이블 | 관리 방식 | schema.sql | 결과 |
|--------|-----------|------------|------|
| user | JPA | DROP 없음 | 데이터 유지 |
| friendship | MyBatis | `DROP TABLE IF EXISTS` | **데이터 유실** |
| user_badge | JPA | DROP 없음 | 데이터 유지 |

**MyBatis 테이블은 JPA가 관리하지 않으므로, schema.sql에서 명시적으로 DROP/CREATE 해야 함**
→ 테스트용 schema.sql이 운영 DB에서 실행되어 데이터 유실

---

## 3. 기술적 상세

### 3.1 Spring Boot 환경변수 우선순위 (높은 순)

1. 명령줄 인자 (`--spring.datasource.url=...`)
2. **OS 환경변수** (`SPRING_DATASOURCE_URL`)
3. application-{profile}.properties
4. application.properties
5. @PropertySource

### 3.2 환경변수 명명 규칙

Spring Boot는 환경변수를 properties로 자동 변환:
```
SPRING_DATASOURCE_URL → spring.datasource.url
```

따라서 `/etc/environment`의 `SPRING_DATASOURCE_URL`이 모든 설정을 오버라이드

### 3.3 테스트 DB 설정의 문제점

```properties
# test/resources/application.properties
spring.datasource.url=${TEST_DB_URL:jdbc:mysql://localhost:3306/ssafy_test_db?...}
```

이 설정은 `TEST_DB_URL` 환경변수만 확인하고, `SPRING_DATASOURCE_URL`은 확인하지 않음.
하지만 Spring Boot가 `SPRING_DATASOURCE_URL`을 `spring.datasource.url`로 자동 매핑하여 우선 적용.

---

## 4. 해결 방법

### 4.1 즉시 조치: CI/CD test 단계 수정

```yaml
# .gitlab-ci.yml - 수정 전
test:
  script:
    - source /etc/environment
    - export SPRING_PROFILES_ACTIVE=test
    - ./gradlew test

# .gitlab-ci.yml - 수정 후
test:
  script:
    - source /etc/environment
    # 테스트 시 운영 DB 연결 방지: 테스트 DB URL로 명시적 오버라이드
    - export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/squiz_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false"
    - export SPRING_PROFILES_ACTIVE=test
    - ./gradlew test
```

### 4.2 근본 해결: 환경변수 분리

#### 방법 A: /etc/environment에서 SPRING_DATASOURCE_URL 제거
```bash
# /etc/environment - 운영 DB URL을 Spring 형식이 아닌 다른 이름으로 변경
PROD_DB_URL="jdbc:mysql://localhost:3306/squiz?..."  # SPRING_ 접두사 제거
TEST_DB_URL="jdbc:mysql://localhost:3306/squiz_test?..."
```

#### 방법 B: 테스트 설정에서 환경변수 무시
```properties
# test/resources/application.properties
# 환경변수를 무시하고 하드코딩
spring.datasource.url=jdbc:mysql://localhost:3306/squiz_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false
```

### 4.3 권장 해결책

**CI/CD에서 테스트 DB URL을 명시적으로 오버라이드** (4.1 방법)

이유:
- 기존 환경변수 구조를 변경하지 않음
- 테스트 단계에서만 안전하게 격리
- 명시적이고 의도가 명확함

---

## 5. 재발 방지 대책

### 5.1 CI/CD 파이프라인 안전장치

```yaml
test:
  script:
    - source /etc/environment
    # 안전장치: 테스트가 운영 DB에 연결되지 않도록 강제
    - export SPRING_DATASOURCE_URL="${TEST_DB_URL}"
    - |
      if echo "$SPRING_DATASOURCE_URL" | grep -q "/squiz?"; then
        echo "ERROR: 테스트가 운영 DB(squiz)에 연결하려고 합니다!"
        exit 1
      fi
    - export SPRING_PROFILES_ACTIVE=test
    - ./gradlew test
```

### 5.2 테스트 시작 시 DB 검증 코드 추가

```java
// src/test/java/com/ssafy/TestDatabaseValidator.java
@Component
@Profile("test")
public class TestDatabaseValidator {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @PostConstruct
    public void validateTestDatabase() {
        if (datasourceUrl.contains("/squiz?") && !datasourceUrl.contains("squiz_test")) {
            throw new IllegalStateException(
                "테스트가 운영 DB에 연결하려고 합니다! URL: " + datasourceUrl
            );
        }
    }
}
```

### 5.3 배포 전 백업 필수화

```yaml
# .gitlab-ci.yml
migrate:
  script:
    # 배포 전 백업 (실패 시 파이프라인 중단)
    - sudo /usr/bin/bash /home/ubuntu/squiz/scripts/backup-db.sh manual
    - ./gradlew flywayMigrate
  allow_failure: false
```

### 5.4 운영 DB 접근 권한 분리 (장기)

테스트용 DB 사용자와 운영용 DB 사용자를 분리:
```sql
-- 테스트 전용 사용자 (squiz DB 접근 불가)
CREATE USER 'squiz_test_user'@'%' IDENTIFIED BY 'test_password';
GRANT ALL ON squiz_test.* TO 'squiz_test_user'@'%';
-- squiz DB에는 권한 부여하지 않음
```

---

## 6. 교훈

### 6.1 환경변수 네이밍의 중요성

Spring Boot의 환경변수 자동 매핑 (`SPRING_DATASOURCE_URL` → `spring.datasource.url`)을 이해해야 함.
운영 환경의 환경변수가 의도치 않게 테스트 설정을 오버라이드할 수 있음.

### 6.2 테스트 격리의 중요성

테스트 환경은 운영 환경과 **물리적으로** 분리되어야 함:
- 별도의 DB 인스턴스 또는 스키마
- 별도의 사용자 계정
- 명시적인 연결 설정

### 6.3 MyBatis vs JPA 관리 테이블

| 구분 | JPA 테이블 | MyBatis 테이블 |
|------|-----------|---------------|
| 스키마 관리 | `ddl-auto=update/validate` | Flyway 또는 수동 SQL |
| 테스트 초기화 | JPA가 자동 관리 | schema.sql에서 DROP/CREATE 필요 |
| 위험도 | 낮음 (JPA가 보호) | **높음** (DROP TABLE 실행 가능) |

---

## 7. 관련 파일

| 파일 | 역할 |
|------|------|
| `.gitlab-ci.yml` | CI/CD 파이프라인 정의 |
| `/etc/environment` | 서버 환경변수 |
| `test/resources/application.properties` | 테스트 설정 |
| `test/resources/schema.sql` | 테스트 DB 스키마 (DROP TABLE 포함) |
| `src/main/resources/db/migration/V1__init.sql` | Flyway 마이그레이션 |

---

## 8. 체크리스트

- [ ] CI/CD test 단계에서 `SPRING_DATASOURCE_URL`을 테스트 DB로 오버라이드
- [ ] 테스트 시작 시 DB URL 검증 코드 추가
- [ ] 배포 전 백업 스크립트 정상 작동 확인
- [ ] 팀원에게 이 문제 공유 및 주의 전달

---

**작성일**: 2026-01-23
**작성자**: Claude Code
**관련 이슈**: CI/CD 테스트 시 운영 DB 데이터 유실
