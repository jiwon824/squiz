# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## IMPORTANT: 프로젝트 규칙 (Custom Rules)

### 1. 역할 및 범위
- **역할**: 프론트엔드 React 개발자
- **주요 작업 디렉토리**: `modustudy/frontend/`
- **백엔드 격리**: `modustudy/backend/` 디렉토리 내의 파일은 **절대 수정/삭제 금지**. API 명세 파악을 위한 읽기만 허용.

### 2. 언어 정책
- **주석**: 모든 코드 주석은 **한국어**로 작성
- **문서화**: README.md, STRUCTURE.md 등 문서 업데이트 시 한국어 사용
- **사용자 소통**: 모든 메시지는 한국어로 작성

### 3. 프론트엔드 컨벤션
- **Architecture**: `features/[domain-name]/` 기반 도메인별 응집 구조
- **스타일 일관성**: 전역 색상 팔레트와 스타일을 일관되게 유지

### 4. Tailwind CSS 가독성 규칙
복잡한 인라인 클래스는 `cn()` 유틸리티와 `classBuilder`를 활용하여 가독성 향상:

```tsx
// ❌ 나쁜 예 - 긴 인라인 클래스
<button className="inline-flex items-center justify-center px-6 py-2.5 bg-google-blue hover:bg-google-blue-dark text-white font-semibold rounded-lg shadow-sm transition-all">

// ✅ 좋은 예 - classBuilder 활용
import { classBuilder } from '@/shared/utils/cn';
<button className={classBuilder.button('primary', 'md')}>

// ✅ 좋은 예 - cn() 조건부 클래스
import { cn } from '@/shared/utils/cn';
<div className={cn(
  'base-class',
  { 'active-class': isActive },
  customClassName
)}>
```

**규칙**:
- `shared/utils/cn.ts`의 `cn()`, `classBuilder`, `conditionalClasses` 적극 활용
- 반복되는 스타일 패턴은 `classBuilder`에 새 빌더 함수 추가
- 조건부 스타일은 `cn()` 객체 문법 사용

### 5. 커밋 메시지 규칙
```
[feat] dashboard: 새로운 기능 설명
[fix] study: 버그 수정 설명
[docs]: 문서 수정 설명
[style]: 스타일 수정 설명
[refactor]: 리팩토링 설명
```
- 메시지는 **한국어**로 작성

### 6. 안전성
- 파괴적인 작업(파일 대량 삭제 등) 전에는 반드시 사용자에게 확인 요청

---

## Project Overview

ModuStudy (스터디 플랫폼) - 온라인 스터디 협업 서비스. Spring Boot + React + Android (Kotlin) 모노레포 구조.

## Build & Run Commands

### Backend (Spring Boot)
```bash
cd modustudy/backend
./gradlew bootRun                    # 개발 서버 실행
./gradlew build                      # 빌드
./gradlew test                       # 테스트 실행
./gradlew flywayMigrate              # DB 마이그레이션
```

### Frontend (React + Vite)
```bash
cd modustudy/frontend
npm install                          # 의존성 설치
npm run dev                          # 개발 서버 (http://localhost:3000)
npm run build                        # 프로덕션 빌드
npm run lint                         # ESLint 실행
```

### SFU Server (mediasoup)
```bash
cd modustudy/sfu-server
npm start                            # WebRTC SFU 서버 실행
```

### Monitor Server
```bash
cd modustudy/monitor-server
npm run dev                          # 개발 모드 (watch)
npm start                            # 프로덕션 실행
```

### Mobile (Android/Kotlin)
```bash
cd modustudy/mobile
./gradlew assembleDebug              # Debug APK 빌드
```

### Docker
```bash
cd modustudy
docker-compose up -d                 # MySQL, Redis, Monitor 실행
docker-compose -f docker-compose.blue.yml up -d   # Blue 배포
docker-compose -f docker-compose.green.yml up -d  # Green 배포
```

## Architecture

```
modustudy/
├── backend/          # Spring Boot 3.2 (Java 21, Gradle)
├── frontend/         # React 18 + TypeScript + Vite + Tailwind
├── mobile/           # Android (Kotlin, Jetpack Compose)
├── sfu-server/       # mediasoup WebRTC SFU (Node.js)
├── monitor-server/   # 서버 모니터링 (Express)
├── ai-training/      # AI 모델 학습 스크립트 (Python)
├── docs/             # API 문서, 설계 문서
├── nginx/            # Nginx 설정
└── scripts/          # 배포 스크립트 (Blue-Green)
```

### Backend Domain Structure
백엔드는 도메인 기반 패키지 구조 사용: `com.ssafy.domain.{도메인명}`

도메인: admin, ai, attendance, board, comendle, daily, dm, friend, gamification, material, meeting, news, notification, quiz, recruitment, report, retrospect, study, user

각 도메인 패키지 내부 구조:
```
domain/{name}/
├── controller/       # REST API 엔드포인트
├── dto/
│   ├── request/      # 요청 DTO
│   └── response/     # 응답 DTO
├── entity/           # JPA 엔티티
├── repository/       # JPA Repository (QueryDSL 포함)
├── service/          # 비즈니스 로직
└── mapper/           # MyBatis Mapper (일부 도메인)
```

### Frontend Layer Structure
```
src/
├── api/              # Axios 인스턴스, API 엔드포인트
├── features/         # 도메인별 기능 (백엔드 도메인과 1:1 매핑)
├── shared/           # 공통 컴포넌트, 훅, 유틸
├── layouts/          # 페이지 레이아웃
├── routes/           # 라우팅 설정 (PrivateRoute, PublicRoute)
└── store/            # Zustand 전역 상태
```

## Tech Stack

| Layer | Technologies |
|-------|--------------|
| Backend | Spring Boot 3.2, Spring Security 6, JPA/Hibernate, QueryDSL 5, MyBatis, Flyway |
| Frontend | React 18, TypeScript, Vite 5, Zustand, TanStack Query, Tailwind CSS, clsx + tailwind-merge |
| Mobile | Kotlin, Jetpack Compose, Firebase |
| Realtime | WebSocket (STOMP), mediasoup (WebRTC SFU) |
| Database | MySQL 8.0, Redis |
| Auth | JWT (auth0 java-jwt), OAuth 2.0 (Google/Kakao/Naver) |

## API Conventions

- Base URL: `/api/v1/{domain}`
- 응답 형식:
```json
{
  "success": true,
  "data": { ... }
}
```
- 에러 응답:
```json
{
  "success": false,
  "error": { "code": "ERROR_CODE", "message": "..." }
}
```
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## Key Ports

| Service | Port |
|---------|------|
| Backend | 8080 |
| Frontend | 3000 |
| MySQL | 3306 |
| Redis | 6379 |
| SFU Server | (env 설정) |
| Monitor | 9090 |

## Environment

- Java 21 필수 (`JAVA_HOME` 설정)
- Node.js 20+
- Backend: `modustudy/backend/.env` 파일에서 DB 설정
- Flyway 마이그레이션: `src/main/resources/db/migration/`
