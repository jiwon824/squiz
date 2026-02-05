import React from 'react';
import { ReviewStatsResponse } from '../../../api/reviewApi';
import { cn } from '@/shared/utils/cn';

interface StatCard {
  label: string;
  value: number | string;
  color: 'retention' | 'mature' | 'combo';
  description: string;
}

interface SummaryCardsProps {
  totalWrong?: number; // kept for backward compatibility if needed, but not used in new design
  totalWrongCount?: number;
  avgWrongCount?: string;
  reviewStats?: ReviewStatsResponse | null;
}

/**
 * 통계 요약 카드를 표시하는 컴포넌트
 * (Retention Rate, Mature Cards, Daily Max Combo)
 */
export const SummaryCards: React.FC<SummaryCardsProps> = React.memo(
  ({ reviewStats }) => {
    // 1. 예상 기억도 (Retention Rate)
    const retentionRate = reviewStats?.averageRetrievability
      ? Math.round(reviewStats.averageRetrievability * 100)
      : 0;

    // 2. 장기 기억 전환 (Mature Cards)
    const matureCards = reviewStats?.matureCards || 0;

    // 3. 오늘의 최고 콤보 (Daily Max Combo)
    const dailyMaxCombo = reviewStats?.dailyMaxCombo || 0;

    const stats: StatCard[] = [
      {
        label: '예상 기억도',
        value: `${retentionRate}%`,
        color: 'retention',
        description: `현재 학습 개념의 ${retentionRate}%를 기억 중입니다`
      },
      {
        label: '장기 기억 전환',
        value: matureCards,
        color: 'mature',
        description: '21일 이상 기억된 완전 학습 카드'
      },
      {
        label: '오늘의 최고 콤보',
        value: dailyMaxCombo,
        color: 'combo',
        description: `오늘 최대 ${dailyMaxCombo}문제를 연속으로 맞혔습니다!`
      },
    ];

    return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {stats.map((stat) => (
          <StatCardItem key={stat.label} stat={stat} />
        ))}
      </div>
    );
  }
);

SummaryCards.displayName = 'SummaryCards';

// === 개별 통계 카드 ===
interface StatCardItemProps {
  stat: StatCard;
}

const StatCardItem: React.FC<StatCardItemProps> = React.memo(({ stat }) => {
  // 색상 매핑
  const colorStyles = {
    retention: {
      bg: 'bg-indigo-50',
      text: 'text-indigo-600',
    },
    mature: {
      bg: 'bg-emerald-50',
      text: 'text-emerald-600',
    },
    combo: {
      bg: 'bg-amber-50',
      text: 'text-amber-600',
    },
  };

  const style = colorStyles[stat.color];

  return (
    <div className="rounded-xl border border-gray-100 px-5 py-4 bg-white shadow-sm hover:shadow-md transition-shadow duration-200">
      <div className="flex flex-col gap-2">
        <div className="flex items-baseline gap-2">
          <span className={cn("text-3xl font-bold", style.text)}>{stat.value}</span>
          <span className="text-sm text-gray-500 font-medium">{stat.label}</span>
        </div>
        <p className="text-xs text-gray-500 leading-relaxed">
          {stat.description}
        </p>
      </div>
    </div>
  );
});

StatCardItem.displayName = 'StatCardItem';

export default SummaryCards;
