import React from 'react';
import { Heart, Users, MapPin, Calendar, Clock, Star, Zap } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface StudyCardContentV2Props {
    study: {
        id: number;
        name: string;
        description: string;
        topic: string;
        format: string;
        studyType: string;
        meetingType: string;
        status: string;
        maxMembers: number;
        currentMembers: number;
        difficulty: string;
        scheduleDays: string;
        scheduleTime?: string;
        regionId?: number;
        locationDetail?: string;
        recruitEndDate?: string;
        region?: {
            id: number;
            name: string;
        };
        leader: {
            id: number;
            nickname: string;
            profileImage?: string | null;
            leaderRating: number;
            leaderReviewCount: number;
        };
        isBookmarked: boolean;
    };
    onBookmarkToggle?: (studyId: number) => void;
    onClick?: (studyId: number) => void;
}

/**
 * StudyCardContentV2 - Google Material Design 스타일 스터디 카드
 *
 * 특징:
 * - 깔끔한 카드 레이아웃
 * - CSS 변수 활용
 * - 명확한 정보 계층
 * - 부드러운 호버 효과
 */
const StudyCardContentV2: React.FC<StudyCardContentV2Props> = ({ study, onBookmarkToggle, onClick }) => {
    // 상태별 텍스트 및 스타일
    const getStatusConfig = (status: string) => {
        switch (status) {
            case 'RECRUITING':
                return { text: '모집중', color: 'bg-[var(--color-success)] text-white' };
            case 'IN_PROGRESS':
                return { text: '진행중', color: 'bg-[var(--color-primary)] text-white' };
            case 'COMPLETED':
                return { text: '완료', color: 'bg-[var(--color-text-tertiary)] text-white' };
            default:
                return { text: status, color: 'bg-gray-400 text-white' };
        }
    };

    // 난이도별 스타일
    const getDifficultyConfig = (difficulty: string) => {
        switch (difficulty) {
            case 'BEGINNER':
            case 'ELEMENTARY':
                return { text: '입문', color: 'text-[var(--color-success)]', bgColor: 'bg-[var(--color-success-light)]' };
            case 'INTERMEDIATE':
                return { text: '중급', color: 'text-[var(--color-primary)]', bgColor: 'bg-[var(--color-primary-alpha-10)]' };
            case 'ADVANCED':
                return { text: '고급', color: 'text-[var(--color-error)]', bgColor: 'bg-[var(--color-error-light)]' };
            default:
                return { text: difficulty, color: 'text-[var(--color-text-tertiary)]', bgColor: 'bg-gray-100' };
        }
    };

    // 미팅 타입 텍스트
    const getMeetingTypeText = (meetingType: string) => {
        switch (meetingType) {
            case 'ONLINE': return '온라인';
            case 'OFFLINE': return '오프라인';
            case 'HYBRID': return '혼합';
            default: return meetingType;
        }
    };

    // 요일 포맷팅
    const formatDays = (days: string) => {
        const dayMap: { [key: string]: string } = {
            MON: '월', TUE: '화', WED: '수', THU: '목', FRI: '금', SAT: '토', SUN: '일',
        };
        return days
            .split(',')
            .map((day) => dayMap[day.trim()] || day)
            .join(' · ');
    };

    const statusConfig = getStatusConfig(study.status);
    const diffConfig = getDifficultyConfig(study.difficulty);
    const isFullCapacity = study.currentMembers >= study.maxMembers;
    const isLightning = study.studyType === 'LIGHTNING';

    return (
        <div
            onClick={() => onClick?.(study.id)}
            className={cn(
                "group relative bg-white border border-[var(--color-border)] rounded-2xl",
                "p-5 transition-all duration-300 cursor-pointer",
                "hover:shadow-lg hover:shadow-[var(--color-shadow-medium)] hover:-translate-y-1",
                "hover:border-[var(--color-primary-alpha-20)]",
                study.status === 'COMPLETED' && "opacity-60"
            )}
        >
            {/* 상단: 뱃지 영역 */}
            <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                    <span className={cn(
                        "px-2.5 py-1 rounded-md text-[11px] font-bold",
                        statusConfig.color
                    )}>
                        {statusConfig.text}
                    </span>
                    <span className={cn(
                        "px-2.5 py-1 rounded-md text-[11px] font-bold",
                        diffConfig.color,
                        diffConfig.bgColor
                    )}>
                        {diffConfig.text}
                    </span>
                    {isLightning && (
                        <span className="px-2.5 py-1 rounded-md text-[11px] font-bold bg-[var(--color-warning-light)] text-[var(--color-warning)] flex items-center gap-1">
                            <Zap size={10} />
                            번개
                        </span>
                    )}
                </div>

                {/* 찜 버튼 */}
                <button
                    className={cn(
                        "p-2 rounded-full transition-all",
                        "hover:bg-[var(--color-error-light)]",
                        study.isBookmarked ? "text-[var(--color-error)]" : "text-[var(--color-text-muted)] hover:text-[var(--color-error)]"
                    )}
                    onClick={(e) => {
                        e.stopPropagation();
                        onBookmarkToggle?.(study.id);
                    }}
                >
                    <Heart
                        size={18}
                        fill={study.isBookmarked ? 'currentColor' : 'none'}
                    />
                </button>
            </div>

            {/* 주제 태그 */}
            <div className="mb-2">
                <span className="text-xs font-semibold text-[var(--color-primary)]">
                    {study.topic}
                </span>
            </div>

            {/* 타이틀 */}
            <h3 className="text-lg font-bold text-[var(--color-text-primary)] mb-2 line-clamp-2 leading-snug group-hover:text-[var(--color-primary)] transition-colors">
                {study.name}
            </h3>

            {/* 설명 */}
            <p className="text-sm text-[var(--color-text-secondary)] line-clamp-2 mb-4 leading-relaxed">
                {study.description}
            </p>

            {/* 정보 그리드 */}
            <div className="grid grid-cols-2 gap-3 mb-5">
                <InfoChip icon={<Users size={14} />} text={`${study.currentMembers}/${study.maxMembers}명`} highlight={isFullCapacity} />
                <InfoChip icon={<MapPin size={14} />} text={getMeetingTypeText(study.meetingType)} />
                <InfoChip icon={<Calendar size={14} />} text={formatDays(study.scheduleDays)} />
                <InfoChip icon={<Clock size={14} />} text={study.scheduleTime ? study.scheduleTime.substring(0, 5) : '시간 미정'} />
            </div>

            {/* 하단: 리더 정보 */}
            <div className="flex items-center justify-between pt-4 border-t border-[var(--color-border-lighter)]">
                <div className="flex items-center gap-3">
                    {study.leader.profileImage ? (
                        <img
                            src={study.leader.profileImage}
                            alt={study.leader.nickname}
                            className="w-9 h-9 rounded-xl object-cover border border-[var(--color-border-lighter)]"
                        />
                    ) : (
                        <div className="w-9 h-9 rounded-xl bg-[var(--color-primary)] text-white flex items-center justify-center text-sm font-bold">
                            {study.leader.nickname.charAt(0)}
                        </div>
                    )}
                    <div>
                        <p className="text-sm font-bold text-[var(--color-text-primary)] truncate max-w-[100px]">
                            {study.leader.nickname}
                        </p>
                        <p className="text-[11px] text-[var(--color-text-tertiary)]">스터디장</p>
                    </div>
                </div>

                {/* 평점 */}
                <div className="flex items-center gap-1 px-2 py-1 bg-[var(--color-background-secondary)] rounded-lg">
                    <Star size={12} className="text-yellow-400 fill-current" />
                    <span className="text-xs font-bold text-[var(--color-text-primary)]">
                        {study.leader.leaderRating.toFixed(1)}
                    </span>
                    <span className="text-[10px] text-[var(--color-text-tertiary)]">
                        ({study.leader.leaderReviewCount})
                    </span>
                </div>
            </div>

            {/* 모집 마감 임박 표시 */}
            {study.recruitEndDate && study.status === 'RECRUITING' && isDeadlineSoon(study.recruitEndDate) && (
                <div className="absolute top-0 left-0 right-0 bg-[var(--color-warning)] text-white text-[10px] font-bold text-center py-1 rounded-t-2xl">
                    마감 임박
                </div>
            )}
        </div>
    );
};

// 정보 칩 컴포넌트
interface InfoChipProps {
    icon: React.ReactNode;
    text: string;
    highlight?: boolean;
}

const InfoChip: React.FC<InfoChipProps> = ({ icon, text, highlight }) => (
    <div className={cn(
        "flex items-center gap-2 text-xs",
        highlight ? "text-[var(--color-error)] font-semibold" : "text-[var(--color-text-secondary)]"
    )}>
        <span className={cn(
            "p-1.5 rounded-lg",
            highlight ? "bg-[var(--color-error-light)]" : "bg-[var(--color-background-secondary)]"
        )}>
            <span className={highlight ? "text-[var(--color-error)]" : "text-[var(--color-primary)]"}>
                {icon}
            </span>
        </span>
        <span className="truncate">{text}</span>
    </div>
);

// 마감일 임박 체크 함수 (3일 이내)
const isDeadlineSoon = (dateStr: string): boolean => {
    const deadline = new Date(dateStr);
    const today = new Date();
    const diffDays = Math.ceil((deadline.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    return diffDays >= 0 && diffDays <= 3;
};

export default StudyCardContentV2;
