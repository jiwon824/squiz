import React, { useState } from 'react';
import { Send, CheckCircle, AlertCircle, Loader2, Users, Calendar, MapPin } from 'lucide-react';
import { Study } from '../services/studyService';
import { studyApi } from '@/api/endpoints/studyApi';
import { Modal, Button, FormField } from '@/shared/components';
import { cn } from '@/shared/utils/cn';

interface StudyApplyModalV2Props {
    study: Study;
    isOpen: boolean;
    onClose: () => void;
}

/**
 * StudyApplyModalV2 - Google Material Design 스타일 스터디 신청 모달
 *
 * 특징:
 * - 스터디 정보 요약 카드
 * - FormField 컴포넌트를 활용한 입력 필드
 * - 상태별 UI (idle, loading, success, error)
 * - 깔끔한 레이아웃
 */
const StudyApplyModalV2: React.FC<StudyApplyModalV2Props> = ({ study, isOpen, onClose }) => {
    const [message, setMessage] = useState('');
    const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
    const [statusMessage, setStatusMessage] = useState('');
    const [messageError, setMessageError] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // 유효성 검사
        if (!message.trim()) {
            setMessageError('신청 메시지를 입력해주세요.');
            return;
        }

        if (message.trim().length < 10) {
            setMessageError('10자 이상 입력해주세요.');
            return;
        }

        setMessageError('');
        setStatus('loading');

        try {
            const response = await studyApi.applyToStudy(study.id, message);
            console.log('[StudyApplyModalV2] 신청 응답:', response);
            setStatus('success');
            setStatusMessage('스터디 신청이 완료되었습니다! 스터디장의 승인을 기다려주세요.');
        } catch (error: any) {
            console.error('[StudyApplyModalV2] 신청 실패:', error);
            setStatus('error');
            const errorMessage = error?.response?.data?.message || error?.message || '신청 처리 중 예상치 못한 오류가 발생했습니다.';
            setStatusMessage(errorMessage);
        }
    };

    const handleClose = () => {
        // 상태 초기화
        setMessage('');
        setStatus('idle');
        setStatusMessage('');
        setMessageError('');
        onClose();
    };

    const getMeetingTypeText = (meetingType: string) => {
        switch (meetingType) {
            case 'ONLINE': return '온라인';
            case 'OFFLINE': return '오프라인';
            case 'HYBRID': return '혼합';
            default: return meetingType;
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={handleClose} title="스터디 참여 신청" maxWidth="md">
            <div className="space-y-6">
                {status === 'idle' && (
                    <form className="space-y-6" onSubmit={handleSubmit}>
                        {/* 스터디 정보 요약 카드 */}
                        <div className="bg-[var(--color-background)] rounded-2xl border border-[var(--color-border-lighter)] p-5">
                            <h3 className="text-lg font-bold text-[var(--color-text-primary)] mb-4 line-clamp-2">
                                {study.name}
                            </h3>

                            <div className="grid grid-cols-3 gap-4">
                                <div className="flex items-center gap-2 text-sm">
                                    <div className="p-1.5 bg-[var(--color-primary-alpha-10)] rounded-lg">
                                        <Users size={14} className="text-[var(--color-primary)]" />
                                    </div>
                                    <span className="text-[var(--color-text-secondary)]">
                                        {study.currentMembers}/{study.maxMembers}명
                                    </span>
                                </div>

                                <div className="flex items-center gap-2 text-sm">
                                    <div className="p-1.5 bg-[var(--color-primary-alpha-10)] rounded-lg">
                                        <MapPin size={14} className="text-[var(--color-primary)]" />
                                    </div>
                                    <span className="text-[var(--color-text-secondary)]">
                                        {getMeetingTypeText(study.meetingType)}
                                    </span>
                                </div>

                                <div className="flex items-center gap-2 text-sm">
                                    <div className="p-1.5 bg-[var(--color-primary-alpha-10)] rounded-lg">
                                        <Calendar size={14} className="text-[var(--color-primary)]" />
                                    </div>
                                    <span className="text-[var(--color-text-secondary)] truncate">
                                        {study.scheduleDays}
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* 신청 메시지 입력 */}
                        <div>
                            <FormField
                                as="textarea"
                                label="스터디장에게 한마디"
                                placeholder="자기소개나 참여 동기를 자유롭게 작성해 주세요! (10자 이상)"
                                value={message}
                                onChange={(e) => {
                                    setMessage(e.target.value);
                                    if (messageError) setMessageError('');
                                }}
                                error={messageError}
                                rows={5}
                                required
                            />
                            <p className="text-xs text-[var(--color-text-tertiary)] mt-2 ml-1">
                                신청 메시지는 스터디장이 승인 여부를 결정할 때 참고하게 됩니다.
                            </p>
                        </div>

                        {/* 버튼 영역 */}
                        <div className="flex gap-3 pt-2">
                            <Button
                                type="button"
                                variant="google-outline"
                                fullWidth
                                onClick={handleClose}
                                className="h-12 rounded-xl font-semibold"
                            >
                                취소
                            </Button>
                            <Button
                                type="submit"
                                variant="primary"
                                fullWidth
                                className="h-12 rounded-xl font-bold shadow-lg shadow-[var(--color-primary-alpha-20)]"
                                leftIcon={<Send size={18} />}
                            >
                                신청하기
                            </Button>
                        </div>
                    </form>
                )}

                {status === 'loading' && (
                    <div className="py-16 flex flex-col items-center justify-center gap-4">
                        <div className="relative">
                            <div className="w-16 h-16 bg-[var(--color-primary-alpha-10)] rounded-full flex items-center justify-center">
                                <Loader2 className="w-8 h-8 text-[var(--color-primary)] animate-spin" />
                            </div>
                        </div>
                        <div className="text-center">
                            <p className="text-lg font-bold text-[var(--color-text-primary)] mb-1">
                                신청 중...
                            </p>
                            <p className="text-sm text-[var(--color-text-secondary)]">
                                잠시만 기다려주세요.
                            </p>
                        </div>
                    </div>
                )}

                {status === 'success' && (
                    <div className="py-10 flex flex-col items-center justify-center text-center">
                        <div className="w-20 h-20 bg-[var(--color-success-light)] rounded-full flex items-center justify-center mb-6">
                            <CheckCircle size={40} className="text-[var(--color-success)]" />
                        </div>
                        <h2 className="text-2xl font-bold text-[var(--color-text-primary)] mb-3">
                            신청 완료!
                        </h2>
                        <p className="text-[var(--color-text-secondary)] mb-8 leading-relaxed max-w-sm">
                            {statusMessage}
                        </p>
                        <Button
                            variant="primary"
                            size="lg"
                            onClick={handleClose}
                            className="h-12 rounded-xl font-bold min-w-[160px]"
                        >
                            확인
                        </Button>
                    </div>
                )}

                {status === 'error' && (
                    <div className="py-10 flex flex-col items-center justify-center text-center">
                        <div className="w-20 h-20 bg-[var(--color-error-light)] rounded-full flex items-center justify-center mb-6">
                            <AlertCircle size={40} className="text-[var(--color-error)]" />
                        </div>
                        <h2 className="text-2xl font-bold text-[var(--color-text-primary)] mb-3">
                            신청 실패
                        </h2>
                        <p className="text-[var(--color-text-secondary)] mb-8 leading-relaxed max-w-sm">
                            {statusMessage}
                        </p>
                        <div className="flex gap-3 w-full max-w-xs">
                            <Button
                                variant="google-outline"
                                fullWidth
                                onClick={() => setStatus('idle')}
                                className="h-12 rounded-xl font-semibold"
                            >
                                다시 시도
                            </Button>
                            <Button
                                variant="primary"
                                fullWidth
                                onClick={handleClose}
                                className="h-12 rounded-xl font-bold"
                            >
                                닫기
                            </Button>
                        </div>
                    </div>
                )}
            </div>
        </Modal>
    );
};

export default StudyApplyModalV2;
