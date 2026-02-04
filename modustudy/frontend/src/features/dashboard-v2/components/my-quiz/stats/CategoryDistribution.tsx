import React, { useMemo } from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/shared/utils/cn';
import { ReviewItemDto } from '../../../api/reviewApi';
import { calculateCategoryStats, getProgressBarColor } from '../utils';

interface CategoryDistributionProps {
  wrongReviews: ReviewItemDto[];
}

/**
 * 카테고리별 오답 분포를 표시하는 컴포넌트
 */
export const CategoryDistribution: React.FC<CategoryDistributionProps> = React.memo(
  ({ wrongReviews }) => {
    // 카테고리별 통계 계산
    const categoryStats = useMemo(
      () => calculateCategoryStats(wrongReviews),
      [wrongReviews]
    );

    // 정렬된 카테고리 목록
    const sortedCategories = useMemo(
      () =>
        Object.entries(categoryStats).sort(
          ([, a], [, b]) => b.questionCount - a.questionCount
        ),
      [categoryStats]
    );

    // 최대 문제 수 (상대적 너비 계산용)
    const maxCount = useMemo(
      () => Math.max(...Object.values(categoryStats).map((s) => s.questionCount), 1),
      [categoryStats]
    );

    if (sortedCategories.length === 0) {
      return (
        <div className="rounded-xl border border-gray-100 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
            <h3 className="font-semibold text-text-primary mb-0">카테고리별 오답 분포</h3>
          </div>
          <div className="p-5 text-center text-text-tertiary">
            오답 데이터가 없습니다.
          </div>
        </div>
      );
    }

    return (
      <div className="rounded-xl border border-gray-100 overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-50 bg-gray-50/50">
          <h3 className="font-semibold text-text-primary mb-0">카테고리별 오답 분포</h3>
        </div>
        <div className="p-5 space-y-5">
          {sortedCategories.map(([category, stats]) => (
            <CategoryBar
              key={category}
              category={category}
              questionCount={stats.questionCount}
              totalWrongCount={stats.totalWrongCount}
              maxCount={maxCount}
            />
          ))}
        </div>
      </div>
    );
  }
);

CategoryDistribution.displayName = 'CategoryDistribution';

// === 개별 카테고리 바 ===
interface CategoryBarProps {
  category: string;
  questionCount: number;
  totalWrongCount: number;
  maxCount: number;
}

const CategoryBar: React.FC<CategoryBarProps> = React.memo(
  ({ category, questionCount, totalWrongCount, maxCount }) => {
    const percentage = (questionCount / maxCount) * 100;

    return (
      <div>
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-text-primary">{category}</span>
          <span className="text-sm text-text-tertiary">
            {questionCount}문제 · 총 {totalWrongCount}회 오답
          </span>
        </div>
        <div className="w-full bg-gray-100 rounded-full h-3 overflow-hidden">
          <motion.div
            initial={{ width: 0 }}
            animate={{ width: `${percentage}%` }}
            transition={{ duration: 0.8, ease: 'easeOut' }}
            className={cn('h-3 rounded-full', getProgressBarColor(percentage))}
          />
        </div>
      </div>
    );
  }
);

CategoryBar.displayName = 'CategoryBar';

export default CategoryDistribution;
