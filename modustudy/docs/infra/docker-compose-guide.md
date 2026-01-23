# Docker Compose 가이드

## Docker Compose란?

여러 Docker 컨테이너를 **하나의 파일로 정의**하고 **한 명령어로 관리**하는 도구입니다.

---

## 왜 사용하는가?

### Docker Compose 없이

```bash
# 6개 컨테이너를 각각 실행해야 함
docker run -d --name squiz-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=... \
  -e MYSQL_DATABASE=squiz \
  -v mysql-data:/var/lib/mysql \
  --network squiz-network \
  mysql:8.0

docker run -d --name squiz-redis -p 6379:6379 \
  -v redis-data:/data \
  --network squiz-network \
  redis:alpine

docker run -d --name squiz-backend -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/squiz \
  -e REDIS_HOST=redis \
  --network squiz-network \
  squiz-backend

# ... 더 많은 컨테이너들
```

문제점:
- 명령어가 너무 길고 복잡
- 의존성 순서 직접 관리 필요
- 재현 불가능 (명령어 기억해야 함)

### Docker Compose 사용 시

```bash
# 모든 컨테이너를 한 줄로
docker compose up -d

# 모든 컨테이너 중지
docker compose down

# 상태 확인
docker compose ps
```

---

## docker-compose.yml 구조

```yaml
services:           # 컨테이너 정의
  nginx:            # 서비스 이름
    build: ...      # 이미지 빌드 설정
    ports: ...      # 포트 매핑
    volumes: ...    # 볼륨 마운트
    depends_on: ... # 의존성
    environment: ...# 환경변수

  backend:
    ...

  mysql:
    ...

networks:           # 네트워크 정의
  squiz-network:
    driver: bridge

volumes:            # 볼륨 정의
  mysql-data:
  redis-data:
```

---

## 주요 설정 항목

### 1. build vs image

```yaml
# 직접 빌드 (Dockerfile 필요)
backend:
  build:
    context: ./backend
    dockerfile: Dockerfile

# 기존 이미지 사용
mysql:
  image: mysql:8.0
```

### 2. ports (포트 매핑)

```yaml
ports:
  - "80:80"      # 호스트:컨테이너
  - "443:443"
```

```
[외부] :80 → [컨테이너 내부] :80
```

### 3. volumes (데이터 영속화)

```yaml
volumes:
  # Named Volume (Docker 관리)
  - mysql-data:/var/lib/mysql

  # Bind Mount (호스트 파일 연결)
  - ./nginx/conf.d:/etc/nginx/conf.d:ro
```

| 유형 | 용도 |
|------|------|
| Named Volume | DB 데이터 등 영구 저장 |
| Bind Mount | 설정 파일, 소스 코드 연결 |

### 4. environment (환경변수)

```yaml
environment:
  - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}  # .env 파일에서 가져옴
  - MYSQL_DATABASE=squiz                      # 직접 지정
```

### 5. depends_on (의존성)

```yaml
backend:
  depends_on:
    mysql:
      condition: service_healthy  # healthcheck 통과 후 시작
    redis:
      condition: service_started  # 시작만 확인
```

시작 순서 보장:
```
mysql (healthy) → redis (started) → backend
```

### 6. healthcheck (상태 확인)

```yaml
mysql:
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
    interval: 10s    # 10초마다 체크
    timeout: 5s      # 5초 내 응답 없으면 실패
    retries: 5       # 5번 실패 시 unhealthy
```

### 7. networks (네트워크)

```yaml
networks:
  squiz-network:
    driver: bridge
```

같은 네트워크의 컨테이너는 **서비스명으로 통신**:
```
backend → mysql:3306  (IP 주소 대신 서비스명 사용)
backend → redis:6379
nginx → backend:8080
```

### 8. restart (재시작 정책)

```yaml
restart: unless-stopped
```

| 값 | 동작 |
|-----|------|
| no | 재시작 안 함 |
| always | 항상 재시작 |
| on-failure | 실패 시만 재시작 |
| unless-stopped | 수동 중지 전까지 재시작 |

---

## Squiz 프로젝트 컨테이너 구성

```
docker-compose.yml
├── nginx          # 리버스 프록시 + 프론트엔드
├── backend        # Spring Boot API 서버
├── sfu-server     # WebRTC 미디어 서버 (Mediasoup)
├── cs-quiz-ai     # AI 퀴즈 생성 서버 (Flask)
├── mysql          # 데이터베이스
└── redis          # 캐시/세션 저장소
```

각 컨테이너의 상세 역할과 구조는 [server-architecture.md](./server-architecture.md) 참고.

---

## 자주 사용하는 명령어

```bash
# 모든 컨테이너 시작 (백그라운드)
docker compose up -d

# 모든 컨테이너 시작 + 이미지 재빌드
docker compose up -d --build

# 무중단 배포 (컨테이너 재생성)
docker compose up -d --build --force-recreate --remove-orphans

# 모든 컨테이너 중지 및 삭제
docker compose down

# 볼륨까지 삭제 (주의: 데이터 삭제됨)
docker compose down -v

# 컨테이너 상태 확인
docker compose ps

# 특정 서비스 로그 확인
docker compose logs -f backend

# 특정 서비스만 재시작
docker compose restart backend

# 특정 서비스만 빌드
docker compose build backend
```

---

## .env 파일

민감한 정보는 `.env` 파일에 분리:

```bash
# .env (Git에 포함하지 않음)
DB_ROOT_PASSWORD=비밀번호
DB_USERNAME=squiz
DB_PASSWORD=비밀번호
JWT_SECRET=시크릿키
```

docker-compose.yml에서 참조:
```yaml
environment:
  - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
```

---

## 참고 문서

- [서버 아키텍처 (컨테이너별 상세 설명)](./server-architecture.md)
- [EC2 아키텍처 및 CI/CD](./ec2-architecture.md)
- [Docker Compose v2 가이드](./docker-compose-v2.md)
- [무중단 배포](./zero-downtime-deployment.md)
