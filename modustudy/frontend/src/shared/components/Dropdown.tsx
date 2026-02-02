import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

export interface DropdownItem {
  label: string;
  value: string;
  icon?: React.ReactNode;
  disabled?: boolean;
  danger?: boolean;
  onClick?: () => void;
}

interface DropdownProps {
  /** 드롭다운 트리거 버튼 텍스트 (trigger 미사용 시 필수) */
  label?: string;
  /** 커스텀 트리거 렌더 함수 (label 대신 사용) */
  trigger?: (props: { isOpen: boolean; toggle: () => void }) => React.ReactNode;
  /** 메뉴 항목 리스트 */
  items: DropdownItem[];
  /** 항목 클릭 시 콜백 */
  onSelect?: (value: string) => void;
  /** 버튼 스타일 변형 */
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost';
  /** 버튼 크기 */
  size?: 'sm' | 'md' | 'lg';
  /** 드롭다운 메뉴 정렬 방향 */
  align?: 'left' | 'right';
  /** 비활성화 여부 */
  disabled?: boolean;
  /** 추가 클래스 */
  className?: string;
  /** 트리거 버튼 커스텀 클래스 */
  buttonClassName?: string;
  /** 메뉴 커스텀 클래스 */
  menuClassName?: string;
}

export const Dropdown: React.FC<DropdownProps> = ({
  label,
  trigger,
  items,
  onSelect,
  variant = 'outline',
  size = 'md',
  align = 'left',
  disabled = false,
  className,
  buttonClassName,
  menuClassName,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  // ESC 키로 닫기
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  const handleItemClick = (item: DropdownItem) => {
    if (item.disabled) return;
    item.onClick?.();
    onSelect?.(item.value);
    setIsOpen(false);
  };

  const sizeStyles = {
    sm: 'px-3 py-1.5 text-xs gap-1.5',
    md: 'px-4 py-2 text-sm gap-2',
    lg: 'px-5 py-2.5 text-base gap-2.5',
  };

  const variantStyles = {
    primary: 'bg-primary hover:bg-primary/90 text-white',
    secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-700',
    outline: 'border border-gray-200 bg-white hover:bg-gray-50 text-gray-700',
    ghost: 'text-gray-700 hover:bg-gray-100',
  };

  const toggle = () => !disabled && setIsOpen(!isOpen);

  return (
    <div className={cn('relative inline-flex', className)} ref={containerRef}>
      {/* 트리거 영역 */}
      {trigger ? (
        trigger({ isOpen, toggle })
      ) : (
        <button
          type="button"
          onClick={toggle}
          disabled={disabled}
          aria-haspopup="menu"
          aria-expanded={isOpen}
          className={cn(
            'inline-flex items-center justify-center rounded-lg font-medium transition-all duration-200',
            'focus:outline-none focus:ring-2 focus:ring-primary/20',
            sizeStyles[size],
            variantStyles[variant],
            disabled && 'opacity-50 cursor-not-allowed',
            buttonClassName
          )}
        >
          <span>{label}</span>
          <ChevronDown
            size={size === 'sm' ? 14 : 16}
            className={cn(
              'text-current transition-transform duration-200',
              isOpen && 'rotate-180'
            )}
          />
        </button>
      )}

      {/* 드롭다운 메뉴 */}
      {isOpen && (
        <ul
          role="menu"
          aria-orientation="vertical"
          className={cn(
            'absolute z-50 mt-1.5 top-full min-w-[200px]',
            'bg-white border border-gray-100 rounded-xl shadow-lg',
            'animate-in fade-in zoom-in-95 duration-150',
            'overflow-hidden',
            align === 'right' ? 'right-0' : 'left-0',
            menuClassName
          )}
        >
          <div className="p-1">
            {items.map((item) => (
              <li key={item.value} role="none">
                <button
                  type="button"
                  role="menuitem"
                  disabled={item.disabled}
                  onClick={() => handleItemClick(item)}
                  className={cn(
                    'w-full flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm text-left transition-colors',
                    item.danger
                      ? 'text-red-600 hover:bg-red-50'
                      : 'text-gray-700 hover:bg-gray-50',
                    item.disabled && 'opacity-40 cursor-not-allowed hover:bg-transparent'
                  )}
                >
                  {item.icon && <span className="flex-shrink-0">{item.icon}</span>}
                  <span>{item.label}</span>
                </button>
              </li>
            ))}
          </div>
        </ul>
      )}
    </div>
  );
};
