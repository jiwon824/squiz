# CI/CD 컨테이너 시작 실패 및 DB 데이터 유실 문제 해결

## 발생일: 2026-01-22

---

## TL;DR (요약)

### 근본 원인
| 문제 | 근본 원인 |
|------|----------|
| 컨테이너 시작 실패 | CI/CD가 컨테이너 시작 완료를 **기다리지 않고** 종료 |
| DB 데이터 유실 | 컨테이너 재생성 시 DB 테이블 재초기화 발생 |
| 복구 불가 | 백업 스크립트가 **권한 문제**로 실패했지만 `|| true`로 **무시됨** |

### 해결 방법
1. `.gitlab-ci.yml` 수정: 컨테이너 시작 대기 로직 추가
2. `.gitlab-ci.yml` 수정: 백업 스크립트 `sudo`로 실행, `|| true` 제거
3. EC2 서버: 백업 디렉토리 권한 설정

### 재발 방지
1. 배포 후 컨테이너 상태 검증 (Exit/Created 상태면 파이프라인 실패)
2. 백업 실패 시 배포 중단
3. 정기 백업 cron 설정
4. `restart: unless-stopped` 정책 추가

---

## 문제 요약

1. **컨테이너 시작 실패**: 파이프라인 배포 후 backend, nginx 컨테이너가 "Created" 상태로 멈춤
2. **DB 데이터 유실**: 배포 과정에서 friendship 테이블 데이터 유실
3. **백업 실패**: 배포 전 DB 백업이 권한 문제로 실패하여 데이터 복구 불가

---

## 1. 컨테이너 시작 실패 문제

### 증상
```bash
$ docker ps -a
NAMES                      STATUS
squiz-backend              Created    # 실행 안됨
squiz-nginx                Created    # 실행 안됨
squiz-mysql                Up (healthy)
squiz-redis                Up
```

### 원인 분석

#### docker-compose.yml 의존성 구조
```yaml
mysql:
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]

backend:
  depends_on:
    mysql:
      condition: service_healthy  # mysql이 healthy 상태여야 시작
    redis:
      condition: service_started

nginx:
  depends_on:
    - backend  # backend가 시작되어야 시작
    - sfu-server
```

#### 문제 발생 과정

1. **파이프라인에서 `docker compose up -d` 실행**
   - `-d` (detached) 모드로 실행하여 즉시 반환
   - 컨테이너 생성만 하고 시작 완료를 기다리지 않음

2. **mysql healthcheck 대기 시간**
   - mysql 컨테이너 시작 후 healthcheck 통과까지 약 10-15초 소요
   - 파이프라인은 기다리지 않고 종료

3. **의존성 체인 실패**
   - mysql이 healthy 되기 전 → backend 시작 안 함 (Created 상태)
   - backend가 없으니 → nginx도 시작 안 함 (Created 상태)

### 해결 방법

#### .gitlab-ci.yml 수정

**수정 전:**
```yaml
deploy:
  stage: deploy
  script:
    - docker compose up -d --build --force-recreate --remove-orphans
    - docker compose ps  # 바로 확인만 하고 끝
```

**수정 후:**
```yaml
deploy:
  stage: deploy
  script:
    - docker compose up -d --build --force-recreate --remove-orphans
    # 컨테이너 시작 대기 (health check 완료까지 최대 60초)
    - |
      echo "Waiting for containers to be healthy..."
      for i in $(seq 1 12); do
        sleep 5
        if docker compose ps | grep -E "starting|unhealthy" > /dev/null; then
          echo "Still waiting... ($i/12)"
        else
          echo "All containers are ready!"
          break
        fi
      done
    # Created 상태 컨테이너 강제 시작
    - docker compose start
    - docker image prune -f
    # 배포 결과 확인 및 검증
    - docker compose ps
    - |
      if docker compose ps | grep -E "Exit|Created" | grep -v "^$" > /dev/null; then
        echo "ERROR: Some containers failed to start!"
        docker compose ps
        exit 1
      fi
```

---

## 2. DB 데이터 유실 문제

### 증상
- 친구 요청 수락 후 정상적으로 친구 목록에 표시됨
- 파이프라인 배포 후 로그인하니 친구 관계 데이터가 모두 사라짐
- 회원 정보는 유지됨 (로그인 가능)

### 원인 분석

#### 타임라인
| 시간 | 이벤트 |
|------|--------|
| 14:30~14:40 | 친구 요청 수락 (정상 작동) |
| **14:45** | **DB 테이블 새로 생성됨 (데이터 유실)** |
| 14:50~14:52 | 컨테이너 재시작 |
| 14:54 | 새 친구 요청 보냄 (PENDING) |

#### 증거
```bash
$ sudo stat /var/lib/docker/volumes/squiz_mysql-data/_data/squiz/friendship.ibd

Birth (생성): 2026-01-22 14:45:48  # 테이블 파일이 새로 생성됨
Modify: 2026-01-22 14:54:44        # 마지막 수정 (PENDING 요청)
```

- friendship.ibd 파일이 14:45에 **새로 생성**됨
- 기존 ACCEPTED 상태 친구 데이터가 모두 유실

#### 원인
- `docker compose up --force-recreate` 실행 시 컨테이너 재생성
- mysql 컨테이너 재생성 과정에서 일부 테이블 데이터 유실 발생
- 볼륨은 유지되었으나 InnoDB 테이블 파일이 재생성됨

---

## 3. 백업 실패 문제

### 증상
```bash
$ ls ~/squiz/backups/
ls: cannot access '/home/ubuntu/squiz/backups/': No such file or directory
```
백업 디렉토리가 존재하지 않음 → 백업이 한 번도 성공한 적 없음

### 원인 분석

#### .gitlab-ci.yml 기존 설정
```yaml
migrate:
  script:
    - ssh ubuntu@localhost "cd /home/ubuntu/squiz && ./scripts/backup-db.sh manual" || true
```

#### 문제점

1. **권한 문제**
   - `/home/ubuntu/squiz` 디렉토리 소유자: `gitlab-runner`
   - 백업 스크립트는 `ubuntu` 유저로 실행
   - 디렉토리 생성/쓰기 권한 없음

2. **`|| true`로 에러 무시**
   - 백업 실패해도 파이프라인 계속 진행
   - 실패 여부 확인 불가

3. **Docker 권한**
   - `ubuntu` 유저가 docker 명령 실행 권한 없을 수 있음

### 해결 방법

#### .gitlab-ci.yml 수정

**수정 전:**
```yaml
migrate:
  script:
    - ssh ubuntu@localhost "cd /home/ubuntu/squiz && ./scripts/backup-db.sh manual" || true
```

**수정 후:**
```yaml
migrate:
  script:
    # 배포 전 DB 백업 (버전 포함) - sudo로 실행하여 권한 문제 해결
    - sudo bash /home/ubuntu/squiz/scripts/backup-db.sh manual
```

- `sudo`로 실행하여 권한 문제 해결
- `|| true` 제거하여 백업 실패 시 파이프라인 중단
- 직접 실행 (ssh localhost 불필요)

---

## 4. 추가 권장 사항

### 4.1 docker-compose.yml에 restart 정책 추가

```yaml
backend:
  ...
  restart: unless-stopped  # 추가

sfu-server:
  ...
  restart: unless-stopped  # 추가
```

### 4.2 자동 백업 cron 설정

```bash
# /etc/cron.d/squiz-backup
# 매일 새벽 3시 백업
0 3 * * * root /home/ubuntu/squiz/scripts/backup-db.sh daily

# 매주 일요일 새벽 4시 주간 백업
0 4 * * 0 root /home/ubuntu/squiz/scripts/backup-db.sh weekly
```

### 4.3 백업 디렉토리 권한 설정

```bash
sudo mkdir -p /home/ubuntu/squiz/backups
sudo chown -R gitlab-runner:gitlab-runner /home/ubuntu/squiz/backups
sudo chmod 755 /home/ubuntu/squiz/backups
```

---

## 5. 검증 방법

### 백업 수동 테스트
```bash
sudo bash /home/ubuntu/squiz/scripts/backup-db.sh manual

# 예상 출력:
# [2026-01-22 15:01:54] ====== Squiz DB 백업 시작 ======
# [2026-01-22 15:01:54] 백업 타입: manual
# [2026-01-22 15:01:55] 백업 완료: /home/ubuntu/squiz/backups/manual/squiz_manual_V1_xxx.sql.gz (16K)
```

### 컨테이너 상태 확인
```bash
docker compose ps

# 모든 컨테이너가 "Up" 상태여야 함
# "Created", "Exit", "unhealthy" 상태가 없어야 함
```

### 백업 파일 확인
```bash
ls -la /home/ubuntu/squiz/backups/manual/

# .sql.gz 파일과 .meta 파일이 있어야 함
```

---

## 6. 관련 파일

- `.gitlab-ci.yml` - CI/CD 파이프라인 설정
- `docker-compose.yml` - 컨테이너 구성
- `scripts/backup-db.sh` - DB 백업 스크립트
- `scripts/restore-db.sh` - DB 복원 스크립트

---

## 7. 교훈

1. **CI/CD에서 컨테이너 시작 완료를 반드시 확인**해야 함
2. **백업은 반드시 성공 여부를 확인**하고, 실패 시 배포를 중단해야 함
3. **`|| true`로 에러를 무시하면 안 됨** - 문제가 숨겨짐
4. **정기 백업 cron 설정**으로 데이터 보호 강화 필요
5. **권한 문제는 사전에 테스트**하여 확인 필요
