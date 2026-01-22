import React, { useEffect, useState } from 'react';
import { MeetingRequestPayload, MeetingType } from '../types';
import '../styles/MeetingShared.css';

const meetingTypeOptions: { label: string; value: MeetingType }[] = [
    { label: '데일리 스탠드업', value: 'DAILY' },
    { label: '주간 회고', value: 'WEEKLY' },
    { label: '자유 회의', value: 'FREE' },
    { label: '기타', value: 'OTHER' },
];

interface MeetingStartModalProps {
    open: boolean;
    initialTitle?: string;
    onClose: () => void;
    onStart: (payload: MeetingRequestPayload) => void;
}

const MeetingStartModal: React.FC<MeetingStartModalProps> = ({ open, initialTitle, onClose, onStart }) => {
    const [title, setTitle] = useState(initialTitle ?? '');
    const [meetingType, setMeetingType] = useState<MeetingType>('DAILY');
    const [autoShareSummary, setAutoShareSummary] = useState(true);
    const [shareChannelId, setShareChannelId] = useState<string>('');

    useEffect(() => {
        if (open) {
            setTitle(initialTitle ?? '');
            setMeetingType('DAILY');
            setAutoShareSummary(true);
            setShareChannelId('');
        }
    }, [open, initialTitle]);

    if (!open) return null;

    const handleSubmit = () => {
        onStart({
            title: title.trim() || '새 미팅',
            meetingType,
            autoShareSummary,
            shareChannelId: shareChannelId ? Number(shareChannelId) : null,
        });
    };

    return (
        <div className="meeting-modal-backdrop" role="dialog" aria-modal="true">
            <div className="meeting-modal">
                <div className="meeting-modal__header">
                    <h2>미팅 시작</h2>
                    <button className="meeting-modal__close" onClick={onClose} aria-label="닫기">
                        ✕
                    </button>
                </div>
                <div className="meeting-modal__body">
                    <label className="meeting-modal__label">
                        제목
                        <input
                            type="text"
                            className="meeting-modal__input"
                            value={title}
                            onChange={(event) => setTitle(event.target.value)}
                            placeholder="미팅 제목을 입력하세요"
                        />
                    </label>

                    <div className="meeting-modal__label">
                        유형
                        <div className="meeting-modal__options">
                            {meetingTypeOptions.map((option) => (
                                <button
                                    key={option.value}
                                    className={`meeting-modal__option ${meetingType === option.value ? 'active' : ''}`}
                                    onClick={() => setMeetingType(option.value)}
                                    type="button"
                                >
                                    {option.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    <label className="meeting-modal__checkbox">
                        <input
                            type="checkbox"
                            checked={autoShareSummary}
                            onChange={(event) => setAutoShareSummary(event.target.checked)}
                        />
                        요약을 텍스트 채널에 자동 공유
                    </label>

                    <label className="meeting-modal__label">
                        공유 채널 ID (선택)
                        <input
                            type="number"
                            className="meeting-modal__input"
                            value={shareChannelId}
                            onChange={(event) => setShareChannelId(event.target.value)}
                            placeholder="예: 1001"
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
