import React, { useCallback } from 'react';
import { cn } from '@/shared/utils/cn';
import { WrongAnswerSortType } from '../../api/reviewApi';

interface SortOption {
  value: WrongAnswerSortType;
  label: string;
}

const SORT_OPTIONS: SortOption[] = [
  { value: 'LATEST', label: '최신순' },
  { value: 'MOST_WRONG', label: '많이 틀린 순' },
  { value: 'FSRS_RECOMMENDED', label: '복습 우선순위' },
];

interface SortToggleProps {
  currentSort: WrongAnswerSortType;
  onSortChange: (sort: WrongAnswerSortType) => void;
}

/**
 * 정렬 옵션 토글 버튼 그룹
 */
export const SortToggle: React.FC<SortToggleProps> = React.memo(
  ({ currentSort, onSortChange }) => {
    return (
      <div className="mb-4 flex gap-2">
        {SORT_OPTIONS.map((option) => (
          <SortButton
            key={option.value}
            option={option}
            isActive={currentSort === option.value}
            onClick={onSortChange}
          />
        ))}
      </div>
    );
  }
);

SortToggle.displayName = 'SortToggle';

// === 개별 정렬 버튼 ===
interface SortButtonProps {
  option: SortOption;
  isActive: boolean;
  onClick: (value: WrongAnswerSortType) => void;
}

const SortButton: React.FC<SortButtonProps> = React.memo(
  ({ option, isActive, onClick }) => {
    const handleClick = useCallback(() => {
      onClick(option.value);
    }, [option.value, onClick]);

    return (
      <button
        onClick={handleClick}
        className={cn(
          'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
          isActive
            ? 'bg-primary text-white'
            : 'bg-gray-100 text-text-secondary hover:bg-gray-200'
        )}
      >
        {option.label}
      </button>
    );
  }
);

SortButton.displayName = 'SortButton';

export default SortToggle;
