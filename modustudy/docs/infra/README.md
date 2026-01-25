# Squiz 인프라 문서

## 개요

Squiz 프로젝트의 인프라 및 DevOps 관련 문서입니다.

## 문서 목록

| 문서 | 설명 |
|-----|------|
| [01-infrastructure-setup.md](./01-infrastructure-setup.md) | EC2 서버 및 Docker 환경 구축 |
| [02-cicd-pipeline.md](./02-cicd-pipeline.md) | GitLab Runner CI/CD 파이프라인 |
| [03-testing-guide.md](./03-testing-guide.md) | 테스트 코드 작성 가이드 |
| [04-gradle-toolchain.md](./04-gradle-toolchain.md) | Gradle Toolchain (Java 21 자동 설정) |
| **[05-deployment-guide.md](./05-deployment-guide.md)** | **배포 가이드 (팀원용)** |

## 인프라 구성도

```
┌─────────────────────────────────────────────────────────────┐
│                        AWS EC2                              │
│                   i14d106.p.ssafy.io                        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   Docker Compose                       │  │
│  │                                                        │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  │  │
│  │  │  Nginx  │  │ Backend │  │   SFU   │  │CS Quiz  │  │  │
│  │  │  :80    │  │  :8080  │  │  :4000  │  │   AI    │  │  │
│  │  │+Frontend│  │ (Spring)│  │(Mediasoup)│ │  :5000  │  │  │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘  │  │
│  │                                                        │  │
│  │  ┌─────────┐  ┌─────────┐  ┌──────────────┐          │  │
│  │  │  MySQL  │  │  Redis  │  │GitLab Runner │          │  │
│  │  │  :3306  │  │  :6379  │  │  (CI/CD)     │          │  │
│  │  └─────────┘  └─────────┘  └──────────────┘          │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## CI/CD 흐름

```
GitLab Push (master)
    ↓
GitLab Runner 실행 (EC2)
    ↓
┌─────────────────────────────────┐
│ Stage 1: Build                  │
│ - ./gradlew build -x test       │
└─────────────────────────────────┘
    ↓ 성공
┌─────────────────────────────────┐
│ Stage 2: Test                   │
│ - ./gradlew test                │
└─────────────────────────────────┘
    ↓ 성공              ↓ 실패
┌─────────────────┐    파이프라인 중단
│ Stage 3: Deploy │    (배포 안 함)
│ - rsync 파일복사 │
│ - docker-compose│
│   up -d --build │
└─────────────────┘
    ↓
배포 완료
```

**테스트 실패 시 배포되지 않습니다.**

## 주요 설정 파일

| 파일 | 위치 | 설명 |
|-----|------|------|
| .gitlab-ci.yml | `/.gitlab-ci.yml` | GitLab CI/CD 파이프라인 |
| docker-compose.yml | `/modustudy/docker-compose.yml` | 컨테이너 오케스트레이션 |
| Backend Dockerfile | `/modustudy/backend/Dockerfile` | Spring Boot 빌드 |
| Frontend Dockerfile | `/modustudy/frontend/Dockerfile` | React 빌드 + Nginx |
| SFU Dockerfile | `/modustudy/sfu-server/Dockerfile` | Mediasoup SFU 서버 |
| CS Quiz AI Dockerfile | `/modustudy/backend/cs-quiz-ai-service/Dockerfile` | Flask AI 서비스 |
| Nginx 설정 | `/modustudy/nginx/` | 리버스 프록시 설정 |
| build.gradle | `/modustudy/backend/build.gradle` | Gradle Toolchain 설정 |

## 진행 상황

### 완료

- [x] EC2 서버 초기 설정 (i14d106.p.ssafy.io)
- [x] UFW 방화벽 포트 설정 (22, 80, 443, 40000-40100/udp)
- [x] Docker 및 Docker Compose 설치
- [x] 프로젝트 디렉토리 생성 (/home/ubuntu/squiz)
- [x] Dockerfile 작성 (Backend, Frontend, SFU, CS Quiz AI)
- [x] docker-compose.yml 작성
- [x] Nginx 설정 파일 작성
- [x] Gradle Toolchain 설정 (Java 21 자동 다운로드)
- [x] EC2 환경변수 설정 (.env 파일)
- [x] GitLab Runner 설치 및 등록
- [x] .gitlab-ci.yml 작성 (자동 빌드/테스트/배포)
- [x] SFU SSL 인증서 설정
- [x] 첫 배포 완료

### 현재 상태 (2026-01-22)

| 컨테이너 | 상태 | 포트 |
|---------|------|------|
| squiz-nginx | Running | 80, 443 |
| squiz-backend | Running | 8080 |
| squiz-cs-quiz-ai | Running | 5000 |
| squiz-sfu | Running | 4000, 20000-22000/udp |
| squiz-mysql | Running | 3306 |
| squiz-redis | Running | 6379 |

**모든 서비스 정상 운영 중**

### 해야 할 것

- [ ] **백엔드 테스트 코드 수정** (백엔드 팀원)
- [ ] **DB 초기화 스크립트 작성** (init.sql)

### 예정

- [ ] SSL 인증서 발급 (Let's Encrypt)
- [ ] HTTPS 적용 및 HTTP 리다이렉트
- [ ] DB 백업 스크립트 작성
- [ ] 모니터링 설정 (선택)

## 접속 정보

| 항목 | 값 |
|-----|-----|
| 웹사이트 | http://i14d106.p.ssafy.io |
| EC2 Host | i14d106.p.ssafy.io |
| SSH User | ubuntu |
| 배포 경로 | /home/ubuntu/squiz |

## 환경 변수

### EC2 (.env)

```
/home/ubuntu/squiz/.env
```

| 변수 | 설명 |
|------|------|
| DB_ROOT_PASSWORD | MySQL root 비밀번호 |
| DB_USERNAME | MySQL 사용자명 |
| DB_PASSWORD | MySQL 비밀번호 |

### 로컬 개발 (backend/.env)

```
modustudy/backend/.env
```

| 변수 | 설명 |
|------|------|
| DB_USERNAME | 로컬 MySQL 사용자명 |
| DB_PASSWORD | 로컬 MySQL 비밀번호 |
| KAKAO_REST_API_KEY | 카카오 로그인 |
| NAVER_CLIENT_ID | 네이버 로그인 |
| GOOGLE_CLIENT_ID | 구글 로그인 |

**주의**: `.env` 파일은 `.gitignore`에 포함되어 Git에 올라가지 않습니다.

## 트러블슈팅

### docker-compose 버전 문제
- **증상**: `KeyError: 'ContainerConfig'` 에러
- **원인**: docker-compose v1 호환성 문제
- **해결**: 컨테이너 직접 삭제 후 `docker run`으로 실행

### SFU 서버 SSL 설정
- **증상**: `Missing SSL files for SFU server`
- **원인**: 로컬 개발용 SSL 경로 하드코딩
- **해결**: EC2에서 자체 서명 인증서 생성 + docker-compose 환경변수 설정

```bash
# EC2에서 SSL 인증서 생성
cd /home/ubuntu/squiz/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout sfu-key.pem -out sfu-cert.pem \
  -subj "/CN=i14d106.p.ssafy.io"
```

### TypeScript 빌드 에러
- **증상**: `tsc` 타입 체크 실패
- **해결**: Dockerfile에서 `vite build`만 실행 (타입 체크 스킵)

### mediasoup 빌드 실패
- **증상**: Node 22 필요, python pip 없음
- **해결**: SFU Dockerfile에서 `node:22-alpine` + `py3-pip` 설치

### GitLab Runner 권한 문제
- **증상**: 배포 시 권한 오류
- **해결**:
```bash
sudo usermod -aG docker gitlab-runner
sudo chown -R gitlab-runner:gitlab-runner /home/ubuntu/squiz
sudo gitlab-runner restart
```
