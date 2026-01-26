import React, { useEffect, useState } from 'react';
import { MeetingRequestPayload, MeetingType } from '../types';
import '../styles/MeetingShared.css';

const meetingTypeOptions: { label: string; value: MeetingType }[] = [
    { label: '?°ì¼ë¦??¤íƒ ?œì—…', value: 'DAILY' },
    { label: 'ì£¼ê°„ ?Œê³ ', value: 'WEEKLY' },
    { label: '?ìœ  ?Œì˜', value: 'FREE' },
    { label: 'ê¸°í?', value: 'OTHER' },
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
    const [shareWorkspaceId, setShareWorkspaceId] = useState<string>('');

    useEffect(() => {
        if (open) {
            setTitle(initialTitle ?? '');
            setMeetingType('DAILY');
            setAutoShareSummary(true);
            setShareWorkspaceId('');
        }
    }, [open, initialTitle]);

    if (!open) return null;

    const handleSubmit = () => {
        onStart({
            title: title.trim() || '??ë¯¸íŒ…',
            meetingType,
            autoShareSummary,
            shareWorkspaceId: shareWorkspaceId ? Number(shareWorkspaceId) : null,
        });
    };

    return (
        <div className="meeting-modal-backdrop" role="dialog" aria-modal="true">
            <div className="meeting-modal">
                <div className="meeting-modal__header">
                    <h2>ë¯¸íŒ… ?œì‘</h2>
                    <button className="meeting-modal__close" onClick={onClose} aria-label="?«ê¸°">
                        ??
                    </button>
                </div>
                <div className="meeting-modal__body">
                    <label className="meeting-modal__label">
                        °øÀ¯ ¿öÅ©½ºÆäÀÌ½º ID (¼±ÅÃ)
                        <input
                            type="text"
                            className="meeting-modal__input"
                            value={title}
                            onChange={(event) => setTitle(event.target.value)}
                            placeholder="ë¯¸íŒ… ?œëª©???…ë ¥?˜ì„¸??
                        />
                    </label>

                    <div className="meeting-modal__label">
                        ? í˜•
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
                        ?”ì•½???ìŠ¤??ì±„ë„???ë™ ê³µìœ 
                    </label>

                    <label className="meeting-modal__label">
                        °øÀ¯ ¿öÅ©½ºÆäÀÌ½º ID (¼±ÅÃ)
                        <input
                            type="number"
                            className="meeting-modal__input"
                            value={shareWorkspaceId}
                            onChange={(event) => setShareWorkspaceId(event.target.value)}
                            placeholder="?? 1001"
                        />
                    </label>
                </div>
                <div className="meeting-modal__footer">
                    <button className="meeting-btn ghost" onClick={onClose} type="button">
                        ì·¨ì†Œ
                    </button>
                    <button className="meeting-btn primary" onClick={handleSubmit} type="button">
                        ?œì‘?˜ê¸°
                    </button>
                </div>
            </div>
        </div>
    );
};

export default MeetingStartModal;

