# 무중단 배포 (Zero-Downtime Deployment)

## 개요

배포 중에도 서비스가 중단되지 않도록 하는 배포 전략입니다.

---

## 기존 방식의 문제점

### 기존 배포 스크립트

```bash
docker-compose down        # 1. 모든 컨테이너 중지
docker-compose up -d --build  # 2. 새 컨테이너 시작
```

### 문제점

```
시간 ──────────────────────────────────────────────────────▶

     [서비스 운영 중]  │  [중단]  │  [새 서비스 시작]
     ████████████████  │         │  ████████████████
                       │         │
                       ▼         ▼
                   down 실행   up 실행

                   ◀──────────▶
                     다운타임 발생
                     (수 초 ~ 수 분)
```

- `docker-compose down` 실행 시 **모든 컨테이너가 즉시 중지**
- 새 이미지 빌드 + 컨테이너 시작까지 **서비스 불가**
- 빌드 시간이 길수록 다운타임 증가

---

## 무중단 배포 방식

### 새 배포 스크립트

```bash
docker-compose up -d --build --force-recreate --remove-orphans
```

### 동작 원리

```
시간 ──────────────────────────────────────────────────────▶

     [기존 컨테이너 운영 중...]
     ████████████████████████████████████
                         │
                         │ 새 이미지 빌드
                         ▼
                    [새 컨테이너 시작]
                         │
                         ▼
     ████████████████████│███████████████
                         │
                    기존 컨테이너 중지
                    (새 컨테이너가 준비된 후)

                    ◀──▶
                   최소 중단
                   (거의 0초)
```

### 옵션 설명

| 옵션 | 설명 |
|------|------|
| `--build` | 이미지를 새로 빌드 |
| `--force-recreate` | 설정 변경 없어도 컨테이너 재생성 |
| `--remove-orphans` | docker-compose.yml에서 삭제된 서비스의 컨테이너 제거 |
| `-d` | 백그라운드 실행 |

---

## 컨테이너별 재시작 순서

`docker-compose up`은 의존성(`depends_on`)을 고려하여 순차적으로 컨테이너를 재시작합니다:

```
1. mysql, redis (의존성 없음)
       ↓
2. backend (mysql, redis 의존)
       ↓
3. sfu-server, recorder (의존성 없음)
       ↓
4. nginx (backend, sfu-server 의존)
```

각 컨테이너는 **새 컨테이너가 시작된 후** 기존 컨테이너가 중지됩니다.

---

## 비교

| 항목 | 기존 방식 | 무중단 방식 |
|------|----------|------------|
| 다운타임 | 수 초 ~ 수 분 | 거의 0초 |
| 명령어 | `down` → `up` | `up --force-recreate` |
| 안정성 | 단순하고 확실 | 약간 복잡 |
| 리소스 | 적음 | 일시적으로 2배 (새/구 컨테이너 공존) |

---

## 주의사항

### 1. 데이터베이스 마이그레이션

DB 스키마 변경이 있는 경우, 애플리케이션 시작 전 마이그레이션이 필요합니다:

```yaml
# docker-compose.yml
backend:
  depends_on:
    mysql:
      condition: service_healthy
```

### 2. 헬스체크 설정

컨테이너가 실제로 준비되었는지 확인하는 헬스체크 설정 권장:

```yaml
# docker-compose.yml
backend:
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
    interval: 10s
    timeout: 5s
    retries: 3
```

### 3. 롤백

배포 실패 시 이전 버전으로 롤백:

```bash
# 이전 커밋으로 복원
git checkout HEAD~1 -- docker-compose.yml
docker-compose up -d --build --force-recreate
```

---

## GitLab CI/CD 설정

### .gitlab-ci.yml

```yaml
deploy:
  stage: deploy
  script:
    # 프로젝트 파일 동기화
    - rsync -av --delete --exclude '.env' --exclude 'ssl' modustudy/ $DEPLOY_PATH/

    # 무중단 배포
    - cd $DEPLOY_PATH
    - docker-compose up -d --build --force-recreate --remove-orphans

    # 미사용 이미지 정리
    - docker image prune -f

    # 배포 확인
    - docker-compose ps
```

---

## 더 나은 무중단 배포 방법 (고급)

### 1. Blue-Green 배포

두 개의 환경(Blue, Green)을 번갈아가며 배포:

```
             ┌─────────────┐
             │   nginx     │
             │  (프록시)   │
             └──────┬──────┘
                    │
         ┌──────────┴──────────┐
         ▼                     ▼
   ┌───────────┐         ┌───────────┐
   │   Blue    │         │   Green   │
   │ (현재 운영)│         │ (새 버전) │
   └───────────┘         └───────────┘
```

### 2. Rolling Update (Kubernetes)

```yaml
# Kubernetes에서는 자동으로 지원
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

### 3. Traefik / nginx 동적 라우팅

로드밸런서가 헬스체크 후 자동으로 트래픽 전환

---

## 참고

- 현재 구성은 단일 서버이므로 완벽한 무중단은 아님
- 컨테이너 교체 순간 매우 짧은 중단 발생 가능 (1초 미만)
- 완벽한 무중단이 필요하면 로드밸런서 + 다중 서버 구성 필요
