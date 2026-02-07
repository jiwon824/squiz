import React, { useState, useMemo, useCallback } from 'react';
import { ChevronLeft, ChevronRight, CheckCircle2, XCircle, Clock, RotateCcw } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { ReviewItemListProps } from './types';
import { getDifficultyColor, getDifficultyLabel } from './utils';

const ITEMS_PER_PAGE = 5;

/**
 * 복습/오답 문제 리스트를 표시하는 컴포넌트
 *
 * 특징:
 * - 페이지네이션 지원 (Client-side & Server-side)
 * - 난이도별 색상 표시
 * - 다시 풀기 버튼
 */
export const ReviewItemList: React.FC<ReviewItemListProps> = React.memo(
  ({ items, onRetry, type, currentPage: serverPage, totalPages: serverTotalPages, onPageChange }) => {
    // Client-side pagination state (fallback)
    const [clientPage, setClientPage] = useState(1);
    const isServerSide = serverPage !== undefined && serverTotalPages !== undefined && onPageChange !== undefined;

    // === 상태 결정 ===
    const currentPage = isServerSide ? serverPage : clientPage;
    const totalPages = isServerSide
      ? serverTotalPages
      : Math.ceil(items.length / ITEMS_PER_PAGE);

    // === 아이템 결정 ===
    // Server-side: items are already paginated
    // Client-side: slice items
    const currentItems = useMemo(
      () =>
        isServerSide
          ? items
          : items.slice(
            (clientPage - 1) * ITEMS_PER_PAGE,
            clientPage * ITEMS_PER_PAGE
          ),
      [items, clientPage, isServerSide]
    );

    const showPagination = isServerSide ? (totalPages || 0) > 1 : items.length > ITEMS_PER_PAGE;

    // === 페이지네이션 핸들러 ===
    const handlePrevPage = useCallback(() => {
      if (isServerSide && onPageChange) {
        onPageChange(Math.max(1, (currentPage || 1) - 1));
      } else {
        setClientPage((p) => Math.max(1, p - 1));
      }
    }, [isServerSide, onPageChange, currentPage]);

    const handleNextPage = useCallback(() => {
      if (isServerSide && onPageChange && totalPages) {
        onPageChange(Math.min(totalPages, (currentPage || 1) + 1));
      } else {
        setClientPage((p) => Math.min(totalPages || 1, p + 1));
      }
    }, [isServerSide, onPageChange, currentPage, totalPages]);

    const handlePageClick = useCallback((page: number) => {
      if (isServerSide && onPageChange) {
        onPageChange(page);
      } else {
        setClientPage(page);
      }
    }, [isServerSide, onPageChange]);

    // === 빈 상태 표시 ===
    if (items.length === 0 && !isServerSide) { // Server-side might yield empty items on load or error, handle gracefully?
      // For now keeping original behavior.
      return (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
            <CheckCircle2 size={32} className="text-gray-400" />
          </div>
          <h3 className="text-lg font-bold text-text-primary mb-1">
            {type === 'review' ? '오늘의 복습 끝!' : '틀린 문제가 없습니다'}
          </h3>
          <p className="text-text-tertiary">
            {type === 'review'
              ? '오늘은 더 이상 복습할 내용이 없습니다. 훌륭해요!'
              : '완벽하게 이해하고 계시네요.'}
          </p>
        </div>
      );
    }

    // Empty state for server side (when page 1 explains no results)
    if (isServerSide && totalPages === 0) {
      return (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
            <CheckCircle2 size={32} className="text-gray-400" />
          </div>
          <h3 className="text-lg font-bold text-text-primary mb-1">
            {type === 'review' ? '오늘의 복습 끝!' : '틀린 문제가 없습니다'}
          </h3>
          <p className="text-text-tertiary">
            {type === 'review'
              ? '오늘은 더 이상 복습할 내용이 없습니다. 훌륭해요!'
              : '완벽하게 이해하고 계시네요.'}
          </p>
        </div>
      );
    }

    return (
      <div>
        {/* 리스트 아이템 */}
        <div className="space-y-4">
          {currentItems.map((item) => (
            <ReviewItem
              key={item.reviewItemId}
              item={item}
              type={type}
              onRetry={onRetry}
            />
          ))}
        </div>

        {/* 페이지네이션 */}
        {showPagination && totalPages && currentPage && (
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPrevPage={handlePrevPage}
            onNextPage={handleNextPage}
            onPageClick={handlePageClick}
          />
        )}
      </div>
    );
  }
);

ReviewItemList.displayName = 'ReviewItemList';

// === 개별 리뷰 아이템 컴포넌트 ===
interface ReviewItemComponentProps {
  item: ReviewItemListProps['items'][0];
  type: ReviewItemListProps['type'];
  onRetry: ReviewItemListProps['onRetry'];
}

const ReviewItem: React.FC<ReviewItemComponentProps> = React.memo(
  ({ item, type, onRetry }) => {
    const handleRetryClick = useCallback(() => {
      onRetry(item);
    }, [item, onRetry]);

    return (
      <div className="px-5 py-4 rounded-xl bg-white shadow-[0_4px_15px_rgba(0,0,0,0.05)] hover:shadow-[0_8px_25px_rgba(0,0,0,0.1)] hover:bg-gray-50 transition-all">
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1 min-w-0">
            {/* 메타 정보 */}
            <div className="flex items-center gap-2 mb-3">
              <span
                className={cn(
                  'px-2.5 py-1 rounded-full text-xs font-medium',
                  getDifficultyColor(item.difficulty)
                )}
              >
                {getDifficultyLabel(item.difficulty)}
              </span>
              <span className="px-2.5 py-1 bg-gray-50 rounded-full text-xs font-medium text-text-secondary">
                {item.question?.category || '일반'}
              </span>
              {type === 'wrong' && (
                <span className="px-2.5 py-1 bg-error/5 rounded-full text-xs font-medium text-error flex items-center gap-1">
                  <XCircle size={12} />
                  {item.lapses}회 오답
                </span>
              )}
              {type === 'review' && (
                <span className="px-2.5 py-1 bg-primary/5 rounded-full text-xs font-medium text-primary flex items-center gap-1">
                  <Clock size={12} />
                  오늘 복습
                </span>
              )}
            </div>

            {/* 문제 */}
            <p className="text-text-primary font-medium line-clamp-2 leading-relaxed">
              {item.question?.questionText || '문제를 불러올 수 없습니다'}
            </p>

            {/* 마지막 날짜 */}
            <div className="flex items-center gap-1 mt-3 text-xs text-text-tertiary">
              <Clock size={12} />
              <span>
                {type === 'review' ? '복습 예정일: ' : '마지막 오답: '}
                {new Date(item.nextReviewAt).toLocaleDateString()}
              </span>
            </div>
          </div>

          {/* 다시 풀기 버튼 */}
          <button
            onClick={handleRetryClick}
            className="flex-shrink-0 flex items-center gap-2 px-4 py-2.5 rounded-lg bg-primary/5 text-primary hover:bg-primary/10 transition-colors text-sm font-medium"
          >
            <RotateCcw size={14} />
            <span>{type === 'review' ? '복습하기' : '다시 풀기'}</span>
          </button>
        </div>
      </div>
    );
  }
);

ReviewItem.displayName = 'ReviewItem';

// === 페이지네이션 컴포넌트 ===
interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPrevPage: () => void;
  onNextPage: () => void;
  onPageClick: (page: number) => void;
}

const Pagination: React.FC<PaginationProps> = React.memo(
  ({ currentPage, totalPages, onPrevPage, onNextPage, onPageClick }) => {
    const pages = useMemo(
      () => Array.from({ length: totalPages }, (_, i) => i + 1),
      [totalPages]
    );

    return (
      <nav className="flex items-center justify-center gap-2 mt-6 pt-5 border-t border-gray-50">
        <button
          type="button"
          onClick={onPrevPage}
          disabled={currentPage === 1}
          className={cn(
            'flex items-center gap-1 px-2 py-1.5 rounded-lg text-sm transition-colors',
            currentPage === 1
              ? 'text-gray-300 cursor-not-allowed'
              : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
          )}
        >
          <ChevronLeft size={18} />
          <span className="hidden sm:inline">이전</span>
        </button>

        <div className="flex items-center gap-1">
          {pages.map((page) => (
            <button
              key={page}
              type="button"
              onClick={() => onPageClick(page)}
              className={cn(
                'w-8 h-8 rounded-lg text-sm font-medium transition-colors',
                page === currentPage
                  ? 'text-[var(--color-primary)] font-bold'
                  : 'text-[var(--color-text-tertiary)] hover:text-[var(--color-text-primary)]'
              )}
            >
              {page}
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={onNextPage}
          disabled={currentPage === totalPages}
          className={cn(
            'flex items-center gap-1 px-2 py-1.5 rounded-lg text-sm transition-colors',
            currentPage === totalPages
              ? 'text-gray-300 cursor-not-allowed'
              : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]'
          )}
        >
          <span className="hidden sm:inline">다음</span>
          <ChevronRight size={18} />
        </button>
      </nav>
    );
  }
);

Pagination.displayName = 'Pagination';

export default ReviewItemList;
