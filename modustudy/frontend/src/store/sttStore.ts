// STT 리포트 전역 상태 관리
// Mock 모드와 API 모드를 useMock 플래그로 전환

import { create } from 'zustand';
import { meetingApi } from '@/features/meeting/services/meetingApi';
import type { MeetingReport, TranscriptItem } from '@/features/dashboard-v2/components/stt-report/types';
import type {
    MeetingDetailResponse,
    MeetingTranscriptItemResponse,
    MeetingListItemResponse,
    MeetingActionItemResponse,
} from '@/features/meeting/types';
import { MOCK_REPORTS } from '@/features/dashboard-v2/components/stt-report/constants';

// ===== 변환 유틸리티 =====

/** durationSeconds → "X시간 Y분" 형식 변환 */
const formatDuration = (seconds: number | null): string => {
    if (!seconds) return '0분';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0 && minutes > 0) return `${hours}시간 ${minutes}분`;
    if (hours > 0) return `${hours}시간`;
    return `${minutes}분`;
};

/** timestampSeconds → "MM:SS" 형식 변환 */
const formatTimestamp = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
};

/** 백엔드 트랜스크립트 → UI TranscriptItem 변환 */
const transformTranscript = (item: MeetingTranscriptItemResponse): TranscriptItem => ({
    speaker: item.user?.nickname ?? '알 수 없음',
    time: formatTimestamp(item.timestampSeconds),
    text: item.content,
});

/**
 * summary 텍스트에서 섹션별 내용 파싱
 * AI 서버에서 "📌 주요 내용:", "📝 요약:", "📋 액션 아이템:", "📚 학습 피드백:" 포맷으로 전송
 */
const parseSummarySections = (rawSummary: string): {
    highlights: string[];
    summary: string;
    actionItemsFromSummary: string[];
} => {
    const result = {
        highlights: [] as string[],
        summary: rawSummary,
        actionItemsFromSummary: [] as string[],
    };

    if (!rawSummary) return result;

    // 📌 주요 내용 섹션 파싱
    const highlightsMatch = rawSummary.match(/📌\s*주요\s*내용[:\s]*\n([\s\S]*?)(?=\n\n📝|\n\n📋|\n\n📚|$)/);
    if (highlightsMatch) {
        const highlightsSection = highlightsMatch[1];
        result.highlights = highlightsSection
            .split('\n')
            .map(line => line.replace(/^[•\-\*]\s*/, '').trim())
            .filter(line => line.length > 0);
    }

    // 📝 요약 섹션 파싱 (없으면 전체 텍스트 사용)
    const summaryMatch = rawSummary.match(/📝\s*요약[:\s]*\n([\s\S]*?)(?=\n\n📋|\n\n📚|$)/);
    if (summaryMatch) {
        result.summary = summaryMatch[1].trim();
    } else if (!highlightsMatch) {
        // 섹션 포맷이 아닌 경우 전체를 요약으로 사용
        result.summary = rawSummary;
    }

    // 📋 액션 아이템 섹션 파싱 (DB actionItems와 병합용)
    const actionItemsMatch = rawSummary.match(/📋\s*액션\s*아이템[:\s]*\n([\s\S]*?)(?=\n\n📚|$)/);
    if (actionItemsMatch) {
        const actionSection = actionItemsMatch[1];
        result.actionItemsFromSummary = actionSection
            .split('\n')
            .map(line => line.replace(/^[•\-\*]\s*/, '').trim())
            .filter(line => line.length > 0);
    }

    return result;
};

/** 백엔드 응답 조합 → UI MeetingReport 변환 */
const transformToMeetingReport = (
    detail: MeetingDetailResponse,
    transcripts: MeetingTranscriptItemResponse[],
    studyName: string,
    actionItemsFromDb?: MeetingActionItemResponse[],
): MeetingReport => {
    const rawSummary = detail.summary?.summary ?? '';
    const parsed = parseSummarySections(rawSummary);

    // 우선순위: 1) DB에서 직접 조회한 actionItems, 2) summary 응답의 actionItems, 3) summary 텍스트에서 파싱
    const directDbItems = actionItemsFromDb?.map(a => a.content) ?? [];
    const summaryResponseItems = detail.summary?.actionItems?.map(a => a.content) ?? [];
    const finalActionItems = directDbItems.length > 0
        ? directDbItems
        : (summaryResponseItems.length > 0 ? summaryResponseItems : parsed.actionItemsFromSummary);

    // 우선순위: 1) API 응답의 highlights, 2) summary 텍스트에서 파싱
    const apiHighlights = detail.summary?.highlights ?? [];
    const finalHighlights = apiHighlights.length > 0 ? apiHighlights : parsed.highlights;

    return {
        id: detail.id,
        studyName,
        meetingTitle: detail.title,
        date: detail.startedAt?.split('T')[0] ?? '',
        duration: formatDuration(detail.durationSeconds),
        participants: detail.participants.map(p => p.nickname),
        participantCount: detail.participants.length,
        summary: parsed.summary,
        keywords: detail.keywords ?? [],
        highlights: finalHighlights,
        actionItems: finalActionItems,
        transcript: transcripts.map(transformTranscript),
    };
};

/** 목록 아이템 → 간략 MeetingReport 변환 (상세 조회 전) */
const transformListItemToReport = (
    item: MeetingListItemResponse,
    studyName: string,
): MeetingReport => ({
    id: item.id,
    studyName,
    meetingTitle: item.title,
    date: item.startedAt?.split('T')[0] ?? '',
    duration: formatDuration(item.durationSeconds),
    participants: [],
    participantCount: item.participantCount ?? 0,
    summary: '',
    keywords: [],
    highlights: [],
    actionItems: [],
    transcript: [],
});

// ===== 스토어 타입 =====

interface SttState {
    // 상태
    studyId: number | null;
    studyName: string;
    reports: MeetingReport[];
    selectedReport: MeetingReport | null;
    isLoading: boolean;
    error: string | null;
    /** true면 MOCK_REPORTS 사용, false면 API 호출 */
    useMock: boolean;

    // 액션
    /** 스터디 정보 설정 */
    setStudy: (studyId: number, studyName: string) => void;
    /** Mock/API 모드 전환 */
    setUseMock: (useMock: boolean) => void;
    /** 리포트 선택 (사이드바 클릭) */
    selectReport: (report: MeetingReport) => void;
    /** 미팅 목록 조회 */
    fetchMeetings: (params?: { page?: number; size?: number }) => Promise<void>;
    /** 선택된 미팅 상세 + 트랜스크립트 조회 */
    fetchMeetingDetail: (meetingId: number) => Promise<void>;
    /** 요약 로컬 업데이트 */
    updateSummary: (meetingId: number, newSummary: string) => void;
    /** 트랜스크립트 로컬 업데이트 */
    updateTranscript: (meetingId: number, transcript: TranscriptItem[]) => void;
    /** 상태 초기화 */
    reset: () => void;
}

export const useSttStore = create<SttState>((set, get) => ({
    // 초기 상태: API 모드 (실제 DB 연동)
    studyId: null,
    studyName: '',
    reports: [],
    selectedReport: null,
    isLoading: false,
    error: null,
    useMock: false,

    setStudy: (studyId, studyName) => set({ studyId, studyName }),

    setUseMock: (useMock) => {
        if (useMock) {
            set({ useMock, reports: MOCK_REPORTS, selectedReport: MOCK_REPORTS[0] });
        } else {
            set({ useMock, reports: [], selectedReport: null });
        }
    },

    selectReport: (report) => {
        const { useMock, studyId } = get();
        set({ selectedReport: report });

        // API 모드일 때 선택 시 상세 데이터 lazy load
        if (!useMock && studyId) {
            get().fetchMeetingDetail(report.id);
        }
    },

    fetchMeetings: async (params) => {
        const { studyId, studyName, useMock } = get();

        // Mock 모드면 바로 반환
        if (useMock || !studyId) {
            set({ reports: MOCK_REPORTS, selectedReport: MOCK_REPORTS[0] });
            return;
        }

        set({ isLoading: true, error: null });
        try {
            const page = await meetingApi.listMeetings(studyId, params);

            // 목록은 간략 정보만 변환 (N+1 방지)
            const reports = page.content.map(item =>
                transformListItemToReport(item, studyName)
            );

            const selected = reports[0] ?? null;
            set({ reports, selectedReport: selected, isLoading: false });

            // 첫 번째 미팅의 상세 정보 자동 조회
            if (selected) {
                get().fetchMeetingDetail(selected.id);
            }
        } catch (err) {
            set({
                reports: [],
                selectedReport: null,
                isLoading: false,
                error: err instanceof Error ? err.message : 'API 호출 실패',
            });
        }
    },

    fetchMeetingDetail: async (meetingId) => {
        const { studyId, studyName, useMock } = get();
        if (useMock || !studyId) return;

        set({ isLoading: true, error: null });
        try {
            // 상세, 트랜스크립트, 액션아이템 병렬 조회
            const [detail, transcripts, actionItemsFromDb] = await Promise.all([
                meetingApi.getMeetingDetail(studyId, meetingId),
                meetingApi.getTranscripts(studyId, meetingId),
                meetingApi.getActionItems(studyId, meetingId).catch(() => []),
            ]);

            const report = transformToMeetingReport(detail, transcripts, studyName, actionItemsFromDb);

            set(state => ({
                selectedReport: report,
                // 목록에서도 상세 데이터로 교체
                reports: state.reports.map(r => r.id === meetingId ? report : r),
                isLoading: false,
            }));
        } catch (err) {
            set({
                isLoading: false,
                error: err instanceof Error ? err.message : '상세 조회 실패',
            });
        }
    },

    updateSummary: (meetingId, newSummary) => {
        set(state => ({
            selectedReport: state.selectedReport?.id === meetingId
                ? { ...state.selectedReport, summary: newSummary }
                : state.selectedReport,
            reports: state.reports.map(r =>
                r.id === meetingId ? { ...r, summary: newSummary } : r
            ),
        }));
    },

    updateTranscript: (meetingId, transcript) => {
        set(state => ({
            selectedReport: state.selectedReport?.id === meetingId
                ? { ...state.selectedReport, transcript }
                : state.selectedReport,
        }));
    },

    reset: () => set({
        studyId: null,
        studyName: '',
        reports: [],
        selectedReport: null,
        isLoading: false,
        error: null,
    }),
}));
