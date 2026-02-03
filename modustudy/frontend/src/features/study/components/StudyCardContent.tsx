import React from 'react';
import { Heart, Users, MapPin, Calendar, Clock, Star } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { getProfileImageUrl } from '@/shared/utils/profileImage';

interface StudyCardContentProps {
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

const StudyCardContent: React.FC<StudyCardContentProps> = ({ study, onBookmarkToggle, onClick }) => {
    // 상태별 텍스트 및 스타일
    const getStatusConfig = (status: string) => {
        switch (status) {
            case 'RECRUITING':
                return { text: '모집중', className: 'bg-success/10 text-success border-success/20' };
            case 'IN_PROGRESS':
                return { text: '진행중', className: 'bg-primary/10 text-primary border-primary/20' };
            case 'COMPLETED':
                return { text: '완료', className: 'bg-text-tertiary/10 text-text-tertiary border-text-tertiary/20' };
            default:
                return { text: status, className: 'bg-background-secondary text-text-secondary border-border-light' };
        }
    };

    // 난이도별 스타일 (포인트 컬러)
    const getDifficultyColor = (difficulty: string) => {
        switch (difficulty) {
            case 'BEGINNER':
            case 'ELEMENTARY':
                return 'text-success border-success/30';
            case 'INTERMEDIATE':
                return 'text-primary border-primary/30';
            case 'ADVANCED':
                return 'text-error border-error/30';
            default:
                return 'text-text-tertiary border-border-light';
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

    // 지역 표시 텍스트 (meetingType에 따라 다르게 표시)
    const getRegionText = () => {
        if (study.meetingType === 'ONLINE') {
            return '전국';
        }
        // 오프라인/혼합인 경우
        return study.region?.name || '미지정';
    };

    // 요일 포맷팅 - 요일 순서대로 정렬 (대소문자/한글 모두 지원)
    const formatDays = (days: string) => {
        const dayOrder: { [key: string]: number } = {
            MON: 0, mon: 0, '월': 0,
            TUE: 1, tue: 1, '화': 1,
            WED: 2, wed: 2, '수': 2,
            THU: 3, thu: 3, '목': 3,
            FRI: 4, fri: 4, '금': 4,
            SAT: 5, sat: 5, '토': 5,
            SUN: 6, sun: 6, '일': 6,
        };
        const dayMap: { [key: string]: string } = {
            MON: '월', mon: '월', '월': '월',
            TUE: '화', tue: '화', '화': '화',
            WED: '수', wed: '수', '수': '수',
            THU: '목', thu: '목', '목': '목',
            FRI: '금', fri: '금', '금': '금',
            SAT: '토', sat: '토', '토': '토',
            SUN: '일', sun: '일', '일': '일',
        };
        const sortedDays = days
            .split(',')
            .map((day) => day.trim())
            .sort((a, b) => (dayOrder[a] ?? 99) - (dayOrder[b] ?? 99));
        return sortedDays.map((day) => dayMap[day] || day).join(', ');
    };

    const statusConfig = getStatusConfig(study.status);

    return (
        <div
            onClick={() => onClick?.(study.id)}
            className={cn(
                "group relative bg-white border border-border-light rounded-[40px] px-8 py-10 transition-all duration-700 hover:shadow-[0_30px_70px_rgba(0,0,0,0.08)] hover:-translate-y-3 cursor-pointer overflow-hidden flex flex-col h-[480px]",
                study.status === 'COMPLETED' && "opacity-60 grayscale-[0.6]"
            )}
        >
            {/* Background Decoration */}
            <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -mr-16 -mt-16 blur-2xl group-hover:bg-primary/10 transition-colors duration-700" />

            {/* Header: Topic (Left) & Status (Right) */}
            <div className="flex justify-between items-center mb-10 relative z-10">
                <div className="flex-1 min-w-0">
                    <span className={cn(
                        "text-[10px] font-black uppercase tracking-[0.2em] border-l-4 pl-4 py-0.5 block truncate",
                        getDifficultyColor(study.difficulty)
                    )}>
                        {study.topic}
                    </span>
                </div>
                <div className="flex items-center gap-3 ml-4">
                    <span className={cn(
                        "px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider shadow-sm border whitespace-nowrap",
                        statusConfig.className
                    )}>
                        {statusConfig.text}
                    </span>
                    <button
                        className={cn(
                            "p-2 rounded-full transition-all hover:bg-error/5 group/bookmark",
                            study.isBookmarked ? "text-error" : "text-text-muted hover:text-error"
                        )}
                        onClick={(e) => {
                            e.stopPropagation();
                            onBookmarkToggle?.(study.id);
                        }}
                    >
                        <Heart
                            size={20}
                            fill={study.isBookmarked ? 'currentColor' : 'none'}
                            className="group-hover/bookmark:scale-110 transition-transform"
                        />
                    </button>
                </div>
            </div>

            {/* Title & Description Container */}
            <div className="flex-1 flex flex-col min-w-0 relative z-10">
                <h3 className="text-[26px] font-black text-text-primary mb-3 leading-tight tracking-tight group-hover:text-primary transition-colors line-clamp-2 h-[64px]">
                    {study.name}
                </h3>

                <p className="text-[14px] text-text-secondary leading-relaxed line-clamp-2 font-medium opacity-70 mb-8 h-[44px]">
                    {study.description}
                </p>

                {/* Info Items - Fixed spacing above footer */}
                <div className="grid grid-cols-2 gap-y-4 gap-x-6">
                    <div className="flex items-center gap-3 text-xs font-bold text-text-tertiary">
                        <div className="p-1.5 bg-primary/5 rounded-lg text-primary/70">
                            <Users size={16} />
                        </div>
                        <span className="text-text-secondary">{getMeetingTypeText(study.meetingType)} · {study.currentMembers}/{study.maxMembers}명</span>
                    </div>
                    <div className="flex items-center gap-3 text-xs font-bold text-text-tertiary overflow-hidden">
                        <div className="p-1.5 bg-primary/5 rounded-lg text-primary/70">
                            <Calendar size={16} />
                        </div>
                        <span className="text-text-secondary truncate">{formatDays(study.scheduleDays)}</span>
                    </div>
                    <div className="flex items-center gap-3 text-xs font-bold text-text-tertiary overflow-hidden">
                        <div className="p-1.5 bg-primary/5 rounded-lg text-primary/70">
                            <MapPin size={16} />
                        </div>
                        <span className="text-text-secondary truncate">{getRegionText()}</span>
                    </div>
                    <div className="flex items-center gap-3 text-xs font-bold text-text-tertiary">
                        <div className="p-1.5 bg-primary/5 rounded-lg text-primary/70">
                            <Clock size={16} />
                        </div>
                        <span className="text-text-secondary">{study.scheduleTime ? study.scheduleTime.substring(0, 5) : '시간미정'}</span>
                    </div>
                </div>
            </div>

            {/* Leader Info - Elevated Footer */}
            <div className="flex items-center justify-between pt-8 border-t border-border-light/40 mt-auto relative z-10">
                <div className="flex items-center gap-4">
                    <div className="relative group/avatar">
                        <div className="absolute inset-0 bg-primary/20 rounded-2xl blur-md group-hover/avatar:blur-lg transition-all opacity-0 group-hover/avatar:opacity-100" />
                        <img
                            src={getProfileImageUrl(study.leader.profileImage)}
                            alt={study.leader.nickname}
                            className="w-10 h-10 rounded-2xl border-2 border-white shadow-sm object-cover relative z-10"
                        />
                        {study.status === 'RECRUITING' && (
                            <span className="absolute -bottom-1 -right-1 w-3 h-3 bg-success border-2 border-white rounded-full z-20 shadow-sm" />
                        )}
                    </div>
                    <span className="text-[15px] font-black text-text-primary tracking-tight truncate max-w-[120px]">
                        {study.leader.nickname}
                    </span>
                </div>

                <div className="flex items-center gap-2 px-3 py-1.5 bg-background-secondary/40 rounded-xl border border-border-light/40 shadow-sm backdrop-blur-sm">
                    <Star size={14} className="text-yellow-400 fill-current" />
                    <span className="text-[12px] font-black text-text-primary">{study.leader.leaderRating.toFixed(1)}</span>
                    <span className="text-[10px] font-bold text-text-tertiary opacity-60">({study.leader.leaderReviewCount})</span>
                </div>
            </div>
        </div>
    );
};

export default StudyCardContent;
