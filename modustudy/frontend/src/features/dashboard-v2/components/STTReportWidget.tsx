import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { FileText, Calendar, Users, ChevronRight } from 'lucide-react';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';
import { WidgetHeader, WidgetContainer } from '@/shared/components/layouts';
import { useSttStore } from '@/store/sttStore';
import { studyApi } from '@/api/endpoints/studyApi';

export const STTReportWidget: React.FC = () => {
    // STT 스토어
    const {
        reports,
        selectedReport,
        selectReport,
        isLoading,
        fetchMeetings,
        setStudy,
    } = useSttStore();

    // 스터디 목록 로딩 상태
    const [studiesLoading, setStudiesLoading] = useState(true);
    const [initialized, setInitialized] = useState(false);

    // 마운트 시 내 스터디 목록에서 첫 번째 스터디 선택 후 미팅 조회
    useEffect(() => {
        if (initialized) return;

        const loadData = async () => {
            try {
                setStudiesLoading(true);
                const response = await studyApi.getMyStudies(0, 10);

                if (response.content.length > 0) {
                    const firstStudy = response.content[0];
                    setStudy(firstStudy.id, firstStudy.name);
                    await fetchMeetings();
                }
            } catch (err) {
                console.error('[STTReportWidget] 스터디 로딩 실패:', err);
            } finally {
                setStudiesLoading(false);
                setInitialized(true);
            }
        };

        loadData();
    }, [initialized, setStudy, fetchMeetings]);

    // 로딩 중
    if (studiesLoading || isLoading) {
        return (
            <WidgetContainer>
                <WidgetHeader
                    icon={FileText}
                    iconColor="neutral"
                    title="미팅 리포트"
                    subtitle="최근 스터디 요약"
                    maximizePath="/meeting-report"
                />
                <div className="flex items-center justify-center h-64">
                    <Spinner variant="center" size="md" label="로딩 중..." />
                </div>
            </WidgetContainer>
        );
    }

    // 데이터 없음
    if (reports.length === 0) {
        return (
            <WidgetContainer>
                <WidgetHeader
                    icon={FileText}
                    iconColor="neutral"
                    title="미팅 리포트"
                    subtitle="최근 스터디 요약"
                    maximizePath="/meeting-report"
                />
                <div className="text-center py-12">
                    <FileText className="mx-auto text-gray-300 mb-4" size={48} />
                    <p className="text-text-secondary">미팅 기록이 없습니다</p>
                    <p className="text-sm text-text-tertiary mt-1">스터디 미팅 후 자동으로 생성됩니다</p>
                </div>
            </WidgetContainer>
        );
    }

    return (
        <WidgetContainer>
            {/* 헤더 - 공통 컴포넌트 사용 */}
            <WidgetHeader
                icon={FileText}
                iconColor="neutral"
                title="미팅 리포트"
                subtitle="최근 스터디 요약"
                maximizePath="/meeting-report"
            />

            <div className="flex">
                {/* 좌측: 미팅 리스트 */}
                <div className="w-1/3 border-r border-gray-100 bg-gray-50/50 max-h-[400px] overflow-y-auto">
                    {reports.map((report) => (
                        <button
                            key={report.id}
                            onClick={() => selectReport(report)}
                            className={cn(
                                'w-full p-4 text-left transition-all border-b border-gray-100',
                                selectedReport?.id === report.id
                                    ? 'bg-primary/10 border-l-4 border-l-primary'
                                    : 'hover:bg-gray-100'
                            )}
                        >
                            <h4 className="font-bold text-text-primary text-sm mb-0">
                                {report.studyName}
                            </h4>
                            <p className="text-xs text-text-secondary truncate">
                                {report.meetingTitle}
                            </p>
                            <div className="flex items-center gap-2 mt-2 text-xs text-text-tertiary">
                                <Calendar size={12} />
                                <span>{report.date}</span>
                            </div>
                        </button>
                    ))}
                </div>

                {/* 우측: 리포트 상세 */}
                <div className="flex-1 p-6 max-h-[400px] overflow-y-auto">
                    {selectedReport ? (
                        <motion.div
                            key={selectedReport.id}
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ duration: 0.3 }}
                        >
                            <h3 className="text-xl font-bold text-text-primary mb-0">
                                {selectedReport.meetingTitle}
                            </h3>
                            <div className="flex items-center gap-4 text-sm text-text-secondary mb-4">
                                <div className="flex items-center gap-1">
                                    <Calendar size={14} />
                                    <span>{selectedReport.date}</span>
                                </div>
                                <div className="flex items-center gap-1">
                                    <Users size={14} />
                                    <span>{selectedReport.participants?.length || 0}명 참여</span>
                                </div>
                                <span>{selectedReport.duration}</span>
                            </div>

                            {/* 요약 */}
                            <div className="bg-gray-50 rounded-xl p-4 mb-4">
                                <h4 className="font-bold text-text-primary mb-0 text-sm">📝 요약</h4>
                                <p className="text-text-secondary text-sm leading-relaxed">
                                    {selectedReport.summary || '요약 정보가 없습니다'}
                                </p>
                            </div>

                            {/* 키워드 */}
                            {selectedReport.keywords && selectedReport.keywords.length > 0 && (
                                <div className="mb-4">
                                    <h4 className="font-bold text-text-primary mb-0 text-sm">🔑 핵심 키워드</h4>
                                    <div className="flex flex-wrap gap-2">
                                        {selectedReport.keywords.map((keyword, idx) => (
                                            <span
                                                key={idx}
                                                className="px-3 py-1 bg-primary/10 text-primary text-xs font-medium rounded-full"
                                            >
                                                {keyword}
                                            </span>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* 액션 아이템 */}
                            {selectedReport.actionItems && selectedReport.actionItems.length > 0 && (
                                <div>
                                    <h4 className="font-bold text-text-primary mb-0 text-sm">💡 액션 아이템</h4>
                                    <ul className="space-y-2">
                                        {selectedReport.actionItems.map((item, idx) => (
                                            <li
                                                key={idx}
                                                className="flex items-start gap-2 text-sm text-text-secondary"
                                            >
                                                <ChevronRight size={16} className="text-primary flex-shrink-0 mt-0.5" />
                                                <span>{item}</span>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </motion.div>
                    ) : (
                        <div className="flex items-center justify-center h-full text-text-tertiary">
                            미팅을 선택해주세요
                        </div>
                    )}
                </div>
            </div>
        </WidgetContainer>
    );
};
