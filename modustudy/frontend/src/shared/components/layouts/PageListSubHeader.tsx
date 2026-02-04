// 목록 페이지 공통 서브헤더 (검색바 + 필터 컨트롤)
import React from 'react';
import { Search, X } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

export interface PageListSubHeaderProps {
  /** 검색 입력값 */
  searchValue: string;
  /** 검색 입력 변경 핸들러 */
  onSearchChange: (value: string) => void;
  /** 검색 placeholder */
  searchPlaceholder?: string;
  /** form submit 핸들러 (있으면 form 태그, 없으면 div) */
  onSearchSubmit?: (e: React.FormEvent<HTMLFormElement>) => void;
  /** 검색바 우측 필터 컨트롤 영역 */
  filterControls?: React.ReactNode;
  /** 확장 필터 영역 (검색바 하단) */
  expandedFilter?: React.ReactNode;
  /** 루트 div 추가 클래스 */
  className?: string;
}

export const PageListSubHeader: React.FC<PageListSubHeaderProps> = ({
  searchValue,
  onSearchChange,
  searchPlaceholder = '검색...',
  onSearchSubmit,
  filterControls,
  expandedFilter,
  className,
}) => {
  // 검색 인풋
  const searchInput = (
    <div className="relative">
      <Search
        size={18}
        className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-text-tertiary)]"
      />
      <input
        type="text"
        placeholder={searchPlaceholder}
        value={searchValue}
        onChange={(e) => onSearchChange(e.target.value)}
        className="w-full h-11 pl-11 pr-10 bg-[var(--color-background)] rounded-xl text-sm focus:outline-none ring-0 focus:ring-2 ring-[var(--color-primary-alpha-10)] transition-all duration-300 ease-in-out"
      />
      {searchValue && (
        <button
          type="button"
          onClick={() => onSearchChange('')}
          className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full hover:bg-[var(--color-background-secondary)] text-[var(--color-text-tertiary)]"
        >
          <X size={16} />
        </button>
      )}
    </div>
  );

  // form 래퍼 또는 div 래퍼
  const searchBar = onSearchSubmit ? (
    <form onSubmit={onSearchSubmit} className="flex-1">
      {searchInput}
    </form>
  ) : (
    <div className="flex-1">
      {searchInput}
    </div>
  );

  return (
    <div className={cn('mb-6', className)}>
      <div className="flex flex-col lg:flex-row gap-4">
        {searchBar}
        {filterControls && (
          <div className="flex items-center gap-3">
            {filterControls}
          </div>
        )}
      </div>
      {expandedFilter}
    </div>
  );
};
