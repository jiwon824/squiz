# Dashboard V2

## 📖 개요

"Effortless Learning" 비전을 구현한 새로운 대시보드입니다.

### 핵심 개념
- **자동화된 학습 관리**: 스터디 참여만으로 복습-요약-평가가 자동 완성
- **스터디 기록의 자산화**: 휘발성 대화가 아닌 나만의 학습 데이터베이스로 축적

## 🎯 주요 기능

### 비로그인 사용자 (GuestDashboardV2)
- **Full-width 레이아웃**: 사이드바 제거로 시각적 개방감 제공
- **강력한 히어로 섹션**: "말만 하세요, 공부는 저희가 시켜드릴게요" 메시지
- **스터디 미리보기**: 인기 스터디 카드로 참여 욕구 자극
- **꼬멘틀 체험**: 서비스 핵심 기능을 가볍게 체험

### 로그인 사용자 (UserDashboardV2)
- **STT 리포트 위젯**: 최근 미팅 자동 요약, 키워드 하이라이팅
- **AI 퀴즈 위젯**: 키워드 기반 자동 퀴즈 생성, 정답률 피드백
- **학습 보관함**: 과거 스터디 기록 검색/필터링
- **RightSideBarV2**: 다가오는 미팅 알림 및 원클릭 참여

## 📁 구조

```
dashboard-v2/
├── components/
│   ├── GuestDashboardV2.tsx      # 비로그인 대시보드
│   ├── UserDashboardV2.tsx       # 로그인 대시보드
│   ├── STTReportWidget.tsx       # STT 미팅 리포트 위젯
│   ├── AIQuizWidget.tsx          # AI 퀴즈 위젯
│   └── LearningArchiveWidget.tsx # 학습 보관함 위젯
├── services/                      # API 서비스 (향후 추가)
├── styles/
│   └── DashboardV2.css           # 스타일
├── index.tsx                      # 진입점
└── README.md                      # 이 문서
```

## 🚀 사용 방법

### 1. 개발 서버 실행
```bash
cd modustudy/frontend
npm run dev
```

### 2. 접속
- 기존 대시보드: http://localhost:3000/dashboard
- **V2 대시보드**: http://localhost:3000/dashboard-v2

### 3. 로그인/비로그인 테스트
- 비로그인: `/dashboard-v2` 접속 → GuestDashboardV2 표시
- 로그인: 로그인 후 `/dashboard-v2` 접속 → UserDashboardV2 표시

## 🎨 디자인 시스템

### 색상
- Primary: `var(--color-primary)` (study-blue)
- Teal: `var(--color-teal)` (study-teal)
- Green: `var(--color-green)` (study-green)
- 그라데이션: Purple → Pink, Blue → Teal

### 컴포넌트 패턴
- **위젯 카드**: 흰색 배경, rounded-2xl, shadow-lg
- **헤더**: 그라데이션 배경, 아이콘 + 제목 + 설명
- **인터랙티브**: Framer Motion 애니메이션, hover 효과

## 📦 Mock 데이터

현재 모든 위젯은 Mock 데이터를 사용합니다:
- **STT 리포트**: `MOCK_REPORTS` (STTReportWidget.tsx)
- **AI 퀴즈**: `MOCK_QUIZZES` (AIQuizWidget.tsx)
- **학습 보관함**: `MOCK_ARCHIVES` (LearningArchiveWidget.tsx)
- **미팅 일정**: `MOCK_MEETINGS` (RightSideBarV2.tsx)

실제 API 연동은 백엔드 준비 후 진행합니다.

## 🔄 기존 코드와의 관계

### 충돌 없음!
- ✅ 기존 `/dashboard` 경로는 그대로 유지
- ✅ 기존 `features/dashboard/` 디렉토리 건드리지 않음
- ✅ 기존 `MainLayout.tsx` 건드리지 않음
- ✅ 라우팅 파일에 1줄만 추가 (`/dashboard-v2`)

### 독립적 작동
- V2는 완전히 독립적인 경로로 작동
- 팀원 확인 후 `/dashboard` 경로를 V2로 교체 가능

## 🎯 다음 단계

### 1. 팀원 확인
```bash
# V2 대시보드 확인
http://localhost:3000/dashboard-v2
```

### 2. 피드백 반영
- 디자인 조정
- 위젯 추가/삭제
- Mock 데이터 개선

### 3. API 연동
```typescript
// services/reportApi.ts 생성
export const reportApi = {
  getRecentReports: () => axios.get('/api/v1/report/recent'),
  // ...
};
```

### 4. 승인 후 통합
```typescript
// routes/index.tsx에서:
<Route path="/dashboard" element={<DashboardV2 />} />  // V2로 교체
```

## 🐛 문제 해결

### Import 오류
```bash
# 의존성 재설치
npm install
```

### 스타일 미적용
```typescript
// index.tsx에 CSS import 추가
import './styles/DashboardV2.css';
```

### 라우팅 404
```typescript
// routes/index.tsx 확인
import { DashboardV2 } from '../features/dashboard-v2';
<Route path="/dashboard-v2" element={<DashboardV2 />} />
```

## 📝 커밋 메시지 예시

```
[feat] dashboard: V2 대시보드 구조 생성

- GuestDashboardV2: 비로그인 사용자 전용 Full-width 레이아웃
- UserDashboardV2: STT 리포트, AI 퀴즈, 학습 보관함 위젯 통합
- RightSideBarV2: 미팅 퀵 액세스 기능 추가
- 기존 코드 충돌 없이 /dashboard-v2 경로로 독립 작동
```

## 💡 참고

- 비전: "Effortless Learning" - 의식적 노력 없이 자동 학습 관리
- 가치: 스터디 기록의 자산화 - 대화 → 학습 데이터베이스
- Phase 1~3 완료: 레이아웃 분기, 비로그인 개선, 로그인 위젯 통합
