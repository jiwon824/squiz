import React from 'react';
import { StatsViewProps } from '../types';
import { SummaryCards } from './SummaryCards';

import { TechStackProficiencyView } from './TechStackProficiency';
import { LearningRecommendation } from './LearningRecommendation';
import { CourseQuizStatsChart } from './CourseQuizStatsChart';

/**
 * 퀴즈 통계 뷰 컴포넌트
 *
 * 구성:
 * - 요약 카드: 총 틀린 문제 수, 오답 횟수, 평균
 * - 코스별 학습 현황 그래프 (Attempted vs Correct)
 * - 기술 스택 숙련도
 * - 학습 제안
 */
export const StatsView: React.FC<StatsViewProps> = React.memo(
  ({ totalWrong, totalWrongCount, avgWrongCount, weakConcepts, courseQuizStats, reviewStats }) => {
    return (
      <div className="space-y-6">
        {/* 요약 카드 */}
        <SummaryCards
          totalWrong={totalWrong}
          totalWrongCount={totalWrongCount}
          avgWrongCount={avgWrongCount}
          reviewStats={reviewStats}
        />

        {/* 코스별 학습 현황 (꺾은선 그래프) */}
        <CourseQuizStatsChart data={courseQuizStats} />



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
