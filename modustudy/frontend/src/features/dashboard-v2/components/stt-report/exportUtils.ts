// STT 미팅 리포트 JSON 내보내기 유틸리티
// LLM 활용에 최적화된 구조화 JSON 포맷

import type { MeetingReport } from './types';

/** 내보내기 범위 */
export type ExportScope = 'current' | 'byStudy' | 'byDate' | 'all';

/** LLM 친화적 JSON 구조로 변환 */
const formatReportForLLM = (report: MeetingReport) => ({
    metadata: {
        id: report.id,
        study_group: report.studyName,
        meeting_title: report.meetingTitle,
        date: report.date,
        duration: report.duration,
        participant_count: report.participantCount > 0 ? report.participantCount : report.participants.length,
        participants: report.participants,
    },
    analysis: {
        summary: report.summary,
        keywords: report.keywords,
        key_highlights: report.highlights,
        action_items: report.actionItems,
    },
    transcript: report.transcript.map(t => ({
        speaker: t.speaker,
        timestamp: t.time,
        content: t.text,
    })),
});

/** 스터디별 그룹핑 */
const groupByStudy = (reports: MeetingReport[]) => {
    const grouped: Record<string, ReturnType<typeof formatReportForLLM>[]> = {};
    reports.forEach(report => {
        const key = report.studyName;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(formatReportForLLM(report));
    });
    return {
        export_type: 'by_study_group',
        exported_at: new Date().toISOString(),
        study_groups: Object.entries(grouped).map(([name, meetings]) => ({
            study_name: name,
            meeting_count: meetings.length,
            meetings,
        })),
    };
};

/** 일자별 그룹핑 */
const groupByDate = (reports: MeetingReport[]) => {
    const sorted = [...reports].sort((a, b) => b.date.localeCompare(a.date));
    return {
        export_type: 'by_date',
        exported_at: new Date().toISOString(),
        total_meetings: sorted.length,
        meetings: sorted.map(formatReportForLLM),
    };
};

/** JSON 파일 다운로드 실행 */
const downloadJSON = (data: unknown, filename: string) => {
    const json = JSON.stringify(data, null, 2);
    const blob = new Blob([json], { type: 'application/json;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
};

/** 내보내기 실행 */
export const exportReports = (
    scope: ExportScope,
    allReports: MeetingReport[],
    currentReport?: MeetingReport
) => {
    const timestamp = new Date().toISOString().slice(0, 10);

    switch (scope) {
        case 'current': {
            if (!currentReport) return;
            const data = {
                export_type: 'single_meeting',
                exported_at: new Date().toISOString(),
                meeting: formatReportForLLM(currentReport),
            };
            downloadJSON(data, `stt-report_${currentReport.studyName}_${currentReport.date}.json`);
            break;
        }
        case 'byStudy': {
            const data = groupByStudy(allReports);
            downloadJSON(data, `stt-reports_by-study_${timestamp}.json`);
            break;
        }
        case 'byDate': {
            const data = groupByDate(allReports);
            downloadJSON(data, `stt-reports_by-date_${timestamp}.json`);
            break;
        }
        case 'all': {
            const data = {
                export_type: 'all',
                exported_at: new Date().toISOString(),
                total_meetings: allReports.length,
                study_groups: groupByStudy(allReports).study_groups,
                meetings_chronological: groupByDate(allReports).meetings,
            };
            downloadJSON(data, `stt-reports_all_${timestamp}.json`);
            break;
        }
    }
};
