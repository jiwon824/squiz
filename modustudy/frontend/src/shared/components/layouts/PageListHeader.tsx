// 목록 페이지 공통 헤더 (제목 + 부제 + 우측 액션 버튼)
import React from 'react';
import { cn } from '@/shared/utils/cn';

export interface PageListHeaderProps {
  /** 페이지 제목 */
  title: string;
  /** 제목에 추가할 클래스 (예: shimmer-text) */
  titleClassName?: string;
  /** 서브타이틀 - 문자열 또는 ReactNode (동적 카운트 등) */
  subtitle?: React.ReactNode;
  /** 우측 버튼/액션 영역 */
  actions?: React.ReactNode;
  /** 루트 div 추가 클래스 */
  className?: string;
}

export const PageListHeader: React.FC<PageListHeaderProps> = ({
  title,
  titleClassName,
  subtitle,
  actions,
  className,
}) => {
  return (
    <div className={cn('flex justify-between mb-2', className)}>
      <div className="flex items-center pt-2">
        <div>
          <h1
            className={cn(
              'text-2xl md:text-3xl font-bold text-[var(--color-text-primary)]',
              titleClassName
            )}
          >
            {title}
          </h1>
          {subtitle && (
            <p className="text-sm text-[var(--color-text-secondary)] mt-1">
              {subtitle}
            </p>
          )}
        </div>
      </div>
      {actions && (
        <div className="flex items-center gap-3">
          {actions}
        </div>
      )}
    </div>
  );
};
