# Squiz 서버 아키텍처

## 개요

Squiz 프로젝트는 Docker Compose 기반으로 여러 서비스가 하나의 EC2 서버에서 운영됩니다.

---

## 전체 아키텍처

```
                                    Internet
                                        │
                                        ▼
                        ┌───────────────────────────────────┐
                        │      i14d106.p.ssafy.io           │
                        │         (EC2 Server)              │
                        └───────────────────────────────────┘
                                        │
                              :80 (HTTP) / :443 (HTTPS)
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              squiz-nginx                                     │
│                     (Reverse Proxy + Frontend Server)                        │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  역할:                                                               │    │
│  │  1. React 정적 파일 서빙 (Frontend)                                  │    │
│  │  2. 리버스 프록시 (Backend, SFU, CS Quiz AI로 라우팅)                │    │
│  │  3. SSL 종료 (HTTPS → HTTP 변환)                                    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
          │                    │                    │                    │
          │ /api/*             │ /ws/*              │ /sfu/*             │ /ai/*
          │ /oauth2/*          │                    │                    │
          ▼                    ▼                    ▼                    ▼
   ┌─────────────┐      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
   │   Backend   │      │   Backend   │      │ SFU Server  │      │ CS Quiz AI  │
   │   :8080     │      │   :8080     │      │   :4000     │      │   :5000     │
   │  (HTTP API) │      │ (WebSocket) │      │ (Mediasoup) │      │  (Flask)    │
   └──────┬──────┘      └─────────────┘      └─────────────┘      └─────────────┘
          │
          ▼
   ┌─────────────┐      ┌─────────────┐
   │    MySQL    │      │    Redis    │
   │   :3306     │      │   :6379     │
   └─────────────┘      └─────────────┘
```

---

## 서비스별 상세 설명

### 1. Nginx (squiz-nginx)

**역할**: 프론트엔드 서버 + 리버스 프록시 + SSL 종료

| 항목 | 값 |
|------|-----|
| 컨테이너명 | squiz-nginx |
| 포트 | 80 (HTTP), 443 (HTTPS) |
| 빌드 경로 | `./frontend/Dockerfile` |
| 설정 파일 | `./nginx/conf.d/default.conf` |

**동작 방식**:
```
[사용자 브라우저]
        │
        │ https://i14d106.p.ssafy.io/
        ▼
   ┌─────────┐
   │  Nginx  │
   └────┬────┘
        │
        ├─ /                    → React 정적 파일 서빙
        ├─ /api/*               → Backend (8080) 프록시
        ├─ /oauth2/*            → Backend (8080) 프록시
        ├─ /ws/*                → Backend WebSocket 프록시
        ├─ /sfu/*               → SFU Server (4000) 프록시
        └─ /ai/*                → CS Quiz AI (5000) 프록시
```

**Dockerfile 빌드 과정**:
```dockerfile
# 1단계: React 빌드
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 2단계: Nginx에 정적 파일 배치
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
```

**핵심 설정** (`default.conf`):
```nginx
# Frontend (React SPA)
location / {
    root   /usr/share/nginx/html;
    index  index.html;
    try_files $uri $uri/ /index.html;  # SPA 라우팅 지원
}

# Backend API 프록시
location /api/ {
    proxy_pass http://backend:8080;
}

# WebSocket 프록시 (채팅, 알림)
location /ws/ {
    proxy_pass http://backend:8080;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

---

### 2. Backend (squiz-backend)

**역할**: REST API 서버 + WebSocket 서버 (Spring Boot)

| 항목 | 값 |
|------|-----|
| 컨테이너명 | squiz-backend |
| 내부 포트 | 8080 |
| 외부 노출 | nginx를 통해 `/api/*`, `/ws/*` |
| 빌드 경로 | `./backend/Dockerfile` |
| 프레임워크 | Spring Boot 3.x (Java 21) |

**기능**:
- REST API (회원, 스터디룸, 퀴즈 등)
- WebSocket (실시간 채팅, 알림)
- OAuth2 소셜 로그인 (카카오)
- JWT 인증/인가

**의존성**:
```
Backend
   ├── MySQL (데이터 저장)
   └── Redis (세션/캐시)
```

**환경변수**:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/squiz
  - REDIS_HOST=redis
  - REDIS_PORT=6379
```

---

### 3. SFU Server (squiz-sfu)

**역할**: WebRTC 미디어 서버 (Mediasoup 기반)

| 항목 | 값 |
|------|-----|
| 컨테이너명 | squiz-sfu |
| 시그널링 포트 | 4000 (WebSocket) |
| 미디어 포트 | 20000-22000/UDP |
| 빌드 경로 | `./sfu-server/Dockerfile` |
| 프레임워크 | Node.js + Mediasoup |

**SFU (Selective Forwarding Unit) 개념**:
```
              ┌─────────────┐
              │  SFU Server │
              │ (Mediasoup) │
              └──────┬──────┘
                     │
       ┌─────────────┼─────────────┐
       │             │             │
       ▼             ▼             ▼
   [User A]      [User B]      [User C]

   - 각 사용자는 자신의 미디어를 SFU에 한 번만 전송
   - SFU가 다른 참여자들에게 선택적으로 전달
   - P2P 대비 클라이언트 부하 감소
```

**포트 설명**:
- `:4000` - WebSocket 시그널링 (nginx 통해 `/sfu/*` 경로로 접근)
- `:20000-22000/UDP` - RTC 미디어 전송 (직접 노출)

**환경변수**:
```yaml
environment:
  - NODE_ENV=production
  - SFU_ANNOUNCED_IP=i14d106.p.ssafy.io  # 클라이언트가 연결할 공인 IP
  - SFU_SSL_KEY_PATH=/ssl/sfu-key.pem
  - SFU_SSL_CERT_PATH=/ssl/sfu-cert.pem
```

---

### 4. CS Quiz AI Server (squiz-cs-quiz-ai)

**역할**: AI 기반 CS 퀴즈 생성 서비스

| 항목 | 값 |
|------|-----|
| 컨테이너명 | squiz-cs-quiz-ai |
| 내부 포트 | 5000 |
| 외부 노출 | nginx를 통해 `/ai/*` |
| 빌드 경로 | `./backend/cs-quiz-ai-service/Dockerfile` |
| 프레임워크 | Flask (Python) |

**기능**:
- CS 지식 기반 퀴즈 자동 생성
- 문제 난이도 조절
- 해설 생성

**환경변수**:
```yaml
environment:
  - HOST=0.0.0.0
  - PORT=5000
  - DEBUG=False
  - CORS_ORIGINS=https://i14d106.p.ssafy.io,http://localhost:5173
```

---

### 5. MySQL (squiz-mysql)

**역할**: 관계형 데이터베이스

| 항목 | 값 |
|------|-----|
| 컨테이너명 | squiz-mysql |
| 이미지 | mysql:8.0 |
| 포트 | 3306 |
| 데이터 볼륨 | mysql-data |

**데이터베이스**:
| DB명 | 용도 |
|------|------|
| squiz | 운영 데이터베이스 |
| squiz_test | 테스트 데이터베이스 (CI/CD) |

**Healthcheck**:
```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
  interval: 10s
  timeout: 5s
  retries: 5
```

---

### 6. Redis (squiz-redis)

**역할**: 세션 저장소 + 캐시

| 항목 | 값 |
|------|-----|
| 컨테이너명 | squiz-redis |
| 이미지 | redis:alpine |
| 포트 | 6379 |
| 데이터 볼륨 | redis-data |

**용도**:
- JWT Refresh Token 저장
- 세션 데이터
- 캐시 (자주 조회되는 데이터)

---

---

## 네트워크 구성

### Docker Network

```yaml
networks:
  squiz-network:
    driver: bridge
```

모든 컨테이너가 `squiz-network`에 연결되어 **서비스명으로 통신** 가능:
- `mysql:3306`
- `redis:6379`
- `backend:8080`
- `sfu-server:4000`
- `cs-quiz-ai:5000`

### 포트 매핑 (외부 노출)

| 포트 | 서비스 | 용도 |
|------|--------|------|
| 80 | nginx | HTTP (→ HTTPS 리다이렉트) |
| 443 | nginx | HTTPS (메인 진입점) |
| 3306 | mysql | DB 직접 접근 (개발용) |
| 4000 | sfu-server | SFU 시그널링 (WebSocket) |
| 5000 | cs-quiz-ai | AI 퀴즈 생성 서비스 |
| 6379 | redis | Redis 직접 접근 (개발용) |
| 20000-22000/UDP | sfu-server | WebRTC 미디어 |

---

## 데이터 영속성

### Docker Volumes

| 볼륨명 | 컨테이너 | 경로 | 용도 |
|--------|----------|------|------|
| mysql-data | squiz-mysql | /var/lib/mysql | DB 데이터 |
| redis-data | squiz-redis | /data | 캐시 데이터 |

### 서버 전용 파일 (Git 미포함)

| 파일/경로 | 용도 |
|-----------|------|
| `.env` | 환경변수 (DB 비밀번호 등) |
| `ssl/` | SSL 인증서 |
| `init.sql` | DB 초기화 스크립트 |

---

## 요청 흐름 예시

### 1. 프론트엔드 페이지 로드

```
브라우저: GET https://i14d106.p.ssafy.io/
    │
    ▼
Nginx (:443)
    │
    ├─ SSL 복호화
    ├─ location / 매칭
    └─ /usr/share/nginx/html/index.html 반환
    │
    ▼
브라우저: React 앱 렌더링
```

### 2. API 호출

```
브라우저: GET https://i14d106.p.ssafy.io/api/users/me
    │
    ▼
Nginx (:443)
    │
    ├─ SSL 복호화
    ├─ location /api/ 매칭
    └─ proxy_pass http://backend:8080
    │
    ▼
Backend (:8080)
    │
    ├─ JWT 검증
    ├─ MySQL 쿼리
    └─ JSON 응답 반환
```

### 3. WebSocket 연결 (채팅)

```
브라우저: wss://i14d106.p.ssafy.io/ws/chat
    │
    ▼
Nginx (:443)
    │
    ├─ SSL 복호화
    ├─ location /ws/ 매칭
    ├─ Upgrade: websocket 헤더 전달
    └─ proxy_pass http://backend:8080
    │
    ▼
Backend (:8080)
    │
    └─ WebSocket 연결 유지 (STOMP)
```

### 4. WebRTC 연결 (화상통화)

```
브라우저: wss://i14d106.p.ssafy.io/sfu/
    │
    ▼
Nginx (:443)
    │
    ├─ SSL 복호화
    ├─ location /sfu/ 매칭
    └─ proxy_pass http://sfu-server:4000
    │
    ▼
SFU Server (:4000)
    │
    └─ 시그널링 처리 (SDP, ICE)

브라우저 ←────── UDP :20000-22000 ──────→ SFU Server
                (미디어 스트림 직접 통신)
```

---

## 컨테이너 의존성

```
                    nginx
                   /     \
                  ▼       ▼
              backend    sfu-server
              /     \
             ▼       ▼
          mysql     redis
          (healthcheck)

cs-quiz-ai (독립)
```

**시작 순서**:
1. mysql (healthcheck 통과 대기)
2. redis
3. backend (mysql, redis 의존)
4. sfu-server
5. cs-quiz-ai (독립)
6. nginx (backend, sfu-server 의존)

---

## 참고

- [EC2 CI/CD 파이프라인](./ec2-architecture.md)
- [CI/CD 트러블슈팅](./cicd-permission-fix.md)
- [테스트 환경 설정](./test-configuration.md)
