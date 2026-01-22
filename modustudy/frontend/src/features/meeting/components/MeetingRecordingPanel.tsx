import React from 'react';
import {
    MeetingAudioRecordingResponse,
    MeetingRecordingResponse,
    MeetingSttFileResponse,
    MeetingSttSummaryResponse,
} from '../types';
import '../styles/MeetingDetail.css';

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
    return (
        <section className="meeting-detail-card">
            <div className="meeting-detail-card__header">
                <h3>녹음/녹화</h3>
            </div>
            <div className="meeting-detail-card__body">
                {recording?.recordingUrl ? (
                    <a href={recording.recordingUrl} target="_blank" rel="noreferrer">
                        📹 전체 녹화 보기
                    </a>
                ) : (
                    <p className="meeting-detail-empty">녹화 파일이 아직 없습니다.</p>
                )}

                <div className="meeting-recording-list">
                    {audioRecordings.map((audio) => (
                        <a
                            key={audio.id}
                            href={audio.recordingUrl ?? '#'}
                            target="_blank"
                            rel="noreferrer"
                            className="meeting-recording-item"
                        >
                            🎙️ {audio.trackType === 'MIXED' ? '전체 음성' : `개인 음성 (${audio.userId ?? '-'})`}
                        </a>
                    ))}
                </div>

                <div className="meeting-recording-files">
                    {sttFile?.fileUrl && (
                        <a href={sttFile.fileUrl} target="_blank" rel="noreferrer">
                            📝 STT 텍스트
                        </a>
                    )}
                    {summaryFile?.fileUrl && (
                        <a href={summaryFile.fileUrl} target="_blank" rel="noreferrer">
                            🧾 요약 텍스트
                        </a>
                    )}
                </div>
            </div>
        </section>
    );
};

export default MeetingRecordingPanel;
