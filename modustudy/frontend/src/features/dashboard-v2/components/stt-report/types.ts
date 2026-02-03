// STT 미팅 리포트 타입 정의

/** 미팅 리포트 */
export interface MeetingReport {
    id: number;
    studyName: string;
    meetingTitle: string;
    date: string;
    duration: string;
    participants: string[];
    /** 참여자 수 (목록 조회 시 사용, 상세 조회 전에도 정확한 수 제공) */
    participantCount: number;
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
export type TabType = 'summary' | 'quiz' | 'transcript' | 'action' | 'stats';

/** 화자별 통계 */
export interface SpeakerStats {
    name: string;
    count: number;
    percentage: number;
}
