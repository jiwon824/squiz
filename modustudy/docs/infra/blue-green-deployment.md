# Blue/Green 무중단 배포 구현 가이드

## 목차
1. [개요](#1-개요)
2. [현재 아키텍처 분석](#2-현재-아키텍처-분석)
3. [Blue/Green 배포 설계](#3-bluegreen-배포-설계)
4. [구현 상세](#4-구현-상세)
5. [배포 프로세스](#5-배포-프로세스)
6. [롤백 절차](#6-롤백-절차)
7. [CI/CD 통합](#7-cicd-통합)
8. [모니터링 및 검증](#8-모니터링-및-검증)
9. [트러블슈팅](#9-트러블슈팅)

---

## 1. 개요

### 1.1 Blue/Green 배포란?
Blue/Green 배포는 두 개의 동일한 프로덕션 환경(Blue, Green)을 운영하여 무중단 배포를 실현하는 전략입니다.

```
┌─────────────────────────────────────────────────────────────┐
│                        Nginx (Proxy)                         │
│                         Port 80/443                          │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┴───────────────┐
          ▼                               ▼
┌─────────────────────┐       ┌─────────────────────┐
│   Blue Environment  │       │  Green Environment  │
│   (현재 Active)      │       │   (대기/신규 배포)   │
│                     │       │                     │
│  - nginx-blue       │       │  - nginx-green      │
│  - backend-blue     │       │  - backend-green    │
│  - sfu-blue         │       │  - sfu-green        │
│  - cs-quiz-ai-blue  │       │  - cs-quiz-ai-green │
└─────────────────────┘       └─────────────────────┘
          │                               │
          └───────────────┬───────────────┘
                          ▼
              ┌─────────────────────┐
              │   공유 인프라        │
              │  - MySQL            │
              │  - Redis            │
              └─────────────────────┘
```

### 1.2 도입 목적
| 목표 | 설명 |
|------|------|
| 무중단 배포 | 서비스 중단 없이 새 버전 배포 |
| 즉시 롤백 | 문제 발생 시 1초 내 이전 버전으로 복구 |
| 배포 검증 | 트래픽 전환 전 새 버전 테스트 가능 |
| 위험 최소화 | 배포 실패 시 영향 범위 제한 |

### 1.3 적용 범위
| 서비스 | Blue/Green 적용 | 이유 |
|--------|----------------|------|
| backend | ✅ | API 서버, 다운타임 영향 큼 |
| nginx (frontend) | ✅ | 정적 파일 서빙 |
| sfu-server | ✅ | WebRTC 시그널링, 연결 유지 중요 |
| cs-quiz-ai | ✅ | AI 서비스 |
| mysql | ❌ | 데이터 영속성, 단일 인스턴스 |
| redis | ❌ | 세션/캐시, 단일 인스턴스 |

---

## 2. 현재 아키텍처 분석

### 2.1 현재 배포 방식의 문제점

**현재 명령어:**
```bash
docker compose up -d --force-recreate --remove-orphans
```

**문제점:**
```
시간 →  0초      2초      5초      10초
        │        │        │        │
        ▼        ▼        ▼        ▼
     기존 컨테이너  중지    새 컨테이너   준비 완료
        Running   │       Starting   │
                  │                  │
                  └──── 다운타임 ────┘
                      (약 5-10초)
```

### 2.2 현재 Nginx Upstream 설정
```nginx
# 현재: 단일 서버
upstream backend {
    server backend:8080;
}

upstream sfu {
    server sfu-server:4000;
}

upstream cs-quiz-ai {
    server cs-quiz-ai:5000;
}
```

### 2.3 현재 Docker Compose 구조
```yaml
# 모든 서비스가 단일 파일에 정의
services:
  nginx:
    image: tkdgns11/squiz-frontend:latest
  backend:
    image: tkdgns11/squiz-backend:latest
  sfu-server:
    image: tkdgns11/squiz-sfu:latest
  cs-quiz-ai:
    image: tkdgns11/squiz-cs-quiz-ai:latest
  mysql:
    image: mysql:8.0
  redis:
    image: redis:alpine
```

---

## 3. Blue/Green 배포 설계

### 3.1 목표 아키텍처

```
┌────────────────────────────────────────────────────────────────┐
│                      Nginx Proxy (squiz-nginx-proxy)           │
│                          Port 80/443                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    upstream.conf                          │  │
│  │  upstream backend { server backend-blue:8080 weight=100; │  │
│  │                     server backend-green:8080 weight=0; } │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────┬───────────────────────────────┘
                                 │
         ┌───────────────────────┴───────────────────────┐
         ▼                                               ▼
┌─────────────────────────┐               ┌─────────────────────────┐
│    Blue Environment     │               │    Green Environment    │
│    (Active: weight=100) │               │    (Standby: weight=0)  │
│                         │               │                         │
│  ┌───────────────────┐  │               │  ┌───────────────────┐  │
│  │ nginx-blue (:8081)│  │               │  │ nginx-green(:8082)│  │
│  └───────────────────┘  │               │  └───────────────────┘  │
│  ┌───────────────────┐  │               │  ┌───────────────────┐  │
│  │ backend-blue      │  │               │  │ backend-green     │  │
│  │ (:8080)           │  │               │  │ (:8083)           │  │
│  └───────────────────┘  │               │  └───────────────────┘  │
│  ┌───────────────────┐  │               │  ┌───────────────────┐  │
│  │ sfu-blue (:4000)  │  │               │  │ sfu-green (:4001) │  │
│  └───────────────────┘  │               │  └───────────────────┘  │
│  ┌───────────────────┐  │               │  ┌───────────────────┐  │
│  │ cs-quiz-ai-blue   │  │               │  │ cs-quiz-ai-green  │  │
│  │ (:5000)           │  │               │  │ (:5001)           │  │
│  └───────────────────┘  │               └───────────────────────┘
└─────────────────────────┘
         │                                               │
         └───────────────────────┬───────────────────────┘
                                 ▼
                    ┌─────────────────────────┐
                    │     Shared Services     │
                    │  ┌─────────┐ ┌───────┐  │
                    │  │  MySQL  │ │ Redis │  │
                    │  │ (:3306) │ │(:6379)│  │
                    │  └─────────┘ └───────┘  │
                    └─────────────────────────┘
```

### 3.2 트래픽 전환 흐름

```
배포 전:        Blue (100%) ←── 모든 트래픽
                Green (0%)

신규 배포:      Blue (100%)
                Green (0%)  ←── 새 버전 배포 & 헬스체크

트래픽 전환:    Blue (0%)
                Green (100%) ←── nginx reload (무중단)

롤백 시:        Blue (100%) ←── nginx reload (1초 이내)
                Green (0%)
```

### 3.3 파일 구조

```
modustudy/
├── docker-compose.yml           # 공유 서비스 (mysql, redis)
├── docker-compose.blue.yml      # Blue 환경
├── docker-compose.green.yml     # Green 환경
├── docker-compose.proxy.yml     # Nginx Proxy
├── nginx/
│   ├── nginx.conf
│   └── conf.d/
│       ├── default.conf         # 메인 설정
│       └── upstream.conf        # 동적 upstream (스크립트가 관리)
└── scripts/
    ├── deploy-blue-green.sh     # 메인 배포 스크립트
    ├── switch-traffic.sh        # 트래픽 전환
    ├── health-check.sh          # 헬스체크
    └── rollback.sh              # 롤백
```

---

## 4. 구현 상세

### 4.1 Docker Compose 파일

#### docker-compose.yml (공유 서비스)
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: squiz-mysql
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
      - MYSQL_DATABASE=squiz
      - MYSQL_USER=${DB_USERNAME}
      - MYSQL_PASSWORD=${DB_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - squiz-network

  redis:
    image: redis:alpine
    container_name: squiz-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    restart: unless-stopped
    networks:
      - squiz-network

networks:
  squiz-network:
    driver: bridge

volumes:
  mysql-data:
  redis-data:
```

#### docker-compose.blue.yml
```yaml
version: '3.8'

services:
  nginx-blue:
    image: tkdgns11/squiz-frontend:latest
    container_name: squiz-nginx-blue
    expose:
      - "80"
    volumes:
      - ./nginx/nginx-internal.conf:/etc/nginx/nginx.conf:ro
    restart: unless-stopped
    networks:
      - squiz-network

  backend-blue:
    image: tkdgns11/squiz-backend:latest
    container_name: squiz-backend-blue
    expose:
      - "8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/squiz?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - APP_DOMAIN=${APP_DOMAIN:-https://i14d106.p.ssafy.io}
      - JWT_SECRET=${JWT_SECRET}
      - KAKAO_REST_API_KEY=${KAKAO_REST_API_KEY}
      - KAKAO_CLIENT_SECRET_KEY=${KAKAO_CLIENT_SECRET_KEY}
      - NAVER_CLIENT_ID=${NAVER_CLIENT_ID}
      - NAVER_CLIENT_SECRET=${NAVER_CLIENT_SECRET}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 60s
    restart: unless-stopped
    networks:
      - squiz-network

  sfu-blue:
    image: tkdgns11/squiz-sfu:latest
    container_name: squiz-sfu-blue
    expose:
      - "4000"
    ports:
      - "20000-21000:20000-21000/udp"
    environment:
      - NODE_ENV=production
      - SFU_SSL_KEY_PATH=/ssl/sfu-key.pem
      - SFU_SSL_CERT_PATH=/ssl/sfu-cert.pem
      - SFU_ANNOUNCED_IP=i14d106.p.ssafy.io
    volumes:
      - ./ssl:/ssl:ro
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4000/health"]
      interval: 10s
      timeout: 5s
      retries: 3
    restart: unless-stopped
    networks:
      - squiz-network

  cs-quiz-ai-blue:
    image: tkdgns11/squiz-cs-quiz-ai:latest
    container_name: squiz-cs-quiz-ai-blue
    expose:
      - "5000"
    environment:
      - HOST=0.0.0.0
      - PORT=5000
      - DEBUG=False
      - CORS_ORIGINS=https://i14d106.p.ssafy.io,http://localhost:5173
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/health"]
      interval: 10s
      timeout: 5s
      retries: 3
    restart: unless-stopped
    networks:
      - squiz-network

networks:
  squiz-network:
    external: true
    name: modustudy_squiz-network
```

#### docker-compose.green.yml
```yaml
version: '3.8'

services:
  nginx-green:
    image: tkdgns11/squiz-frontend:latest
    container_name: squiz-nginx-green
    expose:
      - "80"
    volumes:
      - ./nginx/nginx-internal.conf:/etc/nginx/nginx.conf:ro
    restart: unless-stopped
    networks:
      - squiz-network

  backend-green:
    image: tkdgns11/squiz-backend:latest
    container_name: squiz-backend-green
    expose:
      - "8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/squiz?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - APP_DOMAIN=${APP_DOMAIN:-https://i14d106.p.ssafy.io}
      - JWT_SECRET=${JWT_SECRET}
      - KAKAO_REST_API_KEY=${KAKAO_REST_API_KEY}
      - KAKAO_CLIENT_SECRET_KEY=${KAKAO_CLIENT_SECRET_KEY}
      - NAVER_CLIENT_ID=${NAVER_CLIENT_ID}
      - NAVER_CLIENT_SECRET=${NAVER_CLIENT_SECRET}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 60s
    restart: unless-stopped
    networks:
      - squiz-network

  sfu-green:
    image: tkdgns11/squiz-sfu:latest
    container_name: squiz-sfu-green
    expose:
      - "4000"
    ports:
      - "21001-22000:20000-21000/udp"
    environment:
      - NODE_ENV=production
      - SFU_SSL_KEY_PATH=/ssl/sfu-key.pem
      - SFU_SSL_CERT_PATH=/ssl/sfu-cert.pem
      - SFU_ANNOUNCED_IP=i14d106.p.ssafy.io
    volumes:
      - ./ssl:/ssl:ro
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4000/health"]
      interval: 10s
      timeout: 5s
      retries: 3
    restart: unless-stopped
    networks:
      - squiz-network

  cs-quiz-ai-green:
    image: tkdgns11/squiz-cs-quiz-ai:latest
    container_name: squiz-cs-quiz-ai-green
    expose:
      - "5000"
    environment:
      - HOST=0.0.0.0
      - PORT=5000
      - DEBUG=False
      - CORS_ORIGINS=https://i14d106.p.ssafy.io,http://localhost:5173
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/health"]
      interval: 10s
      timeout: 5s
      retries: 3
    restart: unless-stopped
    networks:
      - squiz-network

networks:
  squiz-network:
    external: true
    name: modustudy_squiz-network
```

#### docker-compose.proxy.yml
```yaml
version: '3.8'

services:
  nginx-proxy:
    image: nginx:alpine
    container_name: squiz-nginx-proxy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - /var/www/certbot:/var/www/certbot:ro
    restart: unless-stopped
    networks:
      - squiz-network

networks:
  squiz-network:
    external: true
    name: modustudy_squiz-network
```

### 4.2 Nginx 설정

#### nginx/conf.d/upstream.conf (스크립트가 동적 생성)
```nginx
# Active Environment: blue
# Last Updated: 2026-01-23 15:30:00

upstream backend {
    server backend-blue:8080 weight=100;
    server backend-green:8080 weight=0 backup;
}

upstream sfu {
    server sfu-blue:4000 weight=100;
    server sfu-green:4000 weight=0 backup;
}

upstream frontend {
    server nginx-blue:80 weight=100;
    server nginx-green:80 weight=0 backup;
}

upstream cs-quiz-ai {
    server cs-quiz-ai-blue:5000 weight=100;
    server cs-quiz-ai-green:5000 weight=0 backup;
}
```

#### nginx/conf.d/default.conf (수정)
```nginx
# upstream 설정을 별도 파일에서 include
include /etc/nginx/conf.d/upstream.conf;

# HTTPS Server
server {
    listen 443 ssl http2;
    server_name i14d106.p.ssafy.io;

    # SSL 설정
    ssl_certificate /etc/letsencrypt/live/i14d106.p.ssafy.io/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/i14d106.p.ssafy.io/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    # Frontend (정적 파일)
    location / {
        proxy_pass http://frontend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Backend API
    location /api/ {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket
    location /ws/ {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400;
    }

    # SFU Server
    location /sfu/ {
        proxy_pass http://sfu/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400;
    }

    # AI Service
    location /api/ai/ {
        proxy_pass http://cs-quiz-ai/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Health Check (Blue/Green 상태 확인용)
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }

    location /deploy-status {
        access_log off;
        default_type application/json;
        return 200 '{"active": "$ACTIVE_ENV", "timestamp": "$time_iso8601"}';
    }
}

# HTTP → HTTPS Redirect
server {
    listen 80;
    server_name i14d106.p.ssafy.io;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}
```

### 4.3 배포 스크립트

#### scripts/deploy-blue-green.sh
```bash
#!/bin/bash
set -e

# ===========================================
# Blue/Green 배포 스크립트
# ===========================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DEPLOY_PATH="/home/ubuntu/squiz"
STATE_FILE="$DEPLOY_PATH/.deploy-state"
LOG_FILE="$DEPLOY_PATH/logs/deploy-$(date +%Y%m%d-%H%M%S).log"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 로깅 함수
log() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${GREEN}[INFO]${NC} $timestamp - $1" | tee -a "$LOG_FILE"
}

warn() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${YELLOW}[WARN]${NC} $timestamp - $1" | tee -a "$LOG_FILE"
}

error() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${RED}[ERROR]${NC} $timestamp - $1" | tee -a "$LOG_FILE"
}

# 현재 활성 환경 확인
get_active_env() {
    if [ -f "$STATE_FILE" ]; then
        cat "$STATE_FILE"
    else
        echo "blue"
    fi
}

# 비활성 환경 확인
get_inactive_env() {
    local active=$(get_active_env)
    if [ "$active" = "blue" ]; then
        echo "green"
    else
        echo "blue"
    fi
}

# 헬스체크
health_check() {
    local env=$1
    local service=$2
    local max_attempts=30
    local attempt=0

    log "헬스체크 시작: ${service}-${env}"

    while [ $attempt -lt $max_attempts ]; do
        attempt=$((attempt + 1))

        case $service in
            backend)
                if docker exec squiz-backend-${env} curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
                    log "✅ backend-${env} 헬스체크 통과"
                    return 0
                fi
                ;;
            sfu)
                if docker exec squiz-sfu-${env} curl -sf http://localhost:4000/health > /dev/null 2>&1; then
                    log "✅ sfu-${env} 헬스체크 통과"
                    return 0
                fi
                ;;
            cs-quiz-ai)
                if docker exec squiz-cs-quiz-ai-${env} curl -sf http://localhost:5000/health > /dev/null 2>&1; then
                    log "✅ cs-quiz-ai-${env} 헬스체크 통과"
                    return 0
                fi
                ;;
            nginx)
                if docker exec squiz-nginx-${env} curl -sf http://localhost:80 > /dev/null 2>&1; then
                    log "✅ nginx-${env} 헬스체크 통과"
                    return 0
                fi
                ;;
        esac

        log "대기 중... ($attempt/$max_attempts)"
        sleep 5
    done

    error "❌ ${service}-${env} 헬스체크 실패"
    return 1
}

# 모든 서비스 헬스체크
health_check_all() {
    local env=$1
    local failed=0

    for service in backend sfu cs-quiz-ai nginx; do
        if ! health_check "$env" "$service"; then
            failed=1
        fi
    done

    return $failed
}

# 이미지 Pull
pull_images() {
    log "Docker Hub에서 최신 이미지 Pull..."
    docker pull tkdgns11/squiz-backend:latest || true
    docker pull tkdgns11/squiz-frontend:latest || true
    docker pull tkdgns11/squiz-sfu:latest || true
    docker pull tkdgns11/squiz-cs-quiz-ai:latest || true
    log "이미지 Pull 완료"
}

# 비활성 환경 배포
deploy_inactive() {
    local inactive=$(get_inactive_env)
    log "비활성 환경(${inactive}) 배포 시작..."

    cd "$DEPLOY_PATH"

    # 기존 비활성 환경 정리
    docker compose -f docker-compose.${inactive}.yml down --remove-orphans 2>/dev/null || true

    # 새 버전 배포
    docker compose -f docker-compose.${inactive}.yml up -d

    log "비활성 환경(${inactive}) 컨테이너 시작됨"
}

# 트래픽 전환
switch_traffic() {
    local new_active=$1
    log "트래픽 전환: ${new_active}으로 전환 중..."

    # upstream.conf 업데이트
    if [ "$new_active" = "blue" ]; then
        cat > "$DEPLOY_PATH/nginx/conf.d/upstream.conf" << 'EOF'
# Active Environment: blue
# Last Updated: $(date '+%Y-%m-%d %H:%M:%S')

upstream backend {
    server backend-blue:8080 weight=100;
    server backend-green:8080 weight=0 backup;
}

upstream sfu {
    server sfu-blue:4000 weight=100;
    server sfu-green:4000 weight=0 backup;
}

upstream frontend {
    server nginx-blue:80 weight=100;
    server nginx-green:80 weight=0 backup;
}

upstream cs-quiz-ai {
    server cs-quiz-ai-blue:5000 weight=100;
    server cs-quiz-ai-green:5000 weight=0 backup;
}
EOF
    else
        cat > "$DEPLOY_PATH/nginx/conf.d/upstream.conf" << 'EOF'
# Active Environment: green
# Last Updated: $(date '+%Y-%m-%d %H:%M:%S')

upstream backend {
    server backend-blue:8080 weight=0 backup;
    server backend-green:8080 weight=100;
}

upstream sfu {
    server sfu-blue:4000 weight=0 backup;
    server sfu-green:4000 weight=100;
}

upstream frontend {
    server nginx-blue:80 weight=0 backup;
    server nginx-green:80 weight=100;
}

upstream cs-quiz-ai {
    server cs-quiz-ai-blue:5000 weight=0 backup;
    server cs-quiz-ai-green:5000 weight=100;
}
EOF
    fi

    # Nginx reload (무중단)
    docker exec squiz-nginx-proxy nginx -s reload

    # 상태 파일 업데이트
    echo "$new_active" > "$STATE_FILE"

    log "✅ 트래픽 전환 완료: ${new_active}"
}

# 이전 환경 정리 (선택적)
cleanup_old() {
    local old_env=$1
    warn "이전 환경(${old_env}) 유지 중 (롤백 대비)"
    # 필요 시 docker compose -f docker-compose.${old_env}.yml down
}

# 메인 배포 함수
deploy() {
    local active=$(get_active_env)
    local inactive=$(get_inactive_env)

    log "======================================"
    log "Blue/Green 배포 시작"
    log "현재 활성 환경: ${active}"
    log "배포 대상 환경: ${inactive}"
    log "======================================"

    # 1. 이미지 Pull
    pull_images

    # 2. 비활성 환경에 배포
    deploy_inactive

    # 3. 헬스체크
    log "헬스체크 진행 중..."
    if ! health_check_all "$inactive"; then
        error "헬스체크 실패! 배포 중단"
        docker compose -f docker-compose.${inactive}.yml down
        exit 1
    fi

    # 4. 트래픽 전환
    switch_traffic "$inactive"

    # 5. 이전 환경 유지 (롤백 대비)
    cleanup_old "$active"

    log "======================================"
    log "✅ 배포 완료!"
    log "활성 환경: ${inactive}"
    log "======================================"
}

# 롤백 함수
rollback() {
    local active=$(get_active_env)
    local inactive=$(get_inactive_env)

    log "======================================"
    log "롤백 시작"
    log "현재 활성: ${active} → 롤백 대상: ${inactive}"
    log "======================================"

    # 이전 환경이 실행 중인지 확인
    if ! docker ps | grep -q "squiz-backend-${inactive}"; then
        error "롤백 대상 환경(${inactive})이 실행 중이 아닙니다!"
        exit 1
    fi

    # 트래픽 전환 (롤백)
    switch_traffic "$inactive"

    log "✅ 롤백 완료: ${inactive}"
}

# 상태 확인
status() {
    local active=$(get_active_env)
    echo ""
    echo "======================================"
    echo "Blue/Green 배포 상태"
    echo "======================================"
    echo "활성 환경: ${active}"
    echo ""
    echo "Blue 환경:"
    docker ps --filter "name=squiz-*-blue" --format "  {{.Names}}: {{.Status}}"
    echo ""
    echo "Green 환경:"
    docker ps --filter "name=squiz-*-green" --format "  {{.Names}}: {{.Status}}"
    echo ""
    echo "Proxy:"
    docker ps --filter "name=squiz-nginx-proxy" --format "  {{.Names}}: {{.Status}}"
    echo "======================================"
}

# 사용법
usage() {
    echo "Usage: $0 {deploy|rollback|status|switch}"
    echo ""
    echo "Commands:"
    echo "  deploy   - 새 버전을 비활성 환경에 배포하고 트래픽 전환"
    echo "  rollback - 이전 환경으로 즉시 롤백"
    echo "  status   - 현재 배포 상태 확인"
    echo "  switch   - 수동으로 트래픽 전환"
    exit 1
}

# 메인
case "${1:-}" in
    deploy)
        deploy
        ;;
    rollback)
        rollback
        ;;
    status)
        status
        ;;
    switch)
        if [ -z "${2:-}" ]; then
            echo "Usage: $0 switch {blue|green}"
            exit 1
        fi
        switch_traffic "$2"
        ;;
    *)
        usage
        ;;
esac
```

---

## 5. 배포 프로세스

### 5.1 일반 배포 흐름

```
개발자 Push → GitLab CI/CD → 이미지 빌드 → Docker Hub Push
                                    ↓
            ← 배포 완료 ← 트래픽 전환 ← 헬스체크 ← 이미지 Pull
```

### 5.2 상세 단계

| 단계 | 동작 | 다운타임 |
|------|------|----------|
| 1. 이미지 Pull | Docker Hub에서 최신 이미지 다운로드 | 없음 |
| 2. 비활성 환경 배포 | Green 환경에 새 컨테이너 시작 | 없음 |
| 3. 헬스체크 | 모든 서비스 정상 여부 확인 | 없음 |
| 4. 트래픽 전환 | nginx reload로 upstream 변경 | 없음 |
| 5. 모니터링 | 새 환경 정상 동작 확인 | 없음 |

### 5.3 수동 배포 명령어

```bash
# 서버 접속
ssh ubuntu@i14d106.p.ssafy.io

# 배포 실행
cd /home/ubuntu/squiz
./scripts/deploy-blue-green.sh deploy

# 상태 확인
./scripts/deploy-blue-green.sh status

# 롤백 (문제 발생 시)
./scripts/deploy-blue-green.sh rollback
```

---

## 6. 롤백 절차

### 6.1 즉시 롤백 (권장)
```bash
./scripts/deploy-blue-green.sh rollback
```

**소요 시간**: 약 1초 (nginx reload만 수행)

### 6.2 롤백 프로세스
```
문제 감지 → rollback 명령 실행 → nginx reload → 이전 환경으로 트래픽 전환
                                                        ↓
                                              롤백 완료 (1초 이내)
```

### 6.3 롤백 시나리오

| 시나리오 | 대응 |
|----------|------|
| API 오류 증가 | 즉시 rollback 실행 |
| 성능 저하 | 모니터링 후 rollback 결정 |
| 새 버전 버그 발견 | rollback 후 수정 배포 |

---

## 7. CI/CD 통합

### 7.1 .gitlab-ci.yml 수정

```yaml
stages:
  - build
  - test
  - build-images
  - migrate
  - deploy

# ... (기존 build, test, build-images, migrate 단계 유지)

# ===========================================
# Deploy Stage (Blue/Green 배포)
# ===========================================
deploy:
  stage: deploy
  script:
    # Docker Hub 로그인
    - echo "$DOCKER_HUB_TOKEN" | docker login -u "$DOCKER_HUB_USER" --password-stdin

    # 프로젝트 파일 동기화
    - rsync -av --delete --exclude 'backups' --exclude '.git' --exclude 'node_modules' --exclude '.gradle' --exclude 'build' --exclude '.env' --exclude 'ssl' --exclude 'init.sql' --exclude '.deploy-state' modustudy/ $DEPLOY_PATH/

    # Blue/Green 배포 실행
    - chmod +x $DEPLOY_PATH/scripts/deploy-blue-green.sh
    - $DEPLOY_PATH/scripts/deploy-blue-green.sh deploy

    # 배포 상태 확인
    - $DEPLOY_PATH/scripts/deploy-blue-green.sh status
  only:
    - master
    - main
  tags:
    - docker
```

---

## 8. 모니터링 및 검증

### 8.1 배포 후 확인 사항

```bash
# 1. 컨테이너 상태 확인
docker ps | grep squiz

# 2. 활성 환경 확인
cat /home/ubuntu/squiz/.deploy-state

# 3. API 응답 테스트
curl -s https://i14d106.p.ssafy.io/api/health

# 4. 로그 확인
docker logs squiz-backend-$(cat /home/ubuntu/squiz/.deploy-state)
```

### 8.2 헬스체크 엔드포인트

| 서비스 | 엔드포인트 | 정상 응답 |
|--------|-----------|-----------|
| Backend | `/actuator/health` | `{"status":"UP"}` |
| SFU | `/health` | `{"status":"ok"}` |
| AI | `/health` | `{"status":"healthy"}` |
| Frontend | `/` | HTTP 200 |

---

## 9. 트러블슈팅

### 9.1 자주 발생하는 문제

| 문제 | 원인 | 해결 |
|------|------|------|
| 헬스체크 타임아웃 | 서비스 시작 지연 | `start_period` 증가 |
| 트래픽 전환 실패 | nginx 설정 오류 | `nginx -t`로 문법 확인 |
| 롤백 불가 | 이전 환경 중지됨 | 수동으로 이전 환경 재시작 |
| 이미지 Pull 실패 | Docker Hub 인증 | 토큰 재발급 및 갱신 |

### 9.2 로그 확인

```bash
# 배포 로그
cat /home/ubuntu/squiz/logs/deploy-*.log

# 서비스별 로그
docker logs squiz-backend-blue
docker logs squiz-backend-green

# Nginx 로그
docker logs squiz-nginx-proxy
```

### 9.3 긴급 복구

```bash
# 모든 컨테이너 재시작
cd /home/ubuntu/squiz
docker compose -f docker-compose.yml up -d
docker compose -f docker-compose.blue.yml up -d
docker compose -f docker-compose.proxy.yml up -d

# 트래픽을 blue로 강제 전환
./scripts/deploy-blue-green.sh switch blue
```

---

## 부록: 체크리스트

### 배포 전 체크리스트
- [ ] Docker Hub 이미지 빌드 완료
- [ ] 테스트 통과
- [ ] DB 마이그레이션 완료 (필요 시)
- [ ] 백업 완료

### 배포 후 체크리스트
- [ ] 모든 서비스 헬스체크 통과
- [ ] API 응답 정상
- [ ] 로그에 에러 없음
- [ ] 모니터링 지표 정상

---

**작성일**: 2026-01-23
**작성자**: Claude Code
**버전**: 1.0
