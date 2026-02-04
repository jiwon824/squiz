import React from 'react';
import { Check } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { TECH_ITEMS, type TechItem } from '@/shared/constants/techItems';

interface TechStackSelectorProps {
    selected: string[];
    onChange: (selected: string[]) => void;
    maxSelect?: number;
    compact?: boolean;
    className?: string;
}

export const TechStackSelector: React.FC<TechStackSelectorProps> = ({
    selected,
    onChange,
    maxSelect,
    compact = false,
    className,
}) => {
    const handleToggle = (techId: string) => {
        if (selected.includes(techId)) {
            onChange(selected.filter(id => id !== techId));
        } else {
            if (maxSelect && selected.length >= maxSelect) return;
            onChange([...selected, techId]);
        }
    };

    const isSelected = (techId: string) => selected.includes(techId);
    const isDisabled = (techId: string) =>
        !isSelected(techId) && !!maxSelect && selected.length >= maxSelect;

    return (
        <div className={cn('w-full', className)}>
            {maxSelect && (
                <p className={cn(
                    'text-sm mb-3 text-center',
                    selected.length >= maxSelect ? 'text-red-500 font-medium' : 'text-gray-400'
                )}>
                    {selected.length} / {maxSelect} 선택됨
                </p>
            )}

            <div className={cn(
                'grid gap-2',
                compact
                    ? 'grid-cols-4 sm:grid-cols-5'
                    : 'grid-cols-4 sm:grid-cols-5 lg:grid-cols-6'
            )}>
                {TECH_ITEMS.map((tech) => (
                    <TechCard
                        key={tech.id}
                        tech={tech}
                        selected={isSelected(tech.id)}
                        disabled={isDisabled(tech.id)}
                        compact={compact}
                        onToggle={handleToggle}
                    />
                ))}
            </div>
        </div>
    );
};

interface TechCardProps {
    tech: TechItem;
    selected: boolean;
    disabled: boolean;
    compact: boolean;
    onToggle: (id: string) => void;
}

const TechCard = React.memo<TechCardProps>(({
    tech, selected, disabled, compact, onToggle,
}) => {
    return (
        <button
            type="button"
            onClick={() => onToggle(tech.id)}
            disabled={disabled}
            className={cn(
                'relative flex flex-col items-center justify-center rounded-xl border-2 transition-all duration-200 cursor-pointer',
                compact ? 'p-2 gap-1' : 'p-3 gap-1.5',
                selected
                    ? 'shadow-sm'
                    : 'border-gray-200 bg-white hover:border-gray-300 hover:shadow-sm',
                disabled && 'opacity-40 cursor-not-allowed',
            )}
            style={selected ? {
                borderColor: tech.color,
                backgroundColor: `${tech.color}15`,
            } : undefined}
        >
            {selected && (
                <div
                    className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full flex items-center justify-center shadow-sm"
                    style={{ backgroundColor: tech.color }}
                >
                    <Check size={12} className="text-white" strokeWidth={3} />
                </div>
            )}

            <div
                className={cn(
                    'rounded-lg flex items-center justify-center overflow-hidden',
                    compact ? 'w-10 h-10' : 'w-12 h-12'
                )}
                style={{ backgroundColor: selected ? `${tech.color}20` : `${tech.color}15` }}
            >
                {tech.icon ? (
                    <img
                        src={tech.icon}
                        alt={tech.name}
                        className={cn(
                            'object-contain',
                            compact ? 'w-7 h-7' : 'w-9 h-9'
                        )}
                        onError={(e) => {
                            // 아이콘 로드 실패 시 initial로 폴백
                            const target = e.target as HTMLImageElement;
                            target.style.display = 'none';
                            if (target.nextSibling) return;
                            const span = document.createElement('span');
                            span.className = `font-bold ${compact ? 'text-xs' : 'text-sm'}`;
                            span.style.color = tech.color;
                            span.textContent = tech.initial;
                            target.parentElement?.appendChild(span);
                        }}
                    />
                ) : (
                    <span
                        className={cn(
                            'font-bold',
                            compact ? 'text-xs' : 'text-sm'
                        )}
                        style={{ color: tech.color }}
                    >
                        {tech.initial}
                    </span>
                )}
            </div>

            <span className={cn(
                'font-medium text-center leading-tight truncate w-full',
                compact ? 'text-[10px]' : 'text-xs',
                selected ? 'text-gray-900' : 'text-gray-500'
            )}>
                {tech.name}
            </span>
        </button>
    );
});

TechCard.displayName = 'TechCard';
