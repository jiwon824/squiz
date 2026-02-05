import React from 'react';
import { ReviewStatsResponse } from '../../../api/reviewApi';
import { XCircle, AlertTriangle, Target, LucideIcon } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface StatCard {
  label: string;
  value: number | string;
  icon: LucideIcon;
  color: 'error' | 'warning' | 'secondary';
}

interface SummaryCardsProps {
  totalWrong: number;
  totalWrongCount: number;
  avgWrongCount: string;
  reviewStats?: ReviewStatsResponse | null;
}

/**
 * 통계 요약 카드를 표시하는 컴포넌트
 */
export const SummaryCards: React.FC<SummaryCardsProps> = React.memo(
  ({ totalWrong, totalWrongCount, avgWrongCount, reviewStats }) => {
    // reviewStats가 있으면 그것을 우선 사용
    // 총 오답 횟수는 reviewStats.totalLapses (누적) 사용
    const displayTotalWrongCount = reviewStats ? reviewStats.totalLapses : totalWrongCount;

    // 평균 오답 횟수도 reviewStats 데이터로 재계산 가능하지만, 
    // 기존 로직(오답 문제들의 평균)을 유지하되 분모/분자를 명확히 함.
    // 여기서는 단순히 카운트를 업데이트합니다.

    // 만약 reviewStats가 있다면, 평균은 (총 누적 오답 / 총 누적 오답 문제수)여야 하는데, 
    // 총 누적 오답 문제수는 reviewStats에 없으므로 기존 wrongReviews.length(totalWrong)을 사용하거나
    // totalWrong이 0이면 0으로 처리.
    // 기존 avgWrongCount는 이미 계산되어 넘어오므로 그대로 사용하되, 
    // displayTotalWrongCount가 바뀌었으므로 일관성을 위해 다시 계산할 수도 있음.
    // 하지만 일단 넘겨받은 값을 존중하되, 오답 카운트만 보정.

    const stats: StatCard[] = [
      { label: '틀린 문제 수', value: totalWrong, icon: XCircle, color: 'error' },
      { label: '총 오답 횟수', value: displayTotalWrongCount, icon: AlertTriangle, color: 'warning' },
      { label: '평균 오답 횟수', value: avgWrongCount, icon: Target, color: 'secondary' },
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
  const Icon = stat.icon;

  return (
    <div className="rounded-xl border border-gray-100 px-5 py-4">
      <div className="flex items-center gap-4">
        <div
          className={cn(
            'w-12 h-12 rounded-xl flex items-center justify-center',
            stat.color === 'error' && 'bg-error/5',
            stat.color === 'warning' && 'bg-warning/5',
            stat.color === 'secondary' && 'bg-secondary/5'
          )}
        >
          <Icon
            className={cn(
              stat.color === 'error' && 'text-error/80',
              stat.color === 'warning' && 'text-warning/80',
              stat.color === 'secondary' && 'text-secondary/80'
            )}
            size={22}
          />
        </div>
        <div>
          <div className="text-2xl font-bold text-text-primary">{stat.value}</div>
          <div className="text-sm text-text-tertiary">{stat.label}</div>
        </div>
      </div>
    </div>
  );
});

StatCardItem.displayName = 'StatCardItem';

export default SummaryCards;
