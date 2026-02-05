import React, { useMemo } from 'react';
import { motion } from 'framer-motion';
import { CheckCircle2, XCircle, BookOpen, Clock } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { WeakConceptListProps, WeakConcept } from './types';
import { getWeaknessBarColor, getRankBadgeColor } from './utils';

/**
 * 취약 개념 목록을 표시하는 컴포넌트
 *
 * 특징:
 * - 오답률에 따른 정렬
 * - 애니메이션 진행 바
 * - 순위별 뱃지 색상
 */
export const WeakConceptList: React.FC<WeakConceptListProps> = React.memo(
  ({ concepts }) => {
    // 오답률 기준으로 정렬
    const sortedConcepts = useMemo(
      () => [...concepts].sort((a, b) => b.wrongRate - a.wrongRate),
      [concepts]
    );

    // 상대적인 막대 길이 계산용 최대 오답률
    const maxRate = useMemo(
      () => Math.max(...sortedConcepts.map((c) => c.wrongRate), 1),
      [sortedConcepts]
    );

    // 빈 상태 표시
    if (concepts.length === 0) {
      return <EmptyState />;
    }

    return (
      <div className="space-y-4">
        {sortedConcepts.map((concept, index) => (
          <WeakConceptItem
            key={concept.id}
            concept={concept}
            index={index}
            maxRate={maxRate}
          />
        ))}
      </div>
    );
  }
);

WeakConceptList.displayName = 'WeakConceptList';

// === 빈 상태 컴포넌트 ===
const EmptyState: React.FC = () => (
  <div className="flex flex-col items-center justify-center py-20 text-center">
    <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
      <CheckCircle2 size={32} className="text-gray-400" />
    </div>
    <h3 className="text-lg font-bold text-text-primary mb-1">
      취약한 개념이 없습니다
    </h3>
    <p className="text-text-tertiary">
      틀린 문제가 없어서 분석할 취약점이 없습니다.
    </p>
  </div>
);

// === 개별 취약 개념 아이템 컴포넌트 ===
interface WeakConceptItemProps {
  concept: WeakConcept;
  index: number;
  maxRate: number;
}

const WeakConceptItem: React.FC<WeakConceptItemProps> = React.memo(
  ({ concept, index, maxRate }) => {
    const relativeWidth = useMemo(
      () => (maxRate > 0 ? (concept.wrongRate / maxRate) * 100 : 0),
      [concept.wrongRate, maxRate]
    );

    return (
      <div className="px-5 py-4 rounded-xl bg-white shadow-[0_4px_15px_rgba(0,0,0,0.05)] hover:shadow-[0_8px_25px_rgba(0,0,0,0.1)] hover:bg-gray-50 transition-all">
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            {/* 순위 및 카테고리 */}
            <div className="flex items-center gap-3 mb-3">
              <RankBadge index={index} />
              <span className="px-2.5 py-1 bg-gray-50 rounded-full text-xs font-medium text-gray-600">
                {concept.category}
              </span>
            </div>

            {/* 개념 이름 */}
            <h4 className="font-bold text-gray-900 mb-2">{concept.concept}</h4>

            {/* 통계 정보 */}
            <ConceptStats concept={concept} />
          </div>

          {/* 취약도 바 */}
          <WeaknessBar wrongRate={concept.wrongRate} relativeWidth={relativeWidth} />
        </div>
      </div>
    );
  }
);

WeakConceptItem.displayName = 'WeakConceptItem';

// === 순위 뱃지 컴포넌트 ===
interface RankBadgeProps {
  index: number;
}

const RankBadge: React.FC<RankBadgeProps> = React.memo(({ index }) => (
  <span
    className={cn(
      'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold',
      getRankBadgeColor(index)
    )}
  >
    {index + 1}
  </span>
));

RankBadge.displayName = 'RankBadge';

// === 개념 통계 컴포넌트 ===
interface ConceptStatsProps {
  concept: WeakConcept;
}

const ConceptStats: React.FC<ConceptStatsProps> = React.memo(({ concept }) => (
  <div className="flex items-center gap-4 text-xs text-gray-500">
    <span className="flex items-center gap-1.5">
      <XCircle size={12} className="text-red-500" />
      오답률 {Math.round(concept.wrongRate)}%
      <span className="text-gray-300">|</span>
      {concept.wrongCount}회 오답
    </span>
    <span className="flex items-center gap-1.5">
      <BookOpen size={12} />
      관련 {concept.relatedQuestions.length}문제
    </span>
    {concept.lastReviewDate && (
      <span className="flex items-center gap-1.5">
        <Clock size={12} />
        {new Date(concept.lastReviewDate).toLocaleDateString()}
      </span>
    )}
  </div>
));

ConceptStats.displayName = 'ConceptStats';

// === 취약도 바 컴포넌트 ===
interface WeaknessBarProps {
  wrongRate: number;
  relativeWidth: number;
}

const WeaknessBar: React.FC<WeaknessBarProps> = React.memo(
  ({ wrongRate, relativeWidth }) => (
    <div className="flex-shrink-0 w-28">
      <div className="text-xs text-gray-500 mb-1.5 text-right">취약도</div>
      <div className="w-full bg-gray-100 rounded-full h-2 overflow-hidden">
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${relativeWidth}%` }}
          transition={{ duration: 0.8, ease: 'easeOut' }}
          className={cn('h-2 rounded-full', getWeaknessBarColor(wrongRate))}
        />
      </div>
    </div>
  )
);

WeaknessBar.displayName = 'WeaknessBar';

export default WeakConceptList;
