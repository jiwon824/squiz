import React, { useState } from 'react';
import { X, Send, CheckCircle, AlertCircle, Loader2 } from 'lucide-react';
import { studyService, Study } from '../services/studyService';

interface StudyApplyModalProps {
    study: Study;
    onClose: () => void;
}

const StudyApplyModal: React.FC<StudyApplyModalProps> = ({ study, onClose }) => {
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
        <div className="modal-overlay" onClick={onClose}>
            <div className="apply-modal-content animate-pop" onClick={e => e.stopPropagation()}>
                <header className="modal-header">
                    <h3>스터디 참여 신청</h3>
                    <button className="btn-close" onClick={onClose}>
                        <X size={20} />
                    </button>
                </header>

                {status === 'idle' && (
                    <form className="apply-form" onSubmit={handleSubmit}>
                        <div className="study-info-brief">
                            <span className="label">신청 스터디</span>
                            <span className="value">{study.name}</span>
                        </div>

                        <div className="input-group">
                            <label htmlFor="apply-message">스터디장에게 한마디</label>
                            <textarea
                                id="apply-message"
                                placeholder="자기소개나 참여 동기를 자유롭게 작성해 주세요!"
                                value={message}
                                onChange={(e) => setMessage(e.target.value)}
                                autoFocus
                                required
                            />
                            <p className="helper-text">신청 메시지는 스터디장이 승인 여부를 결정할 때 참고하게 됩니다.</p>
                        </div>

                        <div className="modal-actions">
                            <button type="button" className="btn-secondary" onClick={onClose}>
                                취소
                            </button>
                            <button type="submit" className="btn-primary">
                                <Send size={18} />
                                <span>신청하기</span>
                            </button>
                        </div>
                    </form>
                )}

                {status === 'loading' && (
                    <div className="status-container loading">
                        <Loader2 className="spinner" size={48} />
                        <p>신청 정보를 전송하고 있습니다...</p>
                    </div>
                )}

                {status === 'success' && (
                    <div className="status-container success">
                        <CheckCircle size={64} className="icon" />
                        <h2>Success!</h2>
                        <p>{statusMessage}</p>
                        <button className="btn-confirm" onClick={onClose}>
                            확인
                        </button>
                    </div>
                )}

                {status === 'error' && (
                    <div className="status-container error">
                        <AlertCircle size={64} className="icon" />
                        <h2>Oh no!</h2>
                        <p>{statusMessage}</p>
                        <div className="error-actions">
                            <button className="btn-secondary" onClick={() => setStatus('idle')}>
                                다시 시도
                            </button>
                            <button className="btn-confirm" onClick={onClose}>
                                닫기
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default StudyApplyModal;
