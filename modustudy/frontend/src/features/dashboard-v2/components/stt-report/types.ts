// STT 미팅 리포트 타입 정의

/** 미팅 리포트 */
export interface MeetingReport {
    id: number;
    studyName: string;
    meetingTitle: string;
    date: string;
    duration: string;
    participants: string[];
    summary: string;
    keywords: string[];
    highlights: string[];
    actionItems: string[];
    transcript: TranscriptItem[];
}

/** 대화 기록 아이템 */
export interface TranscriptItem {
    speaker: string;
    time: string;
    text: string;
}

/** 탭 타입 */
export type TabType = 'summary' | 'transcript' | 'action' | 'stats';

/** 화자별 통계 */
export interface SpeakerStats {
    name: string;
    count: number;
    percentage: number;
}
