import React, { useState } from 'react';
import { Send, CheckCircle, AlertCircle, Loader2 } from 'lucide-react';
import { studyService, Study } from '../services/studyService';
import { Modal, Button } from '@/shared/components';

interface StudyApplyModalProps {
    study: Study;
    isOpen: boolean;
    onClose: () => void;
}

const StudyApplyModal: React.FC<StudyApplyModalProps> = ({ study, isOpen, onClose }) => {
    const [message, setMessage] = useState('');
    const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
    const [statusMessage, setStatusMessage] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!message.trim()) return;

        setStatus('loading');

        try {
            const result = await studyService.applyToStudy(study.id, message);
            if (result.success) {
                setStatus('success');
                setStatusMessage(result.message);
            } else {
                setStatus('error');
                setStatusMessage(result.message);
            }
        } catch (error) {
            setStatus('error');
            setStatusMessage('신청 처리 중 예상치 못한 오류가 발생했습니다.');
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} title="스터디 참여 신청" maxWidth="md">
            <div className="space-y-6">
                {status === 'idle' && (
                    <form className="space-y-8" onSubmit={handleSubmit}>
                        <div className="bg-background-secondary/50 p-6 rounded-2xl border border-border-light/40">
                            <span className="text-[10px] font-black text-text-tertiary uppercase tracking-[0.2em] block mb-2">신청 스터디</span>
                            <span className="text-lg font-black text-text-primary tracking-tight">{study.name}</span>
                        </div>

                        <div className="space-y-3">
                            <label htmlFor="apply-message" className="text-sm font-bold text-text-secondary">스터디장에게 한마디</label>
                            <textarea
                                id="apply-message"
                                className="w-full h-40 p-5 rounded-2xl border border-border-light bg-white focus:border-primary focus:ring-4 focus:ring-primary/5 transition-all outline-none resize-none text-base font-medium leading-relaxed"
                                placeholder="자기소개나 참여 동기를 자유롭게 작성해 주세요!"
                                value={message}
                                onChange={(e) => setMessage(e.target.value)}
                                autoFocus
                                required
                            />
                            <p className="text-xs font-medium text-text-tertiary opacity-70">신청 메시지는 스터디장이 승인 여부를 결정할 때 참고하게 됩니다.</p>
                        </div>

                        <div className="flex gap-3 pt-2">
                            <Button type="button" variant="google-outline" fullWidth onClick={onClose} className="h-14 font-black">
                                취소
                            </Button>
                            <Button 
                                type="submit" 
                                variant="primary" 
                                fullWidth 
                                className="h-14 font-black shadow-lg shadow-primary/20"
                                leftIcon={<Send size={18} />}
                            >
                                <span>신청하기</span>
                            </Button>
                        </div>
                    </form>
                )}

                {status === 'loading' && (
                    <div className="py-20 flex flex-col items-center justify-center gap-6">
                        <Loader2 className="w-12 h-12 text-primary animate-spin" />
                        <p className="text-lg font-bold text-text-secondary">신청 정보를 전송하고 있습니다...</p>
                    </div>
                )}

                {status === 'success' && (
                    <div className="py-10 flex flex-col items-center justify-center text-center">
                        <div className="w-20 h-20 bg-success/10 rounded-full flex items-center justify-center mb-8">
                            <CheckCircle size={40} className="text-success" />
                        </div>
                        <h2 className="text-2xl font-black text-text-primary mb-4">신청 완료!</h2>
                        <p className="text-text-secondary font-medium mb-10 leading-relaxed px-6">{statusMessage}</p>
                        <Button variant="primary" fullWidth size="lg" onClick={onClose} className="h-14 font-black max-w-[200px]">
                            확인
                        </Button>
                    </div>
                )}

                {status === 'error' && (
                    <div className="py-10 flex flex-col items-center justify-center text-center">
                        <div className="w-20 h-20 bg-error/10 rounded-full flex items-center justify-center mb-8">
                            <AlertCircle size={40} className="text-error" />
                        </div>
                        <h2 className="text-2xl font-black text-text-primary mb-4">신청 실패</h2>
                        <p className="text-text-secondary font-medium mb-10 leading-relaxed px-6">{statusMessage}</p>
                        <div className="flex gap-3 w-full max-w-[320px]">
                            <Button variant="google-outline" fullWidth onClick={() => setStatus('idle')} className="h-14 font-black">
                                다시 시도
                            </Button>
                            <Button variant="primary" fullWidth onClick={onClose} className="h-14 font-black">
                                닫기
                            </Button>
                        </div>
                    </div>
                )}
            </div>
        </Modal>
    );
};

export default StudyApplyModal;
