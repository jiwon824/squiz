import React, { useEffect, useState } from 'react';
import { MeetingRequestPayload, MeetingType } from '../types';
import '../styles/MeetingShared.css';

interface MeetingStartModalProps {
    open: boolean;
    initialTitle?: string;
    onClose: () => void;
    onStart: (payload: MeetingRequestPayload) => void;
}

const MeetingStartModal: React.FC<MeetingStartModalProps> = ({ open, initialTitle, onClose, onStart }) => {
    const [title, setTitle] = useState(initialTitle ?? '');
    const meetingType: MeetingType = 'DAILY';
    const autoShareSummary = false;
    const shareWorkspaceId = null;

    useEffect(() => {
        if (open) {
            setTitle(initialTitle ?? '');
        }
    }, [open, initialTitle]);

    if (!open) return null;

    const handleSubmit = () => {
        onStart({
            title: title.trim() || '새 미팅',
            meetingType,
            autoShareSummary,
            shareWorkspaceId,
        });
    };

    return (
        <div className="meeting-modal-backdrop" role="dialog" aria-modal="true">
            <div className="meeting-modal">
                <div className="meeting-modal__header">
                    <h2>미팅 시작</h2>
                    <button className="meeting-modal__close" onClick={onClose} aria-label="닫기">
                        ×
                    </button>
                </div>
                <div className="meeting-modal__body">
                    <label className="meeting-modal__label">
                        미팅 제목
                        <input
                            type="text"
                            className="meeting-modal__input"
                            value={title}
                            onChange={(event) => setTitle(event.target.value)}
                            placeholder="미팅 제목을 입력하세요"
                        />
                    </label>

                </div>
                <div className="meeting-modal__footer">
                    <button className="meeting-btn ghost" onClick={onClose} type="button">
                        취소
                    </button>
                    <button className="meeting-btn primary" onClick={handleSubmit} type="button">
                        시작하기
                    </button>
                </div>
            </div>
        </div>
    );
};

export default MeetingStartModal;
