# 🔔 Toast 알림 시스템 가이드

> 전역 Toast 알림 시스템 사용법 및 API 문서  
> 작성일: 2026년 1월 25일

---

## 📁 관련 파일

| 파일 경로 | 설명 |
|----------|------|
| `src/shared/components/Toast.tsx` | Toast 컴포넌트 |
| `src/store/uiStore.ts` | Toast 상태 관리 (Zustand) |
| `src/assets/styles/index.css` | slideUp 애니메이션 정의 |
| `src/App.tsx` | ToastContainer 전역 렌더링 |

---

## 🚀 빠른 사용법

```tsx
import { useUIStore } from '@/store/uiStore';

const MyComponent = () => {
    const { showToast } = useUIStore();

    const handleClick = () => {
        showToast('작업이 완료되었습니다!', 'success');
    };

    return <button onClick={handleClick}>클릭</button>;
};
```

---

## 📚 API

### `showToast(message, type?)`

Toast 알림을 표시합니다.

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `message` | `string` | (필수) | 표시할 메시지 |
| `type` | `'success' \| 'error' \| 'warning' \| 'info'` | `'success'` | Toast 타입 |

```tsx
// 성공 (기본값)
showToast('저장되었습니다!');
showToast('저장되었습니다!', 'success');

// 에러
showToast('오류가 발생했습니다.', 'error');

// 경고
showToast('주의가 필요합니다.', 'warning');

// 정보
showToast('새로운 알림이 있습니다.', 'info');
```

### `removeToast(id)`

특정 Toast를 즉시 제거합니다. (보통 자동 제거되므로 수동 호출 불필요)

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `id` | `string` | Toast ID |

---

## 🎨 Toast 타입별 스타일

| 타입 | 배경색 | 아이콘 | 용도 |
|------|--------|--------|------|
| `success` | 초록 (`success/10`) | ✓ CheckCircle | 성공 메시지 |
| `error` | 빨강 (`error/10`) | ⊗ AlertCircle | 에러/실패 메시지 |
| `warning` | 노랑 (`warning/10`) | ⚠ AlertTriangle | 경고 메시지 |
| `info` | 파랑 (`primary/10`) | ℹ Info | 정보 알림 |

---

## ⚙️ 동작 방식

1. **표시 위치**: 화면 우측 하단 (fixed, z-index: 9999)
2. **자동 제거**: 3초 후 자동으로 사라짐
3. **애니메이션**: 아래에서 위로 슬라이드 (`slideUp`)
4. **다중 표시**: 여러 Toast가 동시에 표시 가능 (세로로 쌓임)
5. **수동 닫기**: X 버튼 클릭으로 즉시 닫기 가능

---

## 🧩 컴포넌트 구조

### Toast Interface

```typescript
interface Toast {
    id: string;        // 고유 ID (Date.now() 기반)
    message: string;   // 표시 메시지
    type: 'success' | 'error' | 'info' | 'warning';
}
```

### UIStore Toast 관련 상태

```typescript
interface UIState {
    // ... 기존 상태
    toasts: Toast[];
    showToast: (message: string, type?: Toast['type']) => void;
    removeToast: (id: string) => void;
}
```

---

## 📍 사용 예시

### 1. 링크 복사

```tsx
const handleShare = () => {
    navigator.clipboard.writeText(window.location.href);
    showToast('링크가 클립보드에 복사되었습니다!', 'success');
};
```

### 2. 폼 제출 결과

```tsx
const handleSubmit = async () => {
    try {
        await submitForm(data);
        showToast('제출이 완료되었습니다!', 'success');
    } catch (error) {
        showToast('제출에 실패했습니다. 다시 시도해주세요.', 'error');
    }
};
```

### 3. 삭제 확인

```tsx
const handleDelete = async () => {
    await deleteItem(id);
    showToast('삭제되었습니다.', 'info');
};
```

### 4. 유효성 검사 경고

```tsx
const handleValidation = () => {
    if (!isValid) {
        showToast('입력값을 확인해주세요.', 'warning');
        return;
    }
    // 계속 진행...
};
```

---

## 🎯 스타일 상수

```tsx
// Toast.tsx 내부 스타일 정의
const toastStyles = {
    success: {
        container: 'bg-success/10 border-success/30 text-success',
        icon: CheckCircle,
    },
    error: {
        container: 'bg-error/10 border-error/30 text-error',
        icon: AlertCircle,
    },
    warning: {
        container: 'bg-warning/10 border-warning/30 text-warning',
        icon: AlertTriangle,
    },
    info: {
        container: 'bg-primary/10 border-primary/30 text-primary',
        icon: Info,
    },
};
```

---

## 🔧 커스터마이징

### 자동 제거 시간 변경

`uiStore.ts`에서 setTimeout 값 수정:

```typescript
// 현재: 3초 (3000ms)
setTimeout(() => {
    set((state) => ({
        toasts: state.toasts.filter((t) => t.id !== id),
    }));
}, 3000);  // ← 이 값을 변경
```

### 위치 변경

`Toast.tsx`에서 위치 클래스 수정:

```tsx
// 현재: 우측 하단
<div className="fixed bottom-6 right-6 z-[9999] ...">

// 좌측 하단으로 변경
<div className="fixed bottom-6 left-6 z-[9999] ...">

// 상단 중앙으로 변경
<div className="fixed top-6 left-1/2 -translate-x-1/2 z-[9999] ...">
```

---

## 📝 CSS 애니메이션

```css
/* index.css에 정의됨 */
@keyframes slideUp {
    from {
        opacity: 0;
        transform: translateY(20px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.animate-slideUp {
    animation: slideUp 0.3s ease-out;
}
```

---

## ✅ 체크리스트

- [x] `App.tsx`에 `<ToastContainer />` 추가됨
- [x] `uiStore.ts`에 toast 상태 관리 추가됨
- [x] `Toast.tsx` 컴포넌트 생성됨
- [x] `index.css`에 slideUp 애니메이션 추가됨
- [x] `shared/components/index.ts`에 export 추가됨

---

*마지막 업데이트: 2026-01-25*
