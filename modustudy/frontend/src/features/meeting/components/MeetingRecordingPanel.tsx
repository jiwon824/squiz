import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
    MeetingAudioRecordingResponse,
    MeetingRecordingResponse,
    MeetingSttFileResponse,
    MeetingSttSummaryResponse,
} from '../types';
import '../styles/MeetingDetail.css';

interface MeetingRecordingPanelProps {
    studyId: number | null;
    meetingId: number | null;
    recording: MeetingRecordingResponse | null;
    audioRecordings: MeetingAudioRecordingResponse[];
    sttFile: MeetingSttFileResponse | null;
    summaryFile: MeetingSttSummaryResponse | null;
}

const MeetingRecordingPanel: React.FC<MeetingRecordingPanelProps> = ({
    studyId,
    meetingId,
    recording,
    audioRecordings,
    sttFile,
    summaryFile,
}) => {
    const navigate = useNavigate();
    const canViewRecording = Boolean(recording?.recordingUrl && studyId && meetingId);

    return (
        <section className="meeting-detail-card">
            <div className="meeting-detail-card__header">
                <h3>녹화</h3>
            </div>
            <div className="meeting-detail-card__body">
                {canViewRecording ? (
                    <button
                        type="button"
                        className="meeting-btn ghost"
                        onClick={() => navigate(`/study/${studyId}/meetings/${meetingId}/recording`)}
                    >
                        전체 녹화 보기
                    </button>
                ) : (
                    <p className="meeting-detail-empty">녹화 파일이 아직 준비되지 않았어요.</p>
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
                            음성: {audio.trackType === 'MIXED' ? '전체' : `개인 (${audio.userId ?? '-'})`}
                        </a>
                    ))}
                </div>

                <div className="meeting-recording-files">
                    {sttFile?.fileUrl && (
                        <a href={sttFile.fileUrl} target="_blank" rel="noreferrer">
                            STT 텍스트
                        </a>
                    )}
                    {summaryFile?.fileUrl && (
                        <a href={summaryFile.fileUrl} target="_blank" rel="noreferrer">
                            요약 텍스트
                        </a>
                    )}
                </div>
            </div>
        </section>
    );
};

export default MeetingRecordingPanel;
