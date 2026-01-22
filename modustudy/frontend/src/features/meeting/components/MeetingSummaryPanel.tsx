import React from 'react';
import { MeetingSummaryResponse } from '../types';
import '../styles/MeetingDetail.css';

interface MeetingSummaryPanelProps {
    summary: MeetingSummaryResponse | null;
}

const MeetingSummaryPanel: React.FC<MeetingSummaryPanelProps> = ({ summary }) => {
    if (!summary) {
        return (
            <section className="meeting-detail-card">
                <div className="meeting-detail-card__header">
                    <h3>AI 요약</h3>
                </div>
                <div className="meeting-detail-card__body">
                    <p className="meeting-detail-empty">요약이 아직 생성되지 않았습니다.</p>
                </div>
            </section>
        );
    }

    return (
        <section className="meeting-detail-card">
            <div className="meeting-detail-card__header">
                <h3>AI 요약</h3>
                <span className={`meeting-status-chip ${summary.status.toLowerCase()}`}>{summary.status}</span>
            </div>
            <div className="meeting-detail-card__body">
                <p className="meeting-summary-text">{summary.summary}</p>
                <div className="meeting-summary-keywords">
                    {summary.keywords.map((keyword) => (
                        <span key={keyword} className="meeting-summary-keyword">
                            {keyword}
                        </span>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default MeetingSummaryPanel;
