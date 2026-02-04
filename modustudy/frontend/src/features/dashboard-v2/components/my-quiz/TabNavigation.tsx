import React, { useMemo } from 'react';
import { motion } from 'framer-motion';
import { Clock, XCircle, TrendingDown, BarChart3 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { TabType, TabConfig } from './types';

interface TabNavigationProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
  todayReviewCount: number;
  wrongReviewCount: number;
  weakConceptCount: number;
}

/**
 * 탭 네비게이션 컴포넌트
 *
 * 좌측 사이드바 형태의 탭 목록을 표시합니다.
 */
export const TabNavigation: React.FC<TabNavigationProps> = React.memo(
  ({
    activeTab,
    onTabChange,
    todayReviewCount,
    wrongReviewCount,
    weakConceptCount,
  }) => {
    // 탭 구성 정보
    const tabs: TabConfig[] = useMemo(
      () => [
        { id: 'review', label: '오늘의 복습', icon: Clock, count: todayReviewCount },
        { id: 'wrong', label: '틀린 문제', icon: XCircle, count: wrongReviewCount },
        { id: 'weak', label: '취약 개념', icon: TrendingDown, count: weakConceptCount },
        { id: 'stats', label: '통계', icon: BarChart3 },
      ],
      [todayReviewCount, wrongReviewCount, weakConceptCount]
    );

    return (
      <div className="w-60 flex-shrink-0 bg-gray-50/70 relative">
        {tabs.map((tab) => (
          <TabButton
            key={tab.id}
            tab={tab}
            isActive={activeTab === tab.id}
            onClick={() => onTabChange(tab.id)}
          />
        ))}
        {/* 우측 border line */}
        <div className="absolute top-0 right-0 bottom-0 w-px bg-gray-200" />
      </div>
    );
  }
);

TabNavigation.displayName = 'TabNavigation';

// === 개별 탭 버튼 ===
interface TabButtonProps {
  tab: TabConfig;
  isActive: boolean;
  onClick: () => void;
}

const TabButton: React.FC<TabButtonProps> = React.memo(
  ({ tab, isActive, onClick }) => {
    const Icon = tab.icon;

    return (
      <button
        onClick={onClick}
        className={cn(
          'w-full flex items-center gap-2 px-4 py-4 text-sm font-medium transition-colors relative',
          isActive
            ? 'text-primary bg-white -mr-px z-10'
            : 'text-text-secondary hover:text-text-primary hover:bg-gray-100 border-b border-gray-100'
        )}
      >
        {/* 왼쪽 인디케이터 */}
        {isActive && (
          <motion.div
            layoutId="tab-indicator"
            className="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-r"
          />
        )}
        <Icon size={18} />
        <span>{tab.label}</span>
        {tab.count !== undefined && (
          <span
            className={cn(
              'ml-auto px-1.5 py-0.5 rounded-full text-xs',
              isActive
                ? 'bg-primary/10 text-primary'
                : 'bg-gray-200 text-text-tertiary'
            )}
          >
            {tab.count}
          </span>
        )}
      </button>
    );
  }
);

TabButton.displayName = 'TabButton';

export default TabNavigation;
