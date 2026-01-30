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
    const [plannedDurationSeconds, setPlannedDurationSeconds] = useState(3600);
    const meetingType: MeetingType = 'DAILY';
    const autoShareSummary = false;
    const shareWorkspaceId = null;
    const durationOptions = [
        { label: '2분 (테스트)', value: 120 },
        { label: '6분 (테스트)', value: 360 },
        { label: '1시간', value: 3600 },
        { label: '1시간 30분', value: 5400 },
        { label: '2시간', value: 7200 },
        { label: '2시간 30분', value: 9000 },
        { label: '3시간', value: 10800 },
    ];

    useEffect(() => {
        if (open) {
            setTitle(initialTitle ?? '');
            setPlannedDurationSeconds(3600);
        }
    }, [open, initialTitle]);

    if (!open) return null;

    const handleSubmit = () => {
        onStart({
            title: title.trim() || '새 미팅',
            meetingType,
            autoShareSummary,
            shareWorkspaceId,
            plannedDurationSeconds,
        });
    };

    return (
        <div className="meeting-modal-backdrop" role="dialog" aria-modal="true">
            <div className="meeting-modal">
                <div className="meeting-modal__header">
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
                    <label className="meeting-modal__label">
                        미팅 시간
                        <select
                            className="meeting-modal__input"
                            value={plannedDurationSeconds}
                            onChange={(event) => setPlannedDurationSeconds(Number(event.target.value))}
                        >
                            {durationOptions.map((option) => (
                                <option key={option.value} value={option.value}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
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
