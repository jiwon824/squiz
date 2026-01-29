// STT 리포트 전역 상태 관리
// Mock 모드와 API 모드를 useMock 플래그로 전환

import { create } from 'zustand';
import { meetingApi } from '@/features/meeting/services/meetingApi';
import type { MeetingReport, TranscriptItem } from '@/features/dashboard-v2/components/stt-report/types';
import type {
    MeetingDetailResponse,
    MeetingTranscriptItemResponse,
    MeetingListItemResponse,
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

/** 백엔드 응답 조합 → UI MeetingReport 변환 */
const transformToMeetingReport = (
    detail: MeetingDetailResponse,
    transcripts: MeetingTranscriptItemResponse[],
    studyName: string,
): MeetingReport => ({
    id: detail.id,
    studyName,
    meetingTitle: detail.title,
    date: detail.startedAt?.split('T')[0] ?? '',
    duration: formatDuration(detail.durationSeconds),
    participants: detail.participants.map(p => p.nickname),
    summary: detail.summary?.summary ?? '',
    keywords: detail.keywords ?? [],
    highlights: [],
    actionItems: detail.summary?.actionItems?.map(a => a.content) ?? [],
    transcript: transcripts.map(transformTranscript),
});

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
    // 초기 상태: Mock 모드
    studyId: null,
    studyName: '',
    reports: MOCK_REPORTS,
    selectedReport: MOCK_REPORTS[0],
    isLoading: false,
    error: null,
    useMock: true,

    setStudy: (studyId, studyName) => set({ studyId, studyName }),

    setUseMock: (useMock) => {
        if (useMock) {
            set({ useMock, reports: MOCK_REPORTS, selectedReport: MOCK_REPORTS[0] });
        } else {
            set({ useMock });
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
            // API 실패 시 Mock 폴백
            console.error('[SttStore] 미팅 목록 조회 실패, Mock 폴백:', err);
            set({
                reports: MOCK_REPORTS,
                selectedReport: MOCK_REPORTS[0],
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
            const [detail, transcripts] = await Promise.all([
                meetingApi.getMeetingDetail(studyId, meetingId),
                meetingApi.getTranscripts(studyId, meetingId),
            ]);

            const report = transformToMeetingReport(detail, transcripts, studyName);

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
        reports: MOCK_REPORTS,
        selectedReport: MOCK_REPORTS[0],
        isLoading: false,
        error: null,
    }),
}));
