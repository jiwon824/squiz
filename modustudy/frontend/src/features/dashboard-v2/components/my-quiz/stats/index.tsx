import React from 'react';
import { StatsViewProps } from '../types';
import { SummaryCards } from './SummaryCards';
import { CategoryDistribution } from './CategoryDistribution';
import { TechStackProficiencyView } from './TechStackProficiency';
import { LearningRecommendation } from './LearningRecommendation';

/**
 * 퀴즈 통계 뷰 컴포넌트
 *
 * 구성:
 * - 요약 카드: 총 틀린 문제 수, 오답 횟수, 평균
 * - 카테고리별 오답 분포
 * - 기술 스택 숙련도
 * - 학습 제안
 */
export const StatsView: React.FC<StatsViewProps> = React.memo(
  ({ wrongReviews, totalWrong, totalWrongCount, avgWrongCount, weakConcepts }) => {
    return (
      <div className="space-y-6">
        {/* 요약 카드 */}
        <SummaryCards
          totalWrong={totalWrong}
          totalWrongCount={totalWrongCount}
          avgWrongCount={avgWrongCount}
        />

        {/* 카테고리별 오답 분포 */}
        <CategoryDistribution wrongReviews={wrongReviews} />

        {/* 기술 스택 숙련도 */}
        <TechStackProficiencyView />

        {/* 학습 제안 */}
        <LearningRecommendation weakConcepts={weakConcepts} />
      </div>
    );
  }
);

StatsView.displayName = 'StatsView';

// Re-export sub-components for direct access if needed
export { SummaryCards } from './SummaryCards';
export { CategoryDistribution } from './CategoryDistribution';
export { TechStackProficiencyView, TECH_STACK_DATA } from './TechStackProficiency';
export { LearningRecommendation } from './LearningRecommendation';

export default StatsView;
