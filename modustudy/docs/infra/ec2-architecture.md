# EC2 서버 아키텍처 및 CI/CD 파이프라인

## 개요

Squiz 프로젝트의 EC2 서버 구성과 GitLab CI/CD 파이프라인 동작 방식을 설명합니다.

---

## CI/CD 파이프라인

### 실행 환경

- **Runner**: GitLab Runner (shell executor)
- **위치**: EC2 서버 (`ip-172-26-15-97`)
- **태그**: `docker`

### 파이프라인 단계

```
┌─────────────────────────────────────────────────────────────────┐
│                        EC2 Server                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────┐    ┌─────────┐    ┌──────────────────────────┐    │
│  │  Build  │ -> │  Test   │ -> │         Deploy           │    │
│  └─────────┘    └─────────┘    └──────────────────────────┘    │
│       │              │                     │                    │
│       v              v                     v                    │
│  gradlew build  gradlew test     docker-compose up --build     │
│  (컴파일 검증)   (MySQL 연결)      (컨테이너 내부 빌드)          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1. Build Stage

```yaml
script:
  - cd modustudy/backend
  - chmod +x gradlew
  - ./gradlew build -x test
```

- **목적**: 코드 컴파일 검증 (fail fast)
- **실행 위치**: EC2 gitlab-runner
- **결과물**: 컴파일 성공 여부만 확인 (jar 파일은 deploy에서 다시 빌드)

### 2. Test Stage

```yaml
script:
  - cd modustudy/backend
  - source /etc/environment && export SPRING_PROFILES_ACTIVE=test && ./gradlew test
```

- **목적**: 단위/통합 테스트 실행
- **실행 위치**: EC2 gitlab-runner
- **DB 연결**: EC2의 `squiz-mysql` 컨테이너 (`squiz_test` 데이터베이스)
- **환경변수**: `/etc/environment`에서 로드

### 3. Deploy Stage

```yaml
script:
  - rsync -av --delete --exclude '.env' --exclude 'ssl' ... modustudy/ $DEPLOY_PATH/
  - cd $DEPLOY_PATH
  - docker-compose down || true
  - docker-compose up -d --build
```

- **목적**: 서비스 배포
- **실행 위치**: EC2 gitlab-runner
- **동작**:
  1. rsync로 소스 파일 복사 (서버 전용 파일 제외)
  2. 기존 컨테이너 중지
  3. Docker 이미지 빌드 및 컨테이너 시작

### 빌드가 2번 되는 이유

| 단계 | 빌드 위치 | 목적 |
|------|----------|------|
| Build Stage | EC2 (gitlab-runner) | 컴파일 검증 (빠른 실패 감지) |
| Deploy Stage | Docker 컨테이너 내부 | 실제 배포용 이미지 생성 |

---

## EC2 서버 구성

### 서버 정보

| 항목 | 값 |
|------|-----|
| 도메인 | `i14d106.p.ssafy.io` |
| 배포 경로 | `/home/ubuntu/squiz` |
| GitLab Runner | shell executor |

### 아키텍처 다이어그램

```
                              ┌─────────────────────────────────────────┐
                              │              EC2 Server                 │
                              │           (i14d106.p.ssafy.io)          │
                              ├─────────────────────────────────────────┤
    Internet                  │                                         │
        │                     │  ┌─────────────────────────────────┐   │
        │    :80/:443         │  │         squiz-nginx             │   │
        └────────────────────>│  │    (Reverse Proxy + Frontend)   │   │
                              │  └───────────────┬─────────────────┘   │
                              │                  │                      │
                              │     ┌────────────┼────────────┐        │
                              │     │            │            │        │
                              │     v            v            v        │
                              │  ┌──────┐   ┌────────┐   ┌────────┐   │
                              │  │ :8080│   │ :4000  │   │ :5000  │   │
                              │  │      │   │        │   │        │   │
                              │  │backend   │sfu-server  │cs-quiz-ai  │
                              │  │      │   │        │   │        │   │
                              │  └──┬───┘   └────────┘   └────────┘   │
                              │     │                                  │
                              │     v                                  │
                              │  ┌──────┐   ┌────────┐                │
                              │  │ :3306│   │ :6379  │                │
                              │  │ mysql│   │ redis  │                │
                              │  └──────┘   └────────┘                │
                              │                                        │
                              │  ┌─────────────────────────────────┐  │
                              │  │       gitlab-runner (shell)     │  │
                              │  │    - build / test / deploy      │  │
                              │  └─────────────────────────────────┘  │
                              └─────────────────────────────────────────┘
```

### Docker 컨테이너

| 컨테이너 | 이미지 | 포트 | 역할 |
|----------|--------|------|------|
| squiz-nginx | 커스텀 빌드 | 80, 443 | Reverse Proxy, Frontend 정적 파일 서빙 |
| squiz-backend | 커스텀 빌드 | 8080 | Spring Boot API 서버, WebSocket |
| squiz-sfu | 커스텀 빌드 | 4000, 20000-22000/udp | Mediasoup SFU (WebRTC) |
| squiz-cs-quiz-ai | 커스텀 빌드 | 5000 | Flask AI 퀴즈 생성 서비스 |
| squiz-mysql | mysql:8.0 | 3306 | 데이터베이스 |
| squiz-redis | redis:alpine | 6379 | 세션/캐시 저장소 |

### 네트워크 구성

```yaml
networks:
  squiz-network:
    driver: bridge
```

- 모든 컨테이너가 `squiz-network` 브릿지 네트워크로 연결
- 컨테이너 간 통신은 서비스명으로 가능 (예: `mysql:3306`, `redis:6379`)

### 데이터 볼륨

| 볼륨 | 용도 |
|------|------|
| mysql-data | MySQL 데이터 영구 저장 |
| redis-data | Redis 데이터 영구 저장 |

### 서버 전용 파일 (Git 미포함)

rsync `--delete` 옵션으로 삭제되지 않도록 exclude 처리된 파일들:

| 파일/디렉토리 | 용도 |
|--------------|------|
| `.env` | DB 비밀번호 등 환경변수 |
| `ssl/` | SSL 인증서 (sfu-key.pem, sfu-cert.pem) |
| `init.sql` | DB 초기화 스크립트 |

---

## 데이터베이스

### MySQL 데이터베이스

| DB 이름 | 용도 | 접근 |
|---------|------|------|
| squiz | 운영 DB | backend 컨테이너 → mysql:3306 |
| squiz_test | 테스트 DB | gitlab-runner → localhost:3306 |

### 환경변수 설정

**EC2 `/etc/environment`**:
```bash
TEST_DB_URL=jdbc:mysql://localhost:3306/squiz_test?...
TEST_DB_USERNAME=<유저명>
TEST_DB_PASSWORD=<비밀번호>
```

**Docker `.env`** (`/home/ubuntu/squiz/.env`):
```bash
DB_ROOT_PASSWORD=<비밀번호>
DB_USERNAME=<유저명>
DB_PASSWORD=<비밀번호>
```

---

## 의존성 관계

```
nginx
  └─> backend
  └─> sfu-server

backend
  └─> mysql (healthcheck 통과 후 시작)
  └─> redis

cs-quiz-ai (독립)
```

---

## 트러블슈팅

### 테스트 실패 (DB 연결 오류)

**증상**: `java.net.ConnectException` - MySQL 연결 실패

**원인**: 이전 배포 실패로 MySQL 컨테이너가 내려간 상태

**해결**:
```bash
cd /home/ubuntu/squiz
docker-compose up -d mysql redis
```

### Docker Compose 타임아웃

**증상**: `UnixHTTPConnectionPool... Read timed out`

**원인**: 기본 타임아웃 60초 초과

**해결**: `.gitlab-ci.yml`에 `COMPOSE_HTTP_TIMEOUT: "300"` 추가

