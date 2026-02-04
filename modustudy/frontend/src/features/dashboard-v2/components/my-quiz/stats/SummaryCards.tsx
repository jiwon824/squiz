import React from 'react';
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
}

/**
 * 통계 요약 카드를 표시하는 컴포넌트
 */
export const SummaryCards: React.FC<SummaryCardsProps> = React.memo(
  ({ totalWrong, totalWrongCount, avgWrongCount }) => {
    const stats: StatCard[] = [
      { label: '틀린 문제 수', value: totalWrong, icon: XCircle, color: 'error' },
      { label: '총 오답 횟수', value: totalWrongCount, icon: AlertTriangle, color: 'warning' },
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
