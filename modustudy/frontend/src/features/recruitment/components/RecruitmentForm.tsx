import React, { useMemo, useState } from 'react';
import { Save, Users, Tag } from 'lucide-react';
import { Button, ArrowButton } from '@/shared/components';
import {
    RecruitmentPostDetail,
    createRecruitmentPost,
    updateRecruitmentPost,
} from '@/api/endpoints/boardApi';

interface RecruitmentFormProps {
    initialData?: RecruitmentPostDetail | null;
    onCancel: () => void;
    onSuccess: () => void;
}

const meetingTypeOptions = [
    { value: 'ONLINE', label: '온라인' },
    { value: 'OFFLINE', label: '오프라인' },
    { value: 'HYBRID', label: '온·오프라인' },
];

export const RecruitmentForm: React.FC<RecruitmentFormProps> = ({ initialData, onCancel, onSuccess }) => {
    const [title, setTitle] = useState(initialData?.title || '');
    const [content, setContent] = useState(initialData?.content || '');
    const [recruitmentField, setRecruitmentField] = useState(initialData?.recruitmentField || '');
    const [meetingType, setMeetingType] = useState(initialData?.meetingType || 'ONLINE');
    const [targetMembers, setTargetMembers] = useState<number>(initialData?.targetMembers ?? 1);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const recruitmentStatus = useMemo(
        () => initialData?.recruitmentStatus || 'RECRUITING',
        [initialData?.recruitmentStatus]
    );

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim() || !content.trim() || !recruitmentField.trim() || !targetMembers) return;
        setIsSubmitting(true);
        try {
            if (initialData) {
                await updateRecruitmentPost(initialData.id, {
                    title,
                    content,
                    recruitmentField,
                    meetingType,
                    targetMembers,
                    recruitmentStatus,
                });
            } else {
                await createRecruitmentPost({
                    title,
                    content,
                    recruitmentField,
                    meetingType,
                    targetMembers,
                });
            }
            onSuccess();
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="max-w-4xl mx-auto animate-slideInUp">
            <header className="flex items-center gap-6 mb-10">
                <ArrowButton direction="left" onClick={onCancel} size="md" />
                <div>
                    <h2 className="text-3xl font-black text-text-primary tracking-tight">
                        {initialData ? '모집글 수정' : '새로운 팀원 모집'}
                    </h2>
                    <p className="text-text-secondary font-medium">
                        자유게시판 형식으로 모집 정보를 작성해 주세요.
                    </p>
                </div>
            </header>

            <form onSubmit={handleSubmit} className="bg-white border border-border-light rounded-[32px] p-10 shadow-sm space-y-8">
                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">모집 정보</label>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div className="space-y-2">
                            <div className="flex items-center gap-2 text-xs font-semibold text-text-tertiary uppercase tracking-widest">
                                <Tag size={14} className="text-primary" />
                                모집 분야
                            </div>
                            <input
                                required
                                type="text"
                                value={recruitmentField}
                                onChange={(e) => setRecruitmentField(e.target.value)}
                                placeholder="예: 백엔드, 프론트, 기획"
                                className="w-full bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-4 py-3 text-sm font-semibold outline-none transition-all"
                            />
                        </div>
                        <div className="space-y-2">
                            <div className="flex items-center gap-2 text-xs font-semibold text-text-tertiary uppercase tracking-widest">
                                <Users size={14} className="text-primary" />
                                목표 인원
                            </div>
                            <input
                                required
                                type="number"
                                min={1}
                                value={targetMembers}
                                onChange={(e) => setTargetMembers(Number(e.target.value))}
                                className="w-full bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-4 py-3 text-sm font-semibold outline-none transition-all"
                            />
                        </div>
                        <div className="space-y-2">
                            <div className="flex items-center gap-2 text-xs font-semibold text-text-tertiary uppercase tracking-widest">
                                진행 방식
                            </div>
                            <select
                                value={meetingType}
                                onChange={(e) => setMeetingType(e.target.value)}
                                className="w-full bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-4 py-3 text-sm font-semibold outline-none transition-all"
                            >
                                {meetingTypeOptions.map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                </div>

                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">제목</label>
                    <input
                        required
                        type="text"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="매력적인 제목으로 지원자를 모아보세요."
                        className="w-full bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-6 py-4 text-lg font-bold outline-none transition-all"
                    />
                </div>

                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">상세 내용</label>
                    <textarea
                        required
                        rows={8}
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        placeholder="진행 방식, 준비물, 일정 등을 자세히 적어주세요."
                        className="w-full bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-6 py-4 outline-none transition-all resize-none leading-relaxed"
                    />
                </div>

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
