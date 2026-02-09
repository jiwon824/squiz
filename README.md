# SQUIZ

<div align="center">

**흩어진 공부를 모으고, 잊혀질 지식을 퀴즈로 묶다**

AI 올인원 스터디 플랫폼

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot)](https://spring.io/)
[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react)](https://react.dev/)
[![Android](https://img.shields.io/badge/Android-Kotlin-3DDC84?logo=android)](https://developer.android.com/)
[![WebRTC](https://img.shields.io/badge/WebRTC-mediasoup-333333?logo=webrtc)](https://mediasoup.org/)

</div>

---

## 소개

SQUIZ는 스터디 모임을 만들고, 만나고, 복습하는 — 학습에 필요한 모든 것을 하나로 통합한 AI 학습 플랫폼입니다.

카톡, 줌, 노션을 따로 쓰던 시대는 끝났습니다. **SQUIZ 하나면 충분합니다.**

---

## 주요 기능

### AI 학습 계획
- 주제만 입력하면 AI가 회차별 커리큘럼을 자동 생성
- 스터디 로드맵으로 현재 진행 상황 시각화

### 워크스페이스
- 스터디별 실시간 채팅
- 자료실에서 파일 공유 및 관리
- 통합 캘린더로 개인 일정 + 스터디 일정 한눈에

### 화상 모임 + AI 자동 정리
- WebRTC 기반 화상 회의
- 모임 종료 후 AI가 자동으로 요약
  - 핵심 내용 정리
  - 오개념 교정
  - 심화 학습 추천
  - 다음에 할 일 목록

### 퀴즈 학습 시스템
- 망각 곡선 기반 복습 스케줄링
- 약점 분석으로 취약 분야 파악
- CS 코스 학습 (운영체제, 네트워크, 데이터베이스 등)
- 오답 노트로 틀린 문제 집중 복습

### 모바일 앱 (Android)
- 블루투스 기반 자동 출석 (오프라인 스터디)
- 푸시 알림으로 스터디 일정 리마인드
- 오프라인 모임 녹음 → AI 자동 정리

### 게이미피케이션
- 학습 활동으로 경험치 획득
- 레벨업 및 뱃지 시스템
- GitHub 잔디 스타일 활동 히트맵

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| **Backend** | Spring Boot 3.2, Spring Security 6, JPA/Hibernate, QueryDSL 5, MyBatis, Flyway |
| **Frontend** | React 18, TypeScript, Vite 5, Zustand, TanStack Query, Tailwind CSS |
| **Mobile** | Kotlin, Jetpack Compose, Firebase, BLE (Bluetooth Low Energy) |
| **Realtime** | WebSocket (STOMP), mediasoup (WebRTC SFU) |
| **AI** | Claude API, Whisper STT, Qwen3 (요약 모델) |
| **Database** | MySQL 8.0, Redis |
| **Auth** | JWT, OAuth 2.0 (Google/Kakao/Naver) |
| **Infra** | Docker, Nginx, Blue-Green 배포 |

---

## 프로젝트 구조

```
modustudy/
├── backend/                  # Spring Boot API 서버
│   └── src/main/java/com/ssafy/domain/
│       ├── ai/               # AI 연동 (요약, 퀴즈 생성)
│       ├── attendance/       # 출석 관리
│       ├── board/            # 게시판
│       ├── calendar/         # 캘린더
│       ├── comendle/         # 꼬멘틀 (단어 게임)
│       ├── daily/            # 데일리 리포트
│       ├── dm/               # 다이렉트 메시지
│       ├── friend/           # 친구 관리
│       ├── gamification/     # 경험치, 뱃지
│       ├── material/         # 자료실
│       ├── meeting/          # 화상 모임
│       ├── news/             # 뉴스 큐레이션
│       ├── notification/     # 알림
│       ├── quiz/             # 퀴즈 시스템
│       ├── recruitment/      # 스터디원 모집
│       ├── report/           # 보고서
│       ├── retrospect/       # 회고
│       ├── study/            # 스터디 관리
│       └── user/             # 사용자 관리
│
├── frontend/                 # React SPA
│   └── src/
│       ├── api/              # API 통신
│       ├── features/         # 도메인별 기능 (백엔드와 1:1 매핑)
│       ├── shared/           # 공통 컴포넌트, 훅, 유틸
│       ├── layouts/          # 페이지 레이아웃
│       ├── routes/           # 라우팅 설정
│       └── store/            # Zustand 전역 상태
│
├── mobile/                   # Android 앱 (Kotlin)
├── sfu-server/               # WebRTC SFU 서버 (mediasoup)
├── ai-inference/             # AI 추론 서버 (FastAPI)
├── monitor-server/           # 서버 모니터링
├── nginx/                    # Nginx 설정
└── scripts/                  # 배포 스크립트
```

---

## 빠른 시작

### 필수 요구사항

| 소프트웨어 | 버전 |
|-----------|------|
| Java (JDK) | 21 |
| Node.js | 20+ |
| MySQL | 8.0 |
| Redis | 7.0+ |
| Docker | 24+ (권장) |

### 1. Docker Compose로 인프라 실행

```bash
cd modustudy
docker-compose up -d
```

### 2. Backend 실행

```bash
cd backend
./gradlew bootRun
```

### 3. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

### 4. 접속 확인

| 서비스 | URL |
|--------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |

---

## 사용 가이드

### 회원가입 및 로그인

1. 메인 페이지에서 **[시작하기]** 클릭
2. 소셜 로그인 선택 (Google / Kakao / Naver)
3. 프로필 정보 입력 후 가입 완료

### 스터디 생성하기

1. 대시보드에서 **[스터디 만들기]** 클릭
2. 스터디 정보 입력 (이름, 설명, 카테고리)
3. **[AI 커리큘럼 생성]** 클릭 → 주제 입력
4. AI가 생성한 커리큘럼 확인 및 수정
5. **[스터디 생성]** 완료

### 워크스페이스 활용

| 탭 | 기능 |
|----|------|
| **채팅** | 실시간 메시지, 핀 고정, 멘션 |
| **자료실** | 파일 업로드/다운로드, 폴더 관리 |
| **캘린더** | 스터디 일정 등록, Google Calendar 연동 |
| **미팅** | 화상 회의 시작, 이전 미팅 기록 조회 |

### 화상 모임 진행

1. 워크스페이스 → **[미팅]** 탭
2. **[미팅 시작]** 클릭
3. 화상 회의 진행
4. 미팅 종료 후 AI 요약 자동 생성
5. 요약 결과 확인 및 PDF/마크다운 저장

### 퀴즈 학습

| 메뉴 | 설명 |
|------|------|
| **오늘의 복습** | 망각 곡선 기반 복습 퀴즈 |
| **코스 학습** | CS 분야별 체계적 학습 |
| **약점 분석** | 분야별 정답률 차트, 취약점 추천 |
| **오답 노트** | 틀린 문제 모아보기, 재학습 |

### 모바일 앱 (Android)

1. APK 설치 후 동일 계정으로 로그인
2. 오프라인 스터디 시 블루투스 출석 사용
   - 스터디장: **[출석 시작]** 클릭
   - 스터디원: 자동 출석 인식 (10m 이내)

---

## 서비스 포트

| 서비스 | 포트 |
|--------|------|
| Backend (Spring Boot) | 8080 |
| Frontend (Vite) | 3000 |
| SFU Server (mediasoup) | 4000 |
| AI Server (FastAPI) | 8000 |
| MySQL | 3306 |
| Redis | 6379 |

---

## 팀 D106

SSAFY 14기 2학기 프로젝트
