# 📦 Study 시맨틱 스타일 상수 목록 (Semantic Style Constants)

> Tailwind 클래스를 의미 있는 이름으로 분리하여 코드 가독성을 향상시킵니다.  
> 작성일: 2026년 1월 25일

---

## 📁 적용 파일 목록

| 파일명 | 적용 상태 | 설명 |
|--------|----------|------|
| `StudyDetailPage.tsx` | ✅ 적용 완료 | 스터디 상세 페이지 |
| `StudyCreatePage.tsx` | 🔲 미적용 | 스터디 생성 페이지 |
| `StudyCardContent.tsx` | 🔲 미적용 | 스터디 카드 컴포넌트 |
| `StudyListContainer.tsx` | 🔲 미적용 | 스터디 목록 컨테이너 |
| `StudyFilter.tsx` | 🔲 미적용 | 스터디 필터 컴포넌트 |
| `StudyPage.tsx` | 🔲 미적용 | 스터디 메인 페이지 |
| `StudyApplyModal.tsx` | 🔲 미적용 | 스터디 신청 모달 |
| `StudyReportModal.tsx` | 🔲 미적용 | 스터디 신고 모달 |
| `LeaderReviewModal.tsx` | 🔲 미적용 | 리더 리뷰 모달 |

---

## 🎨 StudyDetailPage.tsx 스타일 상수

### 📌 페이지 레이아웃 (Page Layout)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `pageContainer` | `max-w-[1200px] mx-auto py-12 animate-fadeIn` | 메인 페이지 컨테이너 |

---

### 🔝 상단 컨트롤 바 (Control Bar)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `controlBar` | `flex justify-between items-center mb-8` | 상단 컨트롤 영역 |
| `backButtonGroup` | `flex items-center gap-4` | 뒤로가기 버튼 그룹 |
| `backLabel` | `text-sm font-bold text-text-secondary tracking-tight` | "스터디 목록" 라벨 |
| `reportButton` | `text-text-muted hover:text-error hover:bg-error/5 flex items-center gap-2 px-4 h-10` | 신고하기 버튼 |

---

### 🖼️ 히어로 섹션 (Hero Section)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `heroSection` | `bg-white border border-border-light rounded-[40px] p-12 mb-10 relative overflow-hidden shadow-[0_20px_50px_rgba(0,0,0,0.04)]` | 히어로 카드 컨테이너 |
| `heroBlur` | `absolute top-0 right-0 w-80 h-80 bg-primary/5 rounded-full -mr-40 -mt-40 blur-[80px] pointer-events-none` | 배경 블러 효과 |
| `heroContent` | `relative z-10` | 히어로 콘텐츠 래퍼 |

---

### 🏷️ 뱃지 영역 (Badge Area)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `badgeRow` | `flex flex-wrap items-center gap-2 mb-6` | 뱃지 행 컨테이너 |
| `badgeDot` | `text-text-tertiary/50` | 뱃지 구분 점 |
| `badgeText` | `text-[12px] font-semibold text-text-secondary` | 일반 뱃지 텍스트 |
| `badgeTopic` | `text-[12px] font-bold text-primary` | 주제 뱃지 (강조) |

---

### 📝 타이틀 영역 (Title Area)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `titleArea` | `flex flex-col md:flex-row md:items-start justify-between gap-10` | 타이틀 영역 컨테이너 |
| `titleWrapper` | `max-w-2xl` | 타이틀 텍스트 래퍼 |
| `title` | `text-3xl md:text-4xl lg:text-5xl font-black text-text-primary mb-6 leading-tight tracking-tight` | 메인 타이틀 |
| `description` | `text-base md:text-lg text-text-secondary leading-relaxed font-medium opacity-70` | 설명 텍스트 |
| `actionButtons` | `flex items-center gap-2` | 액션 버튼 그룹 |
| `iconButton` | `w-12 h-12 transition-all duration-300 hover:scale-110` | 아이콘 버튼 (북마크, 공유) |

---

### 📐 메인 콘텐츠 그리드 (Content Grid)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `contentGrid` | `grid grid-cols-1 lg:grid-cols-12 gap-10 items-start` | 12컬럼 그리드 |
| `mainColumn` | `lg:col-span-8 space-y-10` | 메인 콘텐츠 영역 (8컬럼) |
| `sideColumn` | `lg:col-span-4 space-y-8` | 사이드바 영역 (4컬럼) |

---

### 📦 섹션 카드 (Section Card)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `sectionCard` | `bg-white border border-border-light rounded-[32px] p-10 shadow-sm` | 섹션 카드 컨테이너 |
| `sectionTitle` | `text-lg font-bold text-text-primary mb-8 flex items-center gap-3` | 섹션 타이틀 |
| `sectionIcon` | `p-2 bg-primary/10 text-primary rounded-xl` | 섹션 아이콘 배경 |
| `infoGrid` | `grid grid-cols-1 sm:grid-cols-2 gap-10` | 정보 2열 그리드 |

---

### 📚 커리큘럼 (Curriculum)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `curriculumItem` | `flex gap-4 p-4 rounded-2xl bg-surface-50 hover:bg-surface-100 transition-colors border border-border-light/50` | 커리큘럼 아이템 |
| `weekBadge` | `flex-shrink-0 w-16 flex flex-col items-center justify-center bg-white rounded-xl border border-primary/20 shadow-sm h-16` | 주차 뱃지 |
| `weekLabel` | `text-[10px] font-bold text-text-tertiary uppercase tracking-wider` | "WEEK" 라벨 |
| `weekNumber` | `text-xl font-black text-primary` | 주차 숫자 |
| `curriculumText` | `flex-grow flex items-center` | 커리큘럼 설명 텍스트 래퍼 |

---

### 👤 리더 카드 (Leader Card)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `leaderCard` | `bg-white border border-border-light rounded-[40px] p-10 shadow-[0_10px_40px_rgba(0,0,0,0.03)] flex flex-col items-center sticky top-10` | 리더 카드 컨테이너 (스티키) |

---

### 🖼️ 리더 아바타 (Leader Avatar)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `avatarWrapper` | `relative mb-6` | 아바타 래퍼 |
| `avatar` | `w-24 h-24 rounded-[32px] overflow-hidden border-4 border-white shadow-xl relative group` | 아바타 이미지 컨테이너 |
| `avatarImage` | `w-full h-full object-cover transition-transform duration-500 group-hover:scale-110` | 아바타 이미지 |
| `avatarFallback` | `w-full h-full bg-primary text-white flex items-center justify-center text-3xl font-black` | 아바타 폴백 (이니셜) |
| `onlineIndicator` | `absolute -bottom-2 -right-2 w-8 h-8 bg-success border-4 border-white rounded-full shadow-lg` | 온라인 상태 표시 |

---

### ⭐ 리더 정보 & 평점 (Leader Info & Rating)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `leaderInfo` | `text-center mb-10 w-full` | 리더 정보 영역 |
| `leaderName` | `text-2xl font-black text-text-primary tracking-tight mb-2 truncate` | 리더 닉네임 |
| `ratingWrapper` | `flex flex-col items-center gap-2` | 평점 래퍼 |
| `ratingButton` | `flex items-center gap-2 px-4 py-1.5 bg-background-secondary/50 rounded-full border border-border-light/30 hover:bg-primary/10 hover:border-primary/30 transition-all cursor-pointer group` | 평점 클릭 버튼 |
| `ratingStar` | `text-yellow-400 fill-current group-hover:scale-110 transition-transform` | 별 아이콘 |
| `ratingValue` | `text-sm font-black text-text-primary` | 평점 숫자 |
| `ratingCount` | `text-[10px] font-bold text-text-tertiary opacity-60 group-hover:text-primary transition-colors` | 리뷰 개수 |

---

### 🔘 버튼 그룹 (Button Group)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `buttonGroup` | `w-full space-y-3` | 버튼 그룹 컨테이너 |
| `inquiryButton` | `h-14 rounded-2xl border-border-light/80 text-text-secondary font-black hover:bg-background-secondary transition-all active:scale-95 shadow-sm` | 문의하기 버튼 |
| `primaryButton` | `h-14 rounded-2xl font-black text-base shadow-xl shadow-primary/20 active:scale-95 transition-all text-white border-none` | 주요 액션 버튼 (신청하기) |

---

### ✅ 인증 배지 (Verified Badge)

| 상수명 | Tailwind 클래스 | 설명 |
|--------|----------------|------|
| `verifiedBadge` | `mt-6 text-[11px] font-bold text-text-tertiary flex items-center gap-1.5 opacity-60` | 인증 배지 컨테이너 |
| `verifiedIcon` | `text-primary/60` | 인증 아이콘 |

---

## 🛠️ 유틸리티 함수

### `getBadgeStyle(status: string)`
상태에 따른 동적 뱃지 스타일 반환

```tsx
const getBadgeStyle = (status: string) => {
    const baseStyle = "px-3 py-1.5 rounded-full text-xs font-black";
    switch (status) {
        case '모집중':
            return cn(baseStyle, "bg-primary/15 text-primary");
        case '진행중':
            return cn(baseStyle, "bg-success/15 text-success");
        case '완료':
            return cn(baseStyle, "bg-text-muted/15 text-text-muted");
        default:
            return cn(baseStyle, "bg-text-muted/10 text-text-muted");
    }
};
```

### `getDifficultyColor(difficulty: string)`
난이도에 따른 동적 색상 반환

```tsx
const getDifficultyColor = (difficulty: string) => {
    switch (difficulty) {
        case '초급': return 'bg-success/15 text-success';
        case '중급': return 'bg-warning/15 text-warning';
        case '고급': return 'bg-error/15 text-error';
        default: return 'bg-text-muted/10 text-text-muted';
    }
};
```

---

## 📖 사용 예시

```tsx
// styles 객체 정의
const styles = {
    pageContainer: "max-w-[1200px] mx-auto py-12 animate-fadeIn",
    heroSection: cn(
        "bg-white border border-border-light rounded-[40px] p-12 mb-10",
        "relative overflow-hidden shadow-[0_20px_50px_rgba(0,0,0,0.04)]"
    ),
    // ... 더 많은 스타일
};

// JSX에서 사용
return (
    <div className={styles.pageContainer}>
        <div className={styles.heroSection}>
            <h1 className={styles.title}>{study.title}</h1>
        </div>
    </div>
);
```

---

## 💡 시맨틱 스타일 상수 장점

1. **가독성 향상**: 긴 Tailwind 클래스 대신 의미 있는 이름 사용
2. **재사용성**: 동일 스타일을 여러 곳에서 사용 가능
3. **유지보수 용이**: 스타일 변경 시 한 곳만 수정
4. **자동완성**: IDE에서 `styles.` 입력 시 자동완성 지원
5. **타입 안정성**: TypeScript와 함께 사용 시 오타 방지

---

## 🔗 관련 유틸리티

- `cn()` 함수: `@/shared/utils/cn.ts` - clsx + tailwind-merge 조합
- Tailwind 설정: `tailwind.config.js` - 커스텀 컬러, 간격 등 정의
- 글로벌 CSS: `index.css` - CSS 변수 및 기본 스타일

---

*마지막 업데이트: 2026-01-25*
