/**
 * 🛠️ ClassName Utility 
 * Tailwind + clsx 기반 클래스명 조합 유틸리티
 * 
 * Usage:
 * import { cn } from '@/shared/utils/cn';
 * 
 * className={cn(
 *   'base-class',
 *   {
 *     'conditional-class': condition,
 *     'another-class': anotherCondition,
 *   },
 *   additionalClassName
 * )}
 */

import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * 클래스명을 조합하고 Tailwind 충돌을 해결하는 유틸리티
 * 
 * @param inputs - 클래스명 또는 조건부 클래스명 객체
 * @returns 조합된 클래스명 문자열
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * 🎨 디자인 토큰 기반 클래스 빌더
 * 프로젝트 실제 패턴을 기반으로 정의된 스타일 프리셋
 */
export const classBuilder = {
  /**
   * 버튼 클래스 빌더 — shared Button 컴포넌트를 쓰지 않는 raw <button>용
   */
  button: (variant: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger', size: 'sm' | 'md' | 'lg') => {
    const baseClasses = 'inline-flex items-center justify-center font-semibold transition-all duration-200 active:scale-[0.98] disabled:opacity-50';

    const variants = {
      primary: 'bg-primary hover:bg-primary-dark text-white shadow-sm',
      secondary: 'bg-background-secondary hover:bg-gray-200 text-text-secondary',
      outline: 'border border-primary text-primary hover:bg-primary/5',
      ghost: 'text-text-secondary hover:bg-gray-100',
      danger: 'bg-error hover:bg-error/90 text-white',
    };

    const sizes = {
      sm: 'px-3 py-1.5 text-xs rounded-lg gap-1.5',
      md: 'px-6 py-2.5 text-sm rounded-lg gap-2',
      lg: 'px-8 py-3.5 text-base rounded-xl gap-2.5',
    };

    return cn(baseClasses, variants[variant], sizes[size]);
  },

  /**
   * 카드 클래스 빌더
   */
  card: (variant: 'default' | 'outlined' | 'elevated' | 'modal' = 'default') => {
    const baseClasses = 'bg-white rounded-2xl';

    const variants = {
      default: 'border border-gray-100 shadow-sm',
      outlined: 'border border-gray-200',
      elevated: 'shadow-[0_4px_15px_rgba(0,0,0,0.05)] transition-shadow hover:shadow-[0_8px_25px_rgba(0,0,0,0.1)]',
      modal: 'shadow-xl',
    };

    return cn(baseClasses, variants[variant]);
  },

  /**
   * 플로팅 인풋 클래스 빌더
   */
  floatingInput: (size: 'xs' | 'sm' | 'md' | 'lg' | 'xl' = 'md') => {
    const baseClasses = 'relative';

    const sizes = {
      xs: 'h-9',
      sm: 'h-10',
      md: 'h-12',
      lg: 'h-14',
      xl: 'h-16',
    };

    return cn(baseClasses, sizes[size]);
  },

  /**
   * 텍스트 클래스 빌더
   */
  text: (variant: 'heading' | 'body' | 'caption', size?: 'sm' | 'md' | 'lg') => {
    const variants = {
      heading: cn(
        'font-semibold text-text-primary',
        {
          'text-lg': size === 'sm',
          'text-xl': size === 'md' || !size,
          'text-2xl': size === 'lg',
        }
      ),
      body: cn(
        'text-text-secondary',
        {
          'text-sm': size === 'sm',
          'text-base': size === 'md' || !size,
          'text-lg': size === 'lg',
        }
      ),
      caption: cn(
        'text-text-tertiary',
        {
          'text-xs': size === 'sm' || !size,
          'text-sm': size === 'md',
          'text-base': size === 'lg',
        }
      ),
    };

    return variants[variant];
  },

  /**
   * 뱃지 클래스 빌더 — 상태/카테고리 표시용 라운드 라벨
   */
  badge: (variant: 'primary' | 'success' | 'warning' | 'error' | 'gray' = 'gray', size: 'xs' | 'sm' = 'sm') => {
    const baseClasses = 'rounded-full font-semibold';

    const variants = {
      primary: 'bg-[var(--color-primary-alpha-10)] text-primary',
      success: 'bg-green-50 text-green-700',
      warning: 'bg-[#FEF7E0] text-[#f9ab00]',
      error: 'bg-error/10 text-error',
      gray: 'bg-background-secondary text-text-secondary',
    };

    const sizes = {
      xs: 'px-2 py-0.5 text-[10px]',
      sm: 'px-3 py-1 text-xs',
    };

    return cn(baseClasses, variants[variant], sizes[size]);
  },

  /**
   * 툴팁 클래스 빌더 — group-hover로 표시되는 툴팁
   */
  tooltip: (position: 'top' | 'bottom' | 'left' | 'right' = 'bottom') => {
    const baseClasses = 'absolute px-2.5 py-1 rounded-lg bg-slate-800 text-white text-xs font-medium whitespace-nowrap opacity-0 group-hover:opacity-100 transition-all duration-200 pointer-events-none shadow-lg z-50';

    const positions = {
      top: '-top-9 left-1/2 -translate-x-1/2',
      bottom: '-bottom-9 left-1/2 -translate-x-1/2',
      left: 'top-1/2 -translate-y-1/2 right-full mr-2',
      right: 'top-1/2 -translate-y-1/2 left-full ml-2',
    };

    return cn(baseClasses, positions[position]);
  },

  /**
   * 아이콘 버튼 클래스 빌더 — 작은 액션 버튼
   */
  iconButton: (variant: 'default' | 'primary' | 'success' | 'danger' | 'ghost' = 'default', size: 'xs' | 'sm' | 'md' = 'sm') => {
    const baseClasses = 'inline-flex items-center justify-center gap-1 font-medium rounded-lg transition-colors disabled:opacity-50';

    const variants = {
      default: 'bg-white border border-gray-200 text-gray-700 hover:bg-gray-50',
      primary: 'bg-study-blue text-white hover:bg-study-blue/90',
      success: 'bg-study-green text-white hover:bg-study-green/90',
      danger: 'bg-red-500 text-white hover:bg-red-600',
      ghost: 'text-gray-500 hover:bg-gray-100 hover:text-gray-700',
    };

    const sizes = {
      xs: 'px-2 py-1 text-[10px]',
      sm: 'px-3 py-1.5 text-xs',
      md: 'px-4 py-2 text-sm',
    };

    return cn(baseClasses, variants[variant], sizes[size]);
  },

  /**
   * 드롭다운/팝오버 컨테이너 클래스 빌더
   */
  dropdown: (size: 'sm' | 'md' | 'lg' = 'md') => {
    const baseClasses = 'absolute z-50 bg-white border border-gray-200 shadow-lg animate-in fade-in zoom-in-95 duration-200 origin-top overflow-hidden';

    const sizes = {
      sm: 'rounded-xl',
      md: 'rounded-2xl',
      lg: 'rounded-[32px]',
    };

    return cn(baseClasses, sizes[size]);
  },

  /**
   * 알림 배지 클래스 빌더 — 숫자 표시용 작은 원형 배지
   */
  notificationBadge: (variant: 'red' | 'blue' | 'green' = 'red') => {
    const baseClasses = 'absolute -top-1 -right-1 min-w-[18px] h-[18px] rounded-full flex items-center justify-center text-[10px] text-white font-bold px-1';

    const variants = {
      red: 'bg-red-500',
      blue: 'bg-study-blue',
      green: 'bg-study-green',
    };

    return cn(baseClasses, variants[variant]);
  },

  /**
   * 아바타 클래스 빌더 — 프로필 이미지/이니셜 표시
   */
  avatar: (size: 'xs' | 'sm' | 'md' | 'lg' = 'md', variant: 'default' | 'gradient' = 'default') => {
    const baseClasses = 'rounded-full flex items-center justify-center font-bold overflow-hidden';

    const variants = {
      default: 'bg-study-blue/10 text-study-blue',
      gradient: 'bg-gradient-to-br from-study-blue to-study-blue-dark text-white',
    };

    const sizes = {
      xs: 'w-6 h-6 text-[10px]',
      sm: 'w-7 h-7 text-xs',
      md: 'w-8 h-8 text-sm',
      lg: 'w-10 h-10 text-sm',
    };

    return cn(baseClasses, variants[variant], sizes[size]);
  },

  /**
   * 입력 필드 클래스 빌더 — 폼 입력 필드
   */
  input: (variant: 'default' | 'filled' | 'outlined' = 'default', size: 'sm' | 'md' | 'lg' = 'md') => {
    const baseClasses = 'w-full outline-none transition-all';

    const variants = {
      default: 'bg-white border border-gray-200 focus:border-primary/50 focus:ring-2 focus:ring-primary/10',
      filled: 'bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white',
      outlined: 'bg-transparent border-2 border-gray-200 focus:border-primary',
    };

    const sizes = {
      sm: 'px-3 py-2 text-sm rounded-lg',
      md: 'px-4 py-3 text-sm rounded-xl',
      lg: 'px-4 py-3.5 text-base rounded-2xl',
    };

    return cn(baseClasses, variants[variant], sizes[size]);
  },
};

/**
 * 🎯 조건부 클래스 헬퍼
 */
export const conditionalClasses = {
  /**
   * 상태에 따른 클래스
   */
  state: (isActive: boolean, activeClass: string, inactiveClass: string = '') => 
    cn({ [activeClass]: isActive, [inactiveClass]: !isActive && inactiveClass }),
    
  /**
   * 사이즈별 반응형 클래스
   */
  responsive: (classes: { sm?: string; md?: string; lg?: string; xl?: string }) => 
    cn(
      classes.sm && `sm:${classes.sm}`,
      classes.md && `md:${classes.md}`,
      classes.lg && `lg:${classes.lg}`,
      classes.xl && `xl:${classes.xl}`
    ),
};