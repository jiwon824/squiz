import React from 'react';
import { Bookmark, Users, MapPin, Clock, Star, Zap, Monitor, Handshake, Layers } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { DifficultyBadge } from './DifficultyBadge';

// 기본 프로필 이미지 경로
const DEFAULT_PROFILE_IMAGE = '/images/default-profile.png';

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
            leaderRating: number | null; // null이면 리뷰가 없는 상태
            leaderReviewCount: number;
        };
        isBookmarked: boolean;
    };
    variant?: 'card' | 'list';
    onBookmarkToggle?: (studyId: number) => void;
    onClick?: (studyId: number) => void;
}

/**
 * StudyCardContentV2 - Google Material Design 스타일 스터디 카드
 */
const StudyCardContentV2: React.FC<StudyCardContentV2Props> = ({ study, variant = 'card', onBookmarkToggle, onClick }) => {
    // 미팅 타입 텍스트 (스터디 상세 페이지와 동일)
    const getMeetingTypeText = (meetingType: string) => {
        switch (meetingType) {
            case 'ONLINE': return '온라인';
            case 'OFFLINE': return '오프라인';
            case 'HYBRID': return '온/오프라인 혼합';
            default: return meetingType;
        }
    };

    // 미팅 타입 아이콘 (스터디 상세 페이지와 동일)
    const getMeetingTypeIcon = (meetingType: string, size: number = 14) => {
        switch (meetingType) {
            case 'ONLINE': return <Monitor size={size} />;
            case 'OFFLINE': return <Handshake size={size} />;
            case 'HYBRID': return <Layers size={size} />;
            default: return <Monitor size={size} />;
        }
    };

    const isFullCapacity = study.currentMembers >= study.maxMembers;
    const isLightning = study.studyType === 'LIGHTNING';

    // 마감 조건: 인원이 다 찼거나 모집 마감일이 지남
    const isRecruitDeadlinePassed = study.recruitEndDate && isDeadlinePassed(study.recruitEndDate);
    const isClosed = isFullCapacity || isRecruitDeadlinePassed;

    // 마감 임박 조건: 모집 인원 1명 남음 OR 마감일이 오늘
    // 모집중 상태에서만 표시 (RECRUITING 또는 완료/취소가 아닌 상태)
    const remainingSlots = study.maxMembers - study.currentMembers;
    const isNotClosed = study.status !== 'COMPLETED' && study.status !== 'CANCELLED';
    const isClosingSoon = isNotClosed && !isClosed && (
        remainingSlots === 1 ||
        (study.recruitEndDate && isDeadlineToday(study.recruitEndDate))
    );

    // 지역 표시 텍스트 (meetingType에 따라 다르게 표시)
    const getRegionText = () => {
        if (study.meetingType === 'ONLINE') {
            return '전국';
        }
        // 오프라인/혼합인 경우
        return study.region?.name || '미지정';
    };

    // 리스트 뷰
    if (variant === 'list') {
        return (
            <div
                onClick={() => onClick?.(study.id)}
                className={cn(
                    "group relative bg-white border border-[var(--color-border)] rounded-xl",
                    "p-4 transition-all duration-200 cursor-pointer",
                    "hover:shadow-md hover:border-[var(--color-primary-alpha-20)]",
                    "flex items-center gap-4",
                    study.status === 'COMPLETED' && "opacity-60"
                )}
            >
                {/* 왼쪽: 리더 프로필 */}
                <div className="flex-shrink-0">
                    <img
                        src={study.leader.profileImage || DEFAULT_PROFILE_IMAGE}
                        alt={study.leader.nickname}
                        className="w-12 h-12 rounded-xl object-cover border border-[var(--color-border-lighter)]"
                    />
                </div>

                {/* 중앙: 정보 */}
                <div className="flex-1 min-w-0">
                    {/* 상단: 뱃지 영역 - 고정 높이, 자연스럽게 붙음 */}
                    <div className="flex items-center gap-2 h-5 mb-1">
                        <DifficultyBadge difficulty={study.difficulty} size="sm" />
                        {isLightning && (
                            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-[var(--color-warning-light)] text-[var(--color-warning)] flex items-center gap-0.5">
                                <Zap size={8} />
                                번개
                            </span>
                        )}
                        {isClosed && (
                            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-gray-400 text-white">
                                마감
                            </span>
                        )}
                        <span className="text-xs text-[var(--color-primary)] font-medium truncate">{study.topic}</span>
                    </div>

                    {/* 타이틀 */}
                    <h3 className="text-base font-bold text-[var(--color-text-primary)] truncate group-hover:text-[var(--color-primary)] transition-colors">
                        {study.name}
                    </h3>

                    {/* 정보 태그들 - 자연스럽게 붙음 */}
                    <div className="flex items-center gap-3 mt-1.5 text-xs text-[var(--color-text-secondary)]">
                        <span className={cn("flex items-center gap-1", isFullCapacity && "text-[var(--color-error)] font-semibold")}>
                            <Users size={12} />
                            {study.currentMembers}/{study.maxMembers}명
                        </span>
                        <span className="flex items-center gap-1">
                            {getMeetingTypeIcon(study.meetingType, 12)}
                            {getMeetingTypeText(study.meetingType)}
                        </span>
                        <span className="flex items-center gap-1">
                            <MapPin size={12} />
                            {getRegionText()}
                        </span>
                        <span className="flex items-center gap-1">
                            <Clock size={12} />
                            {study.scheduleTime ? study.scheduleTime.substring(0, 5) : '미정'}
                        </span>
                    </div>
                </div>

                {/* 오른쪽: 리더 정보 + 액션 */}
                <div className="flex items-center gap-4 flex-shrink-0">
                    {/* 리더 정보 */}
                    <div className="text-right hidden sm:block">
                        <p className="text-sm font-semibold text-[var(--color-text-primary)]">{study.leader.nickname}</p>
                        <div className="flex items-center justify-end gap-1 mt-0.5">
                            <Star size={12} className={cn(
                                study.leader.leaderReviewCount > 0 ? 'text-yellow-400 fill-current' : 'text-gray-300'
                            )} />
                            <span className="text-xs font-semibold text-[var(--color-text-primary)]">
                                {study.leader.leaderReviewCount > 0 && study.leader.leaderRating != null
                                    ? study.leader.leaderRating.toFixed(1)
                                    : '-'}
                            </span>
                            <span className="text-[10px] text-[var(--color-text-tertiary)]">
                                ({study.leader.leaderReviewCount}개 리뷰)
                            </span>
                        </div>
                    </div>

                    {/* 찜 버튼 */}
                    <button
                        className={cn(
                            "p-2 rounded-full transition-all",
                            "hover:bg-amber-50",
                            study.isBookmarked ? "text-amber-500" : "text-[var(--color-text-muted)] hover:text-amber-500"
                        )}
                        onClick={(e) => {
                            e.stopPropagation();
                            onBookmarkToggle?.(study.id);
                        }}
                    >
                        <Bookmark
                            size={18}
                            fill={study.isBookmarked ? 'currentColor' : 'none'}
                        />
                    </button>
                </div>

                {/* 모집 마감 임박 표시 */}
                {isClosingSoon && (
                    <div className="absolute top-0 left-4 bg-[var(--color-warning)] text-white text-[9px] font-bold px-2 py-0.5 rounded-b">
                        마감 임박
                    </div>
                )}
            </div>
        );
    }

    // 카드 뷰 (기본)
    return (
        <div
            onClick={() => onClick?.(study.id)}
            className={cn(
                "group relative bg-white border border-[var(--color-border)] rounded-2xl",
                "p-6 transition-all duration-300 cursor-pointer",
                "hover:shadow-lg hover:shadow-[var(--color-shadow-medium)] hover:-translate-y-1",
                "hover:border-[var(--color-primary-alpha-20)]",
                "h-[400px] flex flex-col",
                study.status === 'COMPLETED' && "opacity-60"
            )}
        >
            {/* 상단: 뱃지 영역 */}
            <div className="flex items-center justify-between mb-3 flex-shrink-0">
                <div className="flex items-center gap-2">
                    <DifficultyBadge difficulty={study.difficulty} size="sm" />
                    {isLightning && (
                        <span className="px-2.5 py-1 rounded-md text-[11px] font-bold bg-[var(--color-warning-light)] text-[var(--color-warning)] flex items-center gap-1">
                            <Zap size={10} />
                            번개
                        </span>
                    )}
                    {isClosed && (
                        <span className="px-2.5 py-1 rounded-md text-[11px] font-bold bg-gray-400 text-white">
                            마감
                        </span>
                    )}
                </div>

                {/* 찜 버튼 */}
                <button
                    className={cn(
                        "p-2 rounded-full transition-all -mr-2",
                        "hover:bg-amber-50",
                        study.isBookmarked ? "text-amber-500" : "text-[var(--color-text-muted)] hover:text-amber-500"
                    )}
                    onClick={(e) => {
                        e.stopPropagation();
                        onBookmarkToggle?.(study.id);
                    }}
                >
                    <Bookmark
                        size={18}
                        fill={study.isBookmarked ? 'currentColor' : 'none'}
                    />
                </button>
            </div>

            {/* 주제 태그 */}
            <div className="mb-1.5 flex-shrink-0">
                <span className="text-xs font-semibold text-[var(--color-primary)]">
                    {study.topic}
                </span>
            </div>

            {/* 타이틀 - 고정 높이 (2줄) */}
            <h3 className="text-lg font-bold text-[var(--color-text-primary)] mb-2 line-clamp-2 leading-snug group-hover:text-[var(--color-primary)] transition-colors min-h-[52px] flex-shrink-0">
                {study.name}
            </h3>

            {/* 설명 - 고정 높이 (3줄) */}
            <p className="text-sm text-[var(--color-text-secondary)] line-clamp-3 leading-relaxed min-h-[63px] flex-shrink-0">
                {study.description}
            </p>

            {/* 정보 그리드 - 고정 2x2 (스터디 상세 페이지와 동일한 아이콘) */}
            <div className="grid grid-cols-2 gap-2.5 mt-auto flex-shrink-0">
                <InfoChip icon={<Users size={14} />} text={`${study.currentMembers}/${study.maxMembers}명`} highlight={isFullCapacity} />
                <InfoChip icon={getMeetingTypeIcon(study.meetingType)} text={getMeetingTypeText(study.meetingType)} />
                <InfoChip icon={<Clock size={14} />} text={study.scheduleTime ? study.scheduleTime.substring(0, 5) : '협의 후 결정'} />
                <InfoChip icon={<MapPin size={14} />} text={getRegionText()} />
            </div>

            {/* 하단: 리더 정보 - 항상 맨 아래 */}
            <div className="flex items-center gap-4 pt-4 mt-4 border-t border-[var(--color-border-lighter)] flex-shrink-0">
                <img
                    src={study.leader.profileImage || DEFAULT_PROFILE_IMAGE}
                    alt={study.leader.nickname}
                    className="w-9 h-9 rounded-xl object-cover border border-[var(--color-border-lighter)]"
                />
                <span className="text-[15px] font-semibold text-[var(--color-text-primary)] truncate flex-1">
                    {study.leader.nickname}
                </span>
                <div className="flex items-center gap-1.5">
                    <Star size={16} className={cn(
                        study.leader.leaderReviewCount > 0 ? 'text-yellow-400 fill-current' : 'text-gray-300'
                    )} />
                    <span className="text-[15px] font-semibold text-[var(--color-text-primary)]">
                        {study.leader.leaderReviewCount > 0 && study.leader.leaderRating != null
                            ? study.leader.leaderRating.toFixed(1)
                            : '-'}
                    </span>
                    <span className="text-xs text-[var(--color-text-tertiary)]">
                        ({study.leader.leaderReviewCount}개 리뷰)
                    </span>
                </div>
            </div>

            {/* 모집 마감 임박 표시 */}
            {isClosingSoon && (
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

// 마감일이 오늘인지 체크 (날짜만 비교)
const isDeadlineToday = (dateStr: string): boolean => {
    const deadline = new Date(dateStr);
    const today = new Date();
    // 날짜만 비교 (시간 제외)
    return deadline.getFullYear() === today.getFullYear() &&
           deadline.getMonth() === today.getMonth() &&
           deadline.getDate() === today.getDate();
};

// 마감일이 지났는지 체크
const isDeadlinePassed = (dateStr: string): boolean => {
    const deadline = new Date(dateStr);
    deadline.setHours(23, 59, 59, 999); // 마감일 당일 23:59:59까지 유효
    const today = new Date();
    return today > deadline;
};

export default StudyCardContentV2;
