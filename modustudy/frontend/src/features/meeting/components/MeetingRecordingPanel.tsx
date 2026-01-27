import React from 'react';
import {
    MeetingAudioRecordingResponse,
    MeetingRecordingResponse,
    MeetingSttFileResponse,
    MeetingSttSummaryResponse,
} from '../types';

interface MeetingRecordingPanelProps {
    recording: MeetingRecordingResponse | null;
    audioRecordings: MeetingAudioRecordingResponse[];
    sttFile: MeetingSttFileResponse | null;
    summaryFile: MeetingSttSummaryResponse | null;
}

const MeetingRecordingPanel: React.FC<MeetingRecordingPanelProps> = ({
    recording,
    audioRecordings,
    sttFile,
    summaryFile,
}) => {
    const apiBaseUrl = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
    const resolveAssetUrl = (url: string | null | undefined) => {
        if (!url) return '';
        if (url.startsWith('http://') || url.startsWith('https://')) return url;
        if (!apiBaseUrl) return url;
        if (url.startsWith('/')) return `${apiBaseUrl}${url}`;
        return `${apiBaseUrl}/${url}`;
    };

    const videoUrl = resolveAssetUrl(recording?.recordingUrl);
    const hasAudio = audioRecordings.some((item) => Boolean(item.recordingUrl));

    return (
        <section className="meeting-detail-card">
            <div className="meeting-detail-card__header">
                <h3>녹화 파일</h3>
                {recording?.status ? (
                    <span className="meeting-status-chip">{recording.status}</span>
                ) : null}
            </div>
            <div className="meeting-detail-card__body">
                {recording?.recordingUrl ? (
                    <div className="meeting-recording-list">
                        <span className="meeting-recording-item">영상 파일</span>
                        <a className="meeting-btn ghost" href={videoUrl} target="_blank" rel="noreferrer">
                            다운로드
                        </a>
                    </div>
                ) : (
                    <p className="meeting-detail-empty">녹화 영상이 아직 준비되지 않았습니다.</p>
                )}

                {hasAudio ? (
                    <div>
                        <p>오디오 녹음</p>
                        <div className="meeting-recording-files">
                            {audioRecordings
                                .filter((item) => item.recordingUrl)
                                .map((item) => (
                                    <a
                                        key={item.id}
                                        className="meeting-recording-item"
                                        href={resolveAssetUrl(item.recordingUrl)}
                                        target="_blank"
                                        rel="noreferrer"
                                    >
                                        {item.trackType} {item.userId ? `#${item.userId}` : ''}
                                    </a>
                                ))}
                        </div>
                    </div>
                ) : (
                    <p className="meeting-detail-empty">오디오 녹음 파일이 없습니다.</p>
                )}

                {(sttFile?.fileUrl || summaryFile?.fileUrl) ? (
                    <div>
                        <p>텍스트 파일</p>
                        <div className="meeting-recording-files">
                            {sttFile?.fileUrl ? (
                                <a
                                    className="meeting-recording-item"
                                    href={resolveAssetUrl(sttFile.fileUrl)}
                                    target="_blank"
                                    rel="noreferrer"
                                >
                                    STT
                                </a>
                            ) : null}
                            {summaryFile?.fileUrl ? (
                                <a
                                    className="meeting-recording-item"
                                    href={resolveAssetUrl(summaryFile.fileUrl)}
                                    target="_blank"
                                    rel="noreferrer"
                                >
                                    요약본
                                </a>
                            ) : null}
                        </div>
                    </div>
                ) : (
                    <p className="meeting-detail-empty">텍스트 파일이 없습니다.</p>
                )}
            </div>
        </section>
    );
};

export default MeetingRecordingPanel;
