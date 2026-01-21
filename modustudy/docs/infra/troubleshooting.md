# Flyway 마이그레이션 정리

## 1. Flyway란?

DB 스키마 버전 관리 도구. Git이 코드를 관리하듯, Flyway는 DB 스키마를 관리.

```
V1__init.sql        → 초기 테이블 생성
V2__add_column.sql  → 컬럼 추가
V3__add_index.sql   → 인덱스 추가
```

---

## 2. V1__init.sql 적용 시점

**최초 1회만 실행됨**

```
[첫 배포]
CI migrate → V1 실행 → 테이블 생성
                     → flyway_schema_history에 기록
                       (버전: 1, 체크섬: ABC123)

[이후 배포]
CI migrate → "V1 이미 적용됨" → 스킵
```

---

## 3. 체크섬(Checksum) 확인 시점

**Flyway 실행될 때마다 확인**

```
1. flyway_schema_history 테이블의 체크섬 조회
2. 현재 파일의 체크섬과 비교
3. 불일치 → 에러, 실행 중단
```

| Flyway 실행되는 경우 | 체크섬 확인 |
|---------------------|------------|
| CI `./gradlew flywayMigrate` | O |
| 백엔드 시작 (enabled=true) | O |
| 백엔드 시작 (enabled=false) | X |

---

## 4. 문제가 발생한 원인

```
1. 첫 배포: V1 적용 → 체크섬 -1355212005 저장

2. 누군가 V1__init.sql 수정 → 체크섬 1127389538로 변경

3. 재배포: Flyway 체크섬 비교
   DB: -1355212005 vs 파일: 1127389538
   → 불일치 → 에러 → 502 Bad Gateway
```

**핵심**: 이미 적용된 파일을 수정하면 안 됨

---

## 5. 현재 설정

| 환경 | JPA ddl-auto | Flyway | 동작 |
|------|-------------|--------|------|
| 로컬 | `update` | - | JPA가 스키마 자동 관리 |
| EC2 백엔드 | `validate` | `enabled=false` | 검증만, Flyway 안 돌림 |
| CI migrate | - | `flywayMigrate` | 수동 실행 시 마이그레이션 |

**장점**: 백엔드 시작 시 체크섬 에러 없음
**주의**: 스키마 변경 시 migrate 먼저 실행해야 함

---

## 6. 워크플로우

**모든 배포 시:**
```
build → test → migrate 클릭 → deploy
```

- migrate는 필수 (allow_failure: false)
- 스키마 변경 없으면 "Nothing to migrate"로 바로 통과
- 스키마 변경 있으면 V2, V3 등 적용 후 통과

### 새 마이그레이션 추가 시
```
1. V2__add_xxx.sql 작성 (V1 수정 금지!)
2. Git 푸시
3. build → test → migrate 클릭 → deploy
```

---

## 7. 핵심 규칙

| 규칙 | 이유 |
|------|------|
| **V1 수정 금지** | 체크섬 불일치 발생 |
| **변경은 V2, V3로** | 새 버전은 새 체크섬 |
| **migrate → deploy** | 스키마 먼저, 앱 나중에 |

---

## 8. 관련 파일

```
backend/src/main/resources/
├── application.properties        # 로컬 (ddl-auto=update)
├── application-prod.properties   # EC2 (flyway.enabled=false)
└── db/migration/
    └── V1__init.sql              # 마이그레이션 파일

.gitlab-ci.yml                    # CI 파이프라인 (migrate 단계)
```

---

## 9. 문제 해결 명령어

```bash
# Flyway 히스토리 확인
docker exec -it squiz-mysql mysql -u root -p'REMOVED_DB_PASSWORD' squiz \
  -e "SELECT version, checksum, installed_on FROM flyway_schema_history;"

# 체크섬 초기화 (특정 버전)
docker exec -it squiz-mysql mysql -u root -p'REMOVED_DB_PASSWORD' squiz \
  -e "UPDATE flyway_schema_history SET checksum = NULL WHERE version = '1';"

# Flyway 히스토리 전체 삭제 (주의!)
docker exec -it squiz-mysql mysql -u root -p'REMOVED_DB_PASSWORD' squiz \
  -e "DROP TABLE IF EXISTS flyway_schema_history;"

# 백엔드 재시작
docker compose restart backend

# 로그 확인
docker logs squiz-backend --tail 50
```
