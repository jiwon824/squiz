import React, { useState } from 'react';
import { RecruitmentPost } from '../types';
import { useRecruitmentStore } from '../useRecruitmentStore';
import { Save, Hash } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Button, ArrowButton } from '@/shared/components';

interface RecruitmentFormProps {
    initialData?: RecruitmentPost | null;
    onCancel: () => void;
    onSuccess: () => void;
}

export const RecruitmentForm: React.FC<RecruitmentFormProps> = ({ initialData, onCancel, onSuccess }) => {
    const { addPost, updatePost } = useRecruitmentStore();
    const [formData, setFormData] = useState({
        title: initialData?.title || '',
        content: initialData?.content || '',
        category: initialData?.category || 'study',
        maxMembers: initialData?.maxMembers || 4,
        tagsInput: initialData?.tags.join(', ') || '',
    });

    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setIsSubmitting(true);

        const tags = formData.tagsInput.split(',').map(tag => tag.trim()).filter(Boolean);

        if (initialData) {
            updatePost(initialData.id, {
                title: formData.title,
                content: formData.content,
                category: formData.category as any,
                maxMembers: formData.maxMembers,
                tags,
            });
        } else {
            addPost({
                title: formData.title,
                content: formData.content,
                authorId: 'me', // Mock user
                authorName: '내 계정',
                authorAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=me',
                category: formData.category as any,
                tags,
                isCompleted: false,
                memberCount: 1,
                maxMembers: formData.maxMembers,
            });
        }

        setIsSubmitting(false);
        onSuccess();
    };

    return (
        <div className="max-w-4xl mx-auto animate-slideInUp">
            <header className="flex items-center gap-6 mb-10">
                <ArrowButton
                    direction="left"
                    onClick={onCancel}
                    size="md"
                />
                <div>
                    <h2 className="text-3xl font-black text-text-primary tracking-tight">
                        {initialData ? '모집글 수정' : '새로운 팀원 모집'}
                    </h2>
                    <p className="text-text-secondary font-medium">모든 필드를 정확하게 입력해주세요.</p>
                </div>
            </header>

            <form onSubmit={handleSubmit} className="bg-white border border-border-light rounded-[32px] p-10 shadow-sm space-y-8">
                {/* Category Selection */}
                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">카테고리</label>
                    <div className="flex gap-4">
                        {['study', 'project', 'mentoring'].map((cat) => (
                            <button
                                key={cat}
                                type="button"
                                onClick={() => setFormData({ ...formData, category: cat as any })}
                                className={cn(
                                    "flex-1 px-4 py-3 rounded-2xl text-sm font-bold border transition-all",
                                    formData.category === cat
                                        ? "bg-primary/5 border-primary text-primary shadow-sm"
                                        : "bg-background-secondary/50 border-transparent text-text-secondary hover:border-border-light"
                                )}
                            >
                                {cat.toUpperCase()}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Title */}
                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">제목</label>
                    <input
                        required
                        type="text"
                        value={formData.title}
                        onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                        placeholder="매력적인 제목으로 팀원을 끌어보세요"
                        className="w-full bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-6 py-4 text-lg font-bold outline-none transition-all"
                    />
                </div>

                {/* Member Count */}
                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">모집 인원 (최대)</label>
                    <div className="flex items-center gap-4">
                        <input
                            type="range"
                            min="2"
                            max="20"
                            value={formData.maxMembers}
                            onChange={(e) => setFormData({ ...formData, maxMembers: parseInt(e.target.value) })}
                            className="flex-1 accent-primary h-2 bg-background-secondary rounded-lg appearance-none cursor-pointer"
                        />
                        <span className="w-12 h-12 flex items-center justify-center bg-primary/10 text-primary font-black rounded-xl">
                            {formData.maxMembers}
                        </span>
                    </div>
                </div>

                {/* Tags */}
                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">기술 스택 / 태그</label>
                    <div className="relative">
                        <input
                            type="text"
                            value={formData.tagsInput}
                            onChange={(e) => setFormData({ ...formData, tagsInput: e.target.value })}
                            placeholder="React, NestJS, 신입환영 (콤마로 구분)"
                            className="w-full bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-6 py-4 outline-none transition-all pr-12"
                        />
                        <Hash className="absolute right-4 top-1/2 -translate-y-1/2 text-text-muted" size={18} />
                    </div>
                </div>

                {/* Content */}
                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">모집 상세 내용</label>
                    <textarea
                        required
                        rows={8}
                        value={formData.content}
                        onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                        placeholder="스터디 진행 방식, 준비물, 예상 일정 등을 상세히 적어주세요."
                        className="w-full bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-6 py-4 outline-none transition-all resize-none leading-relaxed"
                    />
                </div>

                {/* Submit Buttons */}
                <div className="flex gap-4 pt-6">
                    <Button
                        type="button"
                        onClick={onCancel}
                        variant="google-outline"
                        size="xl"
                        fullWidth
                        className="bg-background-secondary border-transparent py-8"
                    >
                        작성 취소
                    </Button>
                    <Button
                        type="submit"
                        disabled={isSubmitting}
                        variant="primary"
                        size="xl"
                        fullWidth
                        leftIcon={<Save size={20} />}
                        className="py-8"
                    >
                        {initialData ? '수정 사항 저장' : '모집글 등록하기'}
                    </Button>
                </div>
            </form>
        </div>
    );
};
