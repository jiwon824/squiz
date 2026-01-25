import React from 'react';
import { Heart, Users, MapPin, Calendar, Clock, Star } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

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

    // 요일 포맷팅
    const formatDays = (days: string) => {
        const dayMap: { [key: string]: string } = {
            MON: '월', TUE: '화', WED: '수', THU: '목', FRI: '금', SAT: '토', SUN: '일',
        };
        return days
            .split(',')
            .map((day) => dayMap[day.trim()] || day)
            .join(', ');
    };

    const statusConfig = getStatusConfig(study.status);

    return (
        <div
            onClick={() => onClick?.(study.id)}
            className={cn(
                "group relative bg-white border border-border-light rounded-[40px] px-8 py-10 transition-all duration-700 hover:shadow-[0_30px_70px_rgba(0,0,0,0.08)] hover:-translate-y-3 cursor-pointer overflow-hidden flex flex-col min-h-[480px] h-fit",
                study.status === 'COMPLETED' && "opacity-60 grayscale-[0.6]"
            )}
        >
            {/* Background Decoration */}
            <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -mr-16 -mt-16 blur-2xl group-hover:bg-primary/10 transition-colors duration-700" />

            {/* Header: Badges & Bookmark */}
            <div className="flex justify-between items-start mb-10 relative z-10">
                <div className="flex flex-wrap gap-2.5">
                    <span className={cn(
                        "px-3.5 py-1 rounded-full text-[10px] font-black uppercase tracking-widest border shadow-sm",
                        statusConfig.className
                    )}>
                        {statusConfig.text}
                    </span>
                    <span className="px-3.5 py-1 rounded-full text-[10px] font-black uppercase tracking-widest border bg-white text-text-secondary border-border-light shadow-sm">
                        {getMeetingTypeText(study.meetingType)}
                    </span>
                </div>
                <button
                    className={cn(
                        "p-2.5 rounded-full transition-all hover:bg-error/5 group/bookmark",
                        study.isBookmarked ? "text-error" : "text-text-muted hover:text-error"
                    )}
                    onClick={(e) => {
                        e.stopPropagation();
                        onBookmarkToggle?.(study.id);
                    }}
                >
                    <Heart
                        size={22}
                        fill={study.isBookmarked ? 'currentColor' : 'none'}
                        className="group-hover/bookmark:scale-110 transition-transform"
                    />
                </button>
            </div>

            {/* Title & Description Container */}
            <div className="flex-1 flex flex-col min-w-0 relative z-10">
                <div className={cn(
                    "inline-flex self-start px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-[0.1em] border-l-4 mb-6 shadow-sm bg-background-secondary/30",
                    getDifficultyColor(study.difficulty)
                )}>
                    {study.topic}
                </div>

                <h3 className="text-[28px] font-black text-text-primary mb-4 leading-[1.25] tracking-tight group-hover:text-primary transition-colors min-h-[70px] h-fit line-clamp-2">
                    {study.name}
                </h3>

                <p className="text-[15px] text-text-secondary leading-relaxed line-clamp-2 font-medium opacity-70 mb-8 min-h-[45px] h-fit">
                    {study.description}
                </p>

                {/* Info Items - Enhanced Spacing */}
                <div className="grid grid-cols-2 gap-y-5 gap-x-6 mt-auto">
                    <div className="flex items-center gap-3 text-xs font-bold text-text-tertiary">
                        <div className="p-1.5 bg-primary/5 rounded-lg text-primary">
                            <Users size={16} />
                        </div>
                        <span className="text-text-secondary">{study.currentMembers}/{study.maxMembers}명</span>
                    </div>
                    <div className="flex items-center gap-3 text-xs font-bold text-text-tertiary overflow-hidden">
                        <div className="p-1.5 bg-primary/5 rounded-lg text-primary">
                            <Calendar size={16} />
                        </div>
                        <span className="text-text-secondary truncate">{formatDays(study.scheduleDays)}</span>
                    </div>
                    {study.region && (
                        <div className="flex items-center gap-3 text-xs font-bold text-text-tertiary overflow-hidden">
                            <div className="p-1.5 bg-primary/5 rounded-lg text-primary">
                                <MapPin size={16} />
                            </div>
                            <span className="text-text-secondary truncate">{study.region.name}</span>
                        </div>
                    )}
                    {study.scheduleTime && (
                        <div className="flex items-center gap-3 text-xs font-bold text-text-tertiary">
                            <div className="p-1.5 bg-primary/5 rounded-lg text-primary">
                                <Clock size={16} />
                            </div>
                            <span className="text-text-secondary">{study.scheduleTime.substring(0, 5)}</span>
                        </div>
                    )}
                </div>
            </div>

            {/* Leader Info - Elevated Footer */}
            <div className="flex items-center justify-between pt-8 border-t border-border-light/40 mt-10 relative z-10">
                <div className="flex items-center gap-4">
                    <div className="relative group/avatar">
                        <div className="absolute inset-0 bg-primary/20 rounded-2xl blur-md group-hover/avatar:blur-lg transition-all opacity-0 group-hover/avatar:opacity-100" />
                        {study.leader.profileImage ? (
                            <img
                                src={study.leader.profileImage}
                                alt={study.leader.nickname}
                                className="w-10 h-10 rounded-2xl border-2 border-white shadow-sm object-cover relative z-10"
                            />
                        ) : (
                            <div className="w-10 h-10 rounded-2xl bg-primary text-white flex items-center justify-center text-base font-black border-2 border-white shadow-sm relative z-10">
                                {study.leader.nickname.charAt(0)}
                            </div>
                        )}
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
