# 🎨 SQUIZ Frontend

## 개발 환경 설정

### 1. 환경변수 설정 (필수)

프로젝트 루트(`frontend/`)에 `.env` 파일을 생성하세요:

```bash
# frontend/.env
VITE_API_URL=http://localhost:8080
```

> **참고**: `.env` 파일은 Git에 커밋되지 않습니다.

### 2. 의존성 설치 및 실행

```bash
npm install
npm run dev
```

### 환경별 API 동작 방식

| 환경 | VITE_API_URL | API 호출 방식 |
|------|-------------|--------------|
| 로컬 개발 | `http://localhost:8080` | 백엔드 직접 호출 |
| 배포 (EC2) | 미설정 | 상대경로 → nginx 프록시 |

```
┌─────────────────────────────────────────────────────┐
│ 로컬 개발                                            │
│   브라우저 → http://localhost:8080/api/...          │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ 배포 환경                                            │
│   브라우저 → /api/... → nginx → backend:8080        │
└─────────────────────────────────────────────────────┘
```

---

## 프로젝트 구조

```
frontend/
├── public/                 # 정적 자원
└── src/
    ├── main.tsx            # 엔트리 포인트
    ├── App.tsx             # 전역 설정 (QueryClient, Theme, 전역 가드)
    │
    ├── api/                # API 통신 관련
    │   ├── axios.ts        # Axios 인스턴스 (Interceptor, 토큰 주입)
    │   └── endpoints/      # 도메인별 API 함수
    │
    ├── assets/             # 스타일 및 미디어
    │   ├── images/
    │   ├── icons/
    │   └── styles/         # 전역 CSS
    │
    ├── shared/             # 전역 공통 모듈
    │   ├── components/     # UI Kit (Button, Modal, Input, Spinner 등)
    │   ├── hooks/          # useAuth, useSocket, useIntersectionObserver
    │   ├── utils/          # date-formatter, storage-util
    │   └── types/          # 전역 공통 타입
    │
    ├── layouts/            # 페이지 레이아웃
    │   ├── MainLayout.tsx
    │   ├── AuthLayout.tsx
    │   └── WorkspaceLayout.tsx
    │
    ├── features/           # 도메인별 기능 응집화 (백엔드 기준 통일)
    │   ├── user/           # 사용자 인증, 프로필, 소셜 계정
    │   ├── friend/         # 친구 관계 관리
    │   ├── dm/             # 다이렉트 메시지
    │   ├── study/          # 스터디 관리, 멤버, 세션
    │   ├── chat/           # 채팅/채널 관리, 메시지
    │   ├── meeting/        # WebRTC 화상 회의, 참가자, 회의록
    │   ├── quiz/           # 퀴즈 대회, 문제, 코스
    │   ├── attendance/     # 출석 관리, 세션 메모
    │   ├── retrospect/     # 회고 작성, 회고 항목
    │   ├── gamification/   # 배지, 경험치, 레벨 시스템
    │   ├── material/       # 학습 자료, 커리큘럼, 진도
    │   ├── notification/   # 알림 관리, 알림 설정, FCM
    │   ├── report/         # 보고서 관리
    │   ├── daily/          # 데일리 리포트, 일일 항목
    │   ├── board/          # 게시판, 댓글, 좋아요
    │   ├── recruitment/    # 팀원 모집, 지원 관리
    │   ├── comendte/       # 꼬멘틀 게임
    │   ├── news/           # IT 뉴스, 북마크
    │   ├── ai/             # AI 피드백 서비스
    │   ├── dashboard/      # 홈 대시보드 (프론트 전용)
    │   └── admin/          # 관리자 페이지 (프론트 전용)
    │
    ├── routes/             # 경로 설정
    │   ├── index.tsx       # 메인 라우터 (Lazy Loading 적용)
    │   ├── PublicRoute.tsx # 비로그인 사용자 전용
    │   └── PrivateRoute.tsx # 로그인 필수 가드
    │
    ├── store/              # 전역 상태 (Zustand)
    │   ├── authStore.ts
    │   ├── workspaceStore.ts
    │   └── uiStore.ts
    │
    └── types/              # 도메인별 상세 타입 정의
```

---

## 🎨 중앙컨트롤 Design System 2.0

### 🏗️ 구조 개요

Google Material Design을 기반으로 한 체계적인 디자인 시스템입니다.

```
src/shared/design-system/
├── tokens/             # 🎯 디자인 토큰 (색상, 간격, 타이포그래피 등)
│   ├── colors.ts       # Google 색상 시스템
│   ├── spacing.ts      # 8px 기반 스페이싱
│   ├── typography.ts   # 타이포그래피 시스템
│   ├── effects.ts      # 그림자, border-radius
│   └── index.ts        # 토큰 통합 export
├── primitives/         # 🧱 기본 컴포넌트
│   └── Box.tsx         # 레이아웃 기본 컴포넌트
├── utils/              # 🛠️ 유틸리티 함수
│   └── cn.ts           # clsx 기반 클래스명 조합
└── index.ts            # 메인 export
```

### 🎨 Google-Style 색상 시스템

**메인 베이스 (흰색/검정색)**
```typescript
colors: {
  white: '#FFFFFF',        // 메인 배경
  black: '#202124',        // 메인 텍스트
  gray: {
    50: '#F8F9FA',         // 페이지 배경
    100: '#E8EAED',        // 경계선, 구분선
    300: '#DADCE0',        // 비활성 요소
    500: '#5F6368',        // 보조 텍스트
    700: '#3C4043',        // 서브 타이틀
  }
}
```

**Google 포인트 색상 (다채로운 액센트)**
```typescript
google: {
  blue: '#4285F4',         // 주요 액션, 링크
  red: '#EA4335',          // 오류, 경고
  yellow: '#FBBC04',       // 알림, 주의
  green: '#34A853',        // 성공, 완료
}
```

### 📏 스페이싱 시스템 (8px 기반)

```typescript
spacing: {
  xs: '0.5rem',   // 8px
  sm: '1rem',     // 16px
  md: '1.5rem',   // 24px
  lg: '2rem',     // 32px
  xl: '3rem',     // 48px
}
```

### 🔤 타이포그래피 시스템

```typescript
textStyles: {
  heading: {
    h1: 'text-4xl font-bold text-black leading-tight',
    h2: 'text-3xl font-semibold text-black leading-tight',
    h3: 'text-2xl font-semibold text-black leading-normal',
  },
  body: {
    large: 'text-lg font-normal text-gray-700 leading-relaxed',
    default: 'text-base font-normal text-gray-700 leading-normal',
    small: 'text-sm font-normal text-gray-500 leading-normal',
  }
}
```

---

## 🛠️ clsx & tailwind-merge 사용법

### clsx 개념

`clsx`는 **조건부 클래스명을 쉽게 조합**할 수 있게 해주는 유틸리티 라이브러리입니다.

**기본 사용법:**
```javascript
import { clsx } from 'clsx';

// 기본 사용
clsx('btn', 'btn-primary');  // "btn btn-primary"

// 조건부 클래스
clsx('btn', {
  'btn-primary': isPrimary,
  'btn-disabled': isDisabled,
});

// 배열과 객체 혼합
clsx([
  'btn',
  {
    'btn-primary': isPrimary,
    'btn-secondary': !isPrimary,
  },
  additionalClasses
]);
```

**실전 예제:**
```jsx
const Button = ({ variant, size, disabled, className }) => {
  const classes = clsx(
    'px-4 py-2 rounded font-medium',  // 기본 클래스
    {
      'bg-blue-500 text-white': variant === 'primary',
      'bg-gray-200 text-gray-800': variant === 'secondary',
      'opacity-50 cursor-not-allowed': disabled,
      'px-3 py-1.5 text-sm': size === 'small',
      'px-6 py-3 text-lg': size === 'large',
    },
    className  // 추가 클래스
  );
  
  return <button className={classes} disabled={disabled} />;
};
```

### tailwind-merge 개념

`tailwind-merge`는 **Tailwind 클래스의 충돌을 해결**해주는 도구입니다.

**문제점:**
```javascript
// 마지막 클래스만 적용됨 (예측하기 어려움)
<div className="p-4 p-6" />  // p-6이 적용될까? p-4가 적용될까?
```

**해결책:**
```javascript
import { twMerge } from 'tailwind-merge';

twMerge('p-4 p-6');  // "p-6" (나중 것이 덮어씀)
twMerge('bg-red-500 bg-blue-500');  // "bg-blue-500"
twMerge('px-4 py-2 p-6');  // "p-6" (더 구체적인 것이 남음)
```

### cn() 유틸리티 함수

두 라이브러리를 결합한 **최적화된 유틸리티 함수**:

```typescript
// src/shared/utils/cn.ts
import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

**사용 예시:**
```jsx
import { cn } from '@/shared/utils/cn';

// 1. 기본 사용
<div className={cn('p-4 bg-white rounded')} />

// 2. 조건부 클래스
<button 
  className={cn(
    'px-4 py-2 rounded font-medium',
    {
      'bg-google-blue text-white': variant === 'primary',
      'bg-google-red text-white': variant === 'danger',
      'opacity-50': disabled,
    }
  )} 
/>

// 3. 클래스 덮어쓰기 (tailwind-merge 효과)
<div 
  className={cn(
    'p-4 bg-gray-100',  // 기본값
    'p-6',              // p-6이 p-4를 덮어씀
    userClassName       // 사용자 클래스가 모든 것을 덮어씀
  )} 
/>

// 4. 디자인 시스템과 함께
import { textStyles } from '@/shared/design-system';

<h1 className={cn(textStyles.heading.h1, 'mb-4')} />
```

### 클래스 빌더 패턴

복잡한 컴포넌트를 위한 **체계적인 클래스 관리**:

```typescript
// 버튼 컴포넌트 예시
const ButtonVariants = {
  variant: {
    primary: 'bg-google-blue hover:bg-google-blue-dark text-white',
    secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-700',
    outline: 'border border-google-blue text-google-blue hover:bg-google-blue-light',
  },
  size: {
    sm: 'px-3 py-1.5 text-xs rounded-md',
    md: 'px-6 py-2.5 text-sm rounded-lg',
    lg: 'px-8 py-3.5 text-base rounded-xl',
  }
};

const Button = ({ variant = 'primary', size = 'md', className, ...props }) => {
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center font-semibold transition-colors',
        ButtonVariants.variant[variant],
        ButtonVariants.size[size],
        className
      )}
      {...props}
    />
  );
};
```

### 성능 최적화 팁

1. **클래스 문자열 캐싱**
```jsx
const memoizedClasses = useMemo(() => 
  cn('base-class', { 'condition-class': condition })
, [condition]);
```

2. **정적 클래스와 동적 클래스 분리**
```jsx
const staticClasses = 'px-4 py-2 rounded font-medium';
const dynamicClasses = cn({
  'bg-blue-500': isPrimary,
  'bg-gray-200': !isPrimary,
});
```

---

## 🚀 Design System 사용법

### 1. 토큰 직접 사용

```typescript
import { colors, spacing, textStyles } from '@/shared/design-system/tokens';

// CSS-in-JS에서 사용
const styles = {
  backgroundColor: colors.google.blue,
  padding: spacing.md,
  margin: spacing.lg,
};

// Tailwind 클래스로 사용
<div className={textStyles.heading.h2} />
```

### 2. Primitive 컴포넌트 사용

```jsx
import { Box, Container } from '@/shared/design-system';

// Box 컴포넌트 (디자인 토큰 기반 props)
<Box 
  bg="google-blue" 
  p="md" 
  rounded="lg"
  shadow="md"
>
  Content
</Box>

// Container 컴포넌트 (페이지 래퍼)
<Container size="xl" centered>
  <h1>Page Title</h1>
</Container>
```

### 3. 유틸리티 함수 활용

```jsx
import { cn, classBuilder } from '@/shared/design-system';

// 클래스 조합
<button className={cn(
  classBuilder.button('primary', 'md'),
  'additional-class'
)} />

// 조건부 스타일링
<div className={cn(
  'base-style',
  {
    'active-style': isActive,
    'disabled-style': isDisabled,
  }
)} />
```

### 4. 컴포넌트 변형 시스템

```jsx
// 버튼 컴포넌트 예시
const Button = ({ variant, size, className, ...props }) => {
  const classes = cn(
    // 기본 스타일
    'inline-flex items-center justify-center font-semibold transition-all',
    // 변형별 스타일
    {
      'bg-google-blue text-white': variant === 'primary',
      'bg-google-green text-white': variant === 'success',
      'border border-google-blue text-google-blue': variant === 'outline',
    },
    // 크기별 스타일
    {
      'px-3 py-1.5 text-xs': size === 'sm',
      'px-6 py-2.5 text-sm': size === 'md',
      'px-8 py-3.5 text-base': size === 'lg',
    },
    className
  );
  
  return <button className={classes} {...props} />;
};
```

---

## 🎯 마이그레이션 가이드

### 기존 코드 → Design System

**Before (하드코딩):**
```jsx
<button 
  className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
  style={{ backgroundColor: '#4285F4' }}
>
  Click me
</button>
```

**After (Design System):**
```jsx
import { Button } from '@/shared/components';

<Button variant="primary" size="md">
  Click me
</Button>
```

**Before (CSS 클래스):**
```css
.custom-card {
  background: #ffffff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
```

**After (Box 컴포넌트):**
```jsx
import { Box } from '@/shared/design-system';

<Box 
  bg="white" 
  p="lg" 
  rounded="xl" 
  shadow="md"
  className="custom-card"
>
  Card Content
</Box>
```

---

## 디자인 시스템

### 색상 팔레트

#### 라이트 모드 (기본)
```css
--color-primary: #80A1BA      /* Muted Blue - 헤더, 주요 버튼 */
--color-secondary: #91C4C3    /* Soft Teal - 사이드바 선택 */
--color-accent: #B4DEBD       /* Pale Green - 완료 상태 */
--color-bg: #FFF7DD           /* Cream White - 전체 배경 */
--text-main: #454a4f          /* 짙은 회색 - 주요 텍스트 */
```

#### 다크 모드
```css
--color-primary: #6A8AA3
--color-secondary: #7AB3B2
--color-accent: #9FD1A9
--color-bg: #1a1d23
--text-main: #e4e6eb
```

### 다크 모드 전환

```javascript
// 다크 모드 활성화
document.documentElement.setAttribute('data-theme', 'dark');

// 라이트 모드로 복귀
document.documentElement.removeAttribute('data-theme');

// 토글
const toggleTheme = () => {
  const current = document.documentElement.getAttribute('data-theme');
  document.documentElement.setAttribute('data-theme', current === 'dark' ? '' : 'dark');
};
```

### CSS 변수 사용법

```css
/* 색상 */
.my-component {
  background-color: var(--color-primary);
  color: var(--color-text-inverse);
  border: 1px solid var(--border-light);
}

/* 간격 */
.card {
  padding: var(--spacing-md) var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
  border-radius: var(--radius-main);
}

/* 애니메이션 */
.smooth-element {
  transition: var(--transition-smooth);
}
```

### 유틸리티 클래스

```html
<!-- 레이아웃 -->
<div class="flex-between">
  <span>왼쪽</span>
  <span>오른쪽</span>
</div>

<!-- 텍스트 -->
<h1 class="text-primary font-bold">제목</h1>
<p class="text-secondary text-sm">설명</p>

<!-- 카드 -->
<div class="bg-surface shadow-md rounded-lg border-light">
  카드 내용
</div>

<!-- 애니메이션 -->
<div class="animate-fade-in">페이드 인</div>
<div class="animate-slide-in-up">슬라이드 업</div>
```

---

## 🏗️ 구글 캘린더 스타일 컴포넌트

### 1. 사이드바 (밀려나오는 애니메이션)

```jsx
<div className="app-container">
  <aside className="sidebar">
    {/* 사이드바 내용 */}
  </aside>
  <main>
    {/* 메인 콘텐츠 */}
  </main>
</div>
```

```css
/* 사이드바 닫기 */
.sidebar.collapsed {
  transform: translateX(-100%);
}
```

### 2. 이벤트 카드

```jsx
<div className="event-item">
  스터디 회의
</div>
```

### 3. 아이콘 버튼

```jsx
<button className="icon-btn">
  <svg>{/* 아이콘 */}</svg>
</button>
```

---

## 📐 주요 디자인 토큰

### Border Radius
```css
--radius-main: 8px        /* 구글 스타일 기본 */
--radius-pill: 24px       /* 알약 모양 */
--radius-full: 9999px     /* 완전한 원형 */
```

### Transitions
```css
--transition-smooth: all 0.3s cubic-bezier(0.4, 0, 0.2, 1)  /* 사이드바용 */
--transition-fast: 150ms ease-in-out
--transition-base: 250ms ease-in-out
```

### Spacing
```css
--spacing-xs: 0.25rem     /* 4px */
--spacing-sm: 0.5rem      /* 8px */
--spacing-md: 1rem        /* 16px */
--spacing-lg: 1.5rem      /* 24px */
--spacing-xl: 2rem        /* 32px */
```

### Layout
```css
--header-height: 64px
--sidebar-width: 280px
--container-max-width: 1280px
```

---

## ⚠️ 가독성 주의사항

연한 배경 위에 연한 색상 텍스트는 가독성이 떨어집니다:

```css
/* ❌ 나쁜 예 */
.bad-text {
  background: #FFF7DD;
  color: #B4DEBD;  /* 잘 안 보임! */
}

/* ✅ 좋은 예 - 텍스트는 짙게 */
.good-text {
  background: #FFF7DD;
  color: var(--text-main);
}

/* ✅ 또는 배경으로 활용 */
.good-background {
  background: #B4DEBD;
  color: var(--text-main);
}
```

---

## 🚀 실전 예제

### 카드 컴포넌트

```css
.study-card {
  background-color: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  transition: var(--transition-smooth);
}

.study-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.study-card-title {
  color: var(--color-text-primary);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--spacing-sm);
}
```

### 버튼 컴포넌트

```css
.btn-primary {
  background-color: var(--color-primary);
  color: var(--color-text-inverse);
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--radius-main);
  border: none;
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: var(--transition-fast);
}

.btn-primary:hover {
  background-color: var(--color-primary-dark);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}
```

---

## 💡 성능 최적화

### 🚀 1. GPU 가속 활용

**transform 사용으로 하드웨어 가속 활성화:**

```css
/* ✅ 권장: transform 사용 (GPU 가속) */
.sidebar {
  transform: translateX(0);
  transition: transform var(--transition-smooth);
  will-change: transform;  /* 애니메이션 힌트 제공 */
}

.sidebar.collapsed {
  transform: translateX(-100%);
}

/* ❌ 비권장: left/margin 사용 (레이아웃 리플로우 발생) */
.sidebar {
  left: 0;
  transition: left var(--transition-smooth);
}

.sidebar.collapsed {
  left: -280px;  /* CPU 기반 레이아웃 재계산 발생 */
}
```

**GPU 가속 가능한 속성:**
- ✅ `transform` (translateX, translateY, scale, rotate)
- ✅ `opacity`

**피해야 할 속성 (레이아웃 리플로우 발생):**
- ❌ `width`, `height`
- ❌ `margin`, `padding`
- ❌ `top`, `left`, `right`, `bottom`
- ❌ `border-width`

### 🎨 2. 그림자 최소화

렌더링 부담을 줄이기 위해 그림자를 최소화했습니다:

```css
/* 현재 적용된 그림자 (최적화됨) */
--shadow-xs: 0 1px 2px   /* 매우 미세 */
--shadow-sm: 0 2px 4px   /* 작음 */
--shadow-md: 0 4px 8px   /* 중간 */
```

**성능 팁:**
- blur 값이 클수록 렌더링 비용 증가
- 여러 겹의 그림자보다 단일 그림자 권장
- 필요한 곳에만 사용

### 🔄 3. CSS 변수로 테마 전환 최적화

CSS 변수 사용으로 테마 전환 시 리플로우 최소화:

```javascript
// 테마 전환 (리플로우 최소화)
document.documentElement.setAttribute('data-theme', 'dark');
```

```css
/* 라이트 모드 */
:root {
  --color-bg: #FFF7DD;
  --color-text: #454a4f;
}

/* 다크 모드 */
[data-theme="dark"] {
  --color-bg: #1a1d23;
  --color-text: #e4e6eb;
}

/* 컴포넌트는 변수만 참조 */
.element {
  background: var(--color-bg);
  color: var(--color-text);
}
```

**장점:**
- ✅ 개별 요소 스타일 재계산 불필요
- ✅ 변수만 변경되므로 리플로우 최소화
- ✅ 부드러운 테마 전환 가능

### ⚡ 4. will-change 사용 가이드

```css
/* ✅ 올바른 사용 */
.sidebar {
  will-change: transform;  /* 실제로 애니메이션되는 속성만 */
}

/* ❌ 잘못된 사용 */
* {
  will-change: transform, opacity;  /* 모든 요소에 적용 금지! */
}
```

**주의사항:**
- 애니메이션이 실제로 발생하는 요소에만 사용
- 너무 많은 요소에 사용하면 오히려 성능 저하
- 애니메이션 완료 후 제거 권장 (JavaScript로 제어)

### 📊 5. 성능 체크리스트

**애니메이션 최적화:**
```css
/* ✅ 크기 변경 대신 scale 사용 */
.element:hover {
  transform: scale(1.1);  /* GPU 가속 */
}

/* ❌ width 변경 */
.element:hover {
  width: 110%;  /* 레이아웃 리플로우 */
}

/* ✅ 위치 변경 대신 translate 사용 */
.element {
  transform: translateY(0);
}

.element.moved {
  transform: translateY(100px);  /* GPU 가속 */
}

/* ❌ top 변경 */
.element {
  top: 0;
}

.element.moved {
  top: 100px;  /* 레이아웃 리플로우 */
}
```

---

## 📱 반응형 디자인

### 모바일 (480px 이하)
- 간격 자동 조정

### 태블릿 (768px 이하)
- 헤더 높이: 56px
- 사이드바 너비: 100%
- 폰트 크기 자동 조정

---

## 🎯 개발 가이드

### 1. 하드코딩 금지
색상, 간격, 폰트 크기 등은 **항상 CSS 변수를 사용**하세요.

### 2. 일관성 유지
디자인 토큰을 사용하여 전체 앱의 일관성을 유지하세요.

### 3. 접근성
텍스트와 배경의 대비를 충분히 확보하세요.

### 4. 반응형
모바일, 태블릿, 데스크톱 모두 고려하여 디자인하세요.

---

## 📦 다음 단계

1. **프로젝트 초기화**
   ```bash
   npm init -y
   ```

2. **Vite + React + TypeScript 설정**
   ```bash
   npm install vite @vitejs/plugin-react react react-dom
   npm install -D typescript @types/react @types/react-dom
   ```

3. **필수 라이브러리 설치**
   - React Router
   - Zustand
   - Axios
   - React Query
