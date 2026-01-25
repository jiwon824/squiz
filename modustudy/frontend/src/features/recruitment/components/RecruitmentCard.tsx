import React from 'react';
import { RecruitmentPost } from '../types';
import { Users, AlertCircle, CheckCircle2 } from 'lucide-react';
import { Button, FeatureCardLayout } from '@/shared/components';

interface RecruitmentCardProps {
    post: RecruitmentPost;
    onClick: (id: string) => void;
    onReport: (e: React.MouseEvent, id: string) => void;
}

export const RecruitmentCard: React.FC<RecruitmentCardProps> = ({ post, onClick, onReport }) => {
    return (
        <FeatureCardLayout
            onClick={() => onClick(post.id)}
            isCompleted={post.isCompleted}
            headerLeft={
                <span className="text-[10px] font-black uppercase tracking-[0.2em] text-primary/60 border-l-4 border-primary/30 pl-4 py-0.5">
                    {post.category}
                </span>
            }
            headerRight={
                <Button
                    variant="google-ghost"
                    size="sm"
                    isCircle
                    onClick={(e) => onReport(e, post.id)}
                    className="text-text-muted opacity-0 group-hover:opacity-100 transition-all hover:text-error hover:bg-error/5 hover:scale-110"
                >
                    <AlertCircle size={18} />
                </Button>
            }
            title={post.title}
            description={post.content}
            body={<>
                <div className="flex flex-wrap gap-2.5 mb-10">
                    {post.tags.slice(0, 3).map((tag, i) => (
                        <span key={i} className="px-3.5 py-1.5 bg-background-secondary/50 text-[11px] font-bold text-text-tertiary rounded-xl border border-border-light/50 shadow-sm backdrop-blur-sm">
                            #{tag}
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

            {/* Status Overlay for Completed Only (Subtle) */ }
    {
        post.isCompleted && (
            <div className="absolute top-6 right-6">
                <div className="flex items-center gap-1.5 px-3 py-1 bg-white/90 backdrop-blur-sm border border-border-light rounded-full shadow-sm">
                    <CheckCircle2 size={14} className="text-text-tertiary" />
                    <span className="text-[10px] font-bold text-text-tertiary">CLOSED</span>
                </div>
            </div>
        )
    }
            </>}
        />
    );
};
