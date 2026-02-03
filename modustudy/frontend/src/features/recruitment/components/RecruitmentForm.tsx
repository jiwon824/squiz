import React, { useMemo, useState } from 'react';
import { Save, Users, Tag } from 'lucide-react';
import { Button, ArrowButton } from '@/shared/components';
import { cn } from '@/shared/utils/cn';
import {
    RecruitmentPostDetail,
    RecruitmentStudy,
    createRecruitmentPost,
    updateRecruitmentPost,
} from '@/api/endpoints/boardApi';

interface RecruitmentFormProps {
    initialData?: RecruitmentPostDetail | null;
    studies: RecruitmentStudy[];
    onCancel: () => void;
    onSuccess: () => void;
}

export const RecruitmentForm: React.FC<RecruitmentFormProps> = ({ initialData, studies, onCancel, onSuccess }) => {
    const initialStudyId = useMemo(() => {
        if (initialData?.studyId) return initialData.studyId;
        return studies[0]?.id ?? null;
    }, [initialData?.studyId, studies]);

    const [selectedStudyId, setSelectedStudyId] = useState<number | null>(initialStudyId);
    const [title, setTitle] = useState(initialData?.title || '');
    const [content, setContent] = useState(initialData?.content || '');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const selectedStudy = studies.find((study) => study.id === selectedStudyId) || null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedStudyId) return;
        setIsSubmitting(true);
        try {
            if (initialData) {
                await updateRecruitmentPost(initialData.id, { title, content });
            } else {
                await createRecruitmentPost({ studyId: selectedStudyId, title, content });
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
                    <p className="text-text-secondary font-medium">모집 중인 스터디를 선택하고 내용을 작성해주세요.</p>
                </div>
            </header>

            <form onSubmit={handleSubmit} className="bg-white border border-border-light rounded-[32px] p-10 shadow-sm space-y-8">
                {/* 스터디 선택 */}
                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">모집 중인 스터디 선택</label>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {studies.map((study) => {
                            const isSelected = selectedStudyId === study.id;
                            return (
                                <button
                                    key={study.id}
                                    type="button"
                                    onClick={() => setSelectedStudyId(study.id)}
                                    className={cn(
                                        "text-left border-2 rounded-2xl p-5 transition-all",
                                        isSelected
                                            ? "border-primary bg-primary/5 shadow-md"
                                            : "border-transparent bg-background-secondary/60 hover:bg-background-secondary"
                                    )}
                                >
                                    <div className="flex items-start justify-between gap-3">
                                        <div>
                                            <p className="text-sm font-bold text-text-primary">{study.name}</p>
                                            <p className="text-xs text-text-secondary mt-1">#{study.topicName || '기타'}</p>
                                        </div>
                                        <span className="text-xs font-semibold text-text-tertiary">{study.meetingType}</span>
                                    </div>
                                    <div className="flex items-center gap-2 mt-4 text-xs text-text-tertiary">
                                        <Users size={14} />
                                        <span>{study.currentMembers}/{study.maxMembers ?? '-'}</span>
                                    </div>
                                </button>
                            );
                        })}
                    </div>
                </div>

                {/* 선택한 스터디 요약 */}
                {selectedStudy && (
                    <div className="flex items-center gap-3 bg-background-secondary/60 border border-border-light rounded-2xl px-5 py-4 text-sm">
                        <Tag size={16} className="text-primary" />
                        <span className="font-semibold text-text-primary">{selectedStudy.name}</span>
                        <span className="text-text-tertiary">#{selectedStudy.topicName || '기타'}</span>
                    </div>
                )}

                {/* 제목 */}
                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">제목</label>
                    <input
                        required
                        type="text"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="매력적인 제목으로 팀원을 끌어보세요"
                        className="w-full bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-6 py-4 text-lg font-bold outline-none transition-all"
                    />
                </div>

                {/* 내용 */}
                <div className="space-y-3">
                    <label className="text-sm font-black text-text-tertiary uppercase tracking-widest">모집 상세 내용</label>
                    <textarea
                        required
                        rows={8}
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        placeholder="스터디 진행 방식, 준비물, 예상 일정 등을 상세히 적어주세요."
                        className="w-full bg-background-secondary/50 border border-transparent focus:border-primary/30 focus:bg-white rounded-2xl px-6 py-4 outline-none transition-all resize-none leading-relaxed"
                    />
                </div>

                {/* 제출 버튼 */}
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
                        disabled={isSubmitting || !selectedStudyId}
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
