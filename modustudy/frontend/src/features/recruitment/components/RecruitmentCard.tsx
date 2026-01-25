import React from 'react';
import { RecruitmentPost } from '../types';
import { Users, AlertCircle, CheckCircle2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button } from '@/shared/components';

interface RecruitmentCardProps {
    post: RecruitmentPost;
    onClick: (id: string) => void;
    onReport: (e: React.MouseEvent, id: string) => void;
}

export const RecruitmentCard: React.FC<RecruitmentCardProps> = ({ post, onClick, onReport }) => {

    return (
        <div
            onClick={() => onClick(post.id)}
            className={cn(
                "group relative bg-white border border-border-light rounded-[32px] p-8 transition-all duration-500 hover:shadow-[0_20px_50px_rgba(0,0,0,0.06)] hover:-translate-y-2 cursor-pointer overflow-hidden flex flex-col h-full",
                post.isCompleted && "opacity-60 grayscale-[0.5]"
            )}
        >
            {/* Top Row: Category & Report */}
            <div className="flex justify-between items-center mb-8">
                <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary/60 border-l-2 border-primary/30 pl-3">
                    {post.category}
                </span>
                <Button
                    variant="google-ghost"
                    size="sm"
                    isCircle
                    onClick={(e) => onReport(e, post.id)}
                    className="text-text-muted opacity-0 group-hover:opacity-100 transition-opacity hover:text-error hover:bg-error/5"
                >
                    <AlertCircle size={16} />
                </Button>
            </div>

            {/* Title */}
            <h3 className="text-2xl font-black text-text-primary mb-6 leading-[1.3] tracking-tight group-hover:text-primary transition-colors line-clamp-2">
                {post.title}
            </h3>

            {/* Content Preview - Simplified */}
            <p className="text-[15px] text-text-secondary leading-relaxed mb-8 line-clamp-2 font-medium opacity-80">
                {post.content}
            </p>

            {/* Tags - Minimalist */}
            <div className="flex flex-wrap gap-2 mb-10 mt-auto">
                {post.tags.slice(0, 2).map((tag, i) => (
                    <span key={i} className="px-3 py-1 bg-background-secondary/50 text-[11px] font-bold text-text-tertiary rounded-full border border-border-light/50">
                        {tag}
                    </span>
                ))}
                {post.tags.length > 2 && (
                    <span className="text-[11px] font-bold text-text-muted self-center ml-1">
                        +{post.tags.length - 2}
                    </span>
                )}
            </div>

            {/* Bottom Row: Minimalist Author & Stats */}
            <div className="flex items-center justify-between pt-6 border-t border-border-light/40">
                <div className="flex items-center gap-3">
                    <div className="relative">
                        <img
                            src={post.authorAvatar}
                            alt=""
                            className="w-8 h-8 rounded-xl border border-border-light object-cover"
                        />
                        {!post.isCompleted && (
                            <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-success border-2 border-white rounded-full" />
                        )}
                    </div>
                    <span className="text-sm font-black text-text-primary tracking-tighter">{post.authorName}</span>
                </div>

                <div className="flex items-center gap-4 text-text-tertiary">
                    <div className="flex items-center gap-1.5 text-[11px] font-bold">
                        <Users size={14} className="text-text-muted" />
                        <span className="text-text-primary">{post.memberCount}</span>
                        <span className="opacity-40">/</span>
                        <span className="text-text-muted">{post.maxMembers}</span>
                    </div>
                </div>
            </div>

            {/* Status Overlay for Completed Only (Subtle) */}
            {post.isCompleted && (
                <div className="absolute top-6 right-6">
                    <div className="flex items-center gap-1.5 px-3 py-1 bg-white/90 backdrop-blur-sm border border-border-light rounded-full shadow-sm">
                        <CheckCircle2 size={14} className="text-text-tertiary" />
                        <span className="text-[10px] font-bold text-text-tertiary">CLOSED</span>
                    </div>
                </div>
            )}
        </div>
    );
};
