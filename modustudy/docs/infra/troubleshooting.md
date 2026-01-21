# Flyway vs JPA ddl-auto 충돌 해결

## 문제
Flyway와 JPA `ddl-auto`가 둘 다 활성화되어 충돌 발생:
- **로컬**: `ddl-auto=update` (JPA가 스키마 자동 관리)
- **EC2**: `ddl-auto=validate` + Flyway 자동실행 → 충돌

또한 Flyway가 2군데서 실행됨:
1. **CI migrate 단계**: `./gradlew flywayMigrate`
2. **백엔드 시작 시**: Spring Boot 자동 실행

---

## 해결 구조

| 환경 | JPA ddl-auto | Flyway | 역할 |
|------|-------------|--------|------|
| 로컬 | `update` | 비활성화 | JPA가 스키마 자동 관리 |
| EC2 (백엔드) | `validate` | 비활성화 | 검증만, 스키마 변경 안 함 |
| CI migrate | - | 활성화 | 수동으로 스키마 변경 적용 |

---

## 설정 변경

**application-prod.properties:**
```properties
# JPA - 스키마 검증만
spring.jpa.hibernate.ddl-auto=validate

# Flyway - 백엔드 시작 시 자동실행 비활성화 (CI에서만 실행)
spring.flyway.enabled=false
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
```

---

## 올바른 마이그레이션 워크플로우

```
1. 로컬에서 V2__add_column.sql 작성
       ↓
2. Git에 커밋/푸시 (파일이 저장소에 백업됨)
       ↓
3. CI migrate 실행 (수동) → V2를 프로덕션 DB에 적용
       ↓
4. Deploy 실행 → 백엔드 시작 (Flyway 안 돌림, validate만)
```

---

## 앞으로 지켜야 할 규칙
1. **이미 적용된 마이그레이션 파일(V1, V2...) 수정 금지**
2. 스키마 변경이 필요하면 **새 버전 파일 생성** (V3__xxx.sql)
3. CI에서 **migrate → deploy** 순서 유지

---

## 관련 파일
- `backend/src/main/resources/application-prod.properties`
- `.gitlab-ci.yml` (migrate 단계)

---

## 유용한 명령어

### EC2 디버깅
```bash
# 컨테이너 상태 확인
docker ps

# 백엔드 로그 확인
docker logs squiz-backend --tail 100

# MySQL 접속
docker exec -it squiz-mysql mysql -u root -p'REMOVED_DB_PASSWORD' squiz

# Flyway 히스토리 확인
docker exec -it squiz-mysql mysql -u root -p'REMOVED_DB_PASSWORD' squiz -e "SELECT * FROM flyway_schema_history;"

# Flyway 히스토리 초기화 (문제 발생 시)
docker exec -it squiz-mysql mysql -u root -p'REMOVED_DB_PASSWORD' squiz -e "DROP TABLE IF EXISTS flyway_schema_history;"

# 백엔드 재시작
docker compose restart backend
```
